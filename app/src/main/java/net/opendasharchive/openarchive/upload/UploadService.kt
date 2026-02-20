package net.opendasharchive.openarchive.upload

import android.app.*
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Configuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.util.CleanInsightsManager
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.analytics.api.AnalyticsEvent
import net.opendasharchive.openarchive.analytics.api.AnalyticsManager
import net.opendasharchive.openarchive.core.domain.Evidence
import net.opendasharchive.openarchive.core.domain.EvidenceStatus
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.core.repositories.CollectionRepository
import net.opendasharchive.openarchive.core.repositories.MediaRepository
import net.opendasharchive.openarchive.core.repositories.ProjectRepository
import net.opendasharchive.openarchive.core.repositories.SpaceRepository
import net.opendasharchive.openarchive.features.main.HomeActivity
import net.opendasharchive.openarchive.services.Conduit
import net.opendasharchive.openarchive.util.DateUtils
import net.opendasharchive.openarchive.util.Prefs
import org.koin.android.ext.android.inject
import java.io.IOException
import java.util.*

class UploadService : JobService() {

    // Inject analytics manager
    private val analyticsManager: AnalyticsManager by inject()
    private val mediaRepository: MediaRepository by inject()
    private val projectRepository: ProjectRepository by inject()
    private val collectionRepository: CollectionRepository by inject()
    private val spaceRepository: SpaceRepository by inject()

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "oasave_channel_1"
    }

    private var mRunning = false
    private var mKeepUploading = true
    private val mConduits = ArrayList<Conduit>()
    private var serviceJob = SupervisorJob()
    private var serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Configuration.Builder().setJobSchedulerJobIdRange(0, Integer.MAX_VALUE).build()
    }

    override fun onStartJob(params: JobParameters): Boolean {
        mKeepUploading = true
        serviceJob.cancel()
        serviceJob = SupervisorJob()
        serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
        
        // Monitor for deletions to cancel active conduits
        serviceScope.launch {
            UploadEventBus.events.collect { event ->
                if (event is UploadEvent.Deleted) {
                    cancelConduitForMedia(event.mediaId)
                }
            }
        }

        serviceScope.launch {
            upload {
                jobFinished(params, false)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            setNotification(
                params,
                7918,
                prepNotification(),
                JOB_END_NOTIFICATION_POLICY_REMOVE
            )
        }

        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        mKeepUploading = false
        synchronized(mConduits) {
            for (conduit in mConduits) conduit.cancel()
            mConduits.clear()
        }
        serviceJob.cancel()

        return true
    }

    private suspend fun upload(completed: () -> Unit) {
        if (mRunning) return completed()

        mRunning = true
        val batchStartTime = System.currentTimeMillis()
        AppLogger.i("upload started")

        if (!shouldUpload()) {
            mRunning = false
            AppLogger.i("no network, upload stopped")
            // Track network error
            analyticsManager.trackEvent(
                AnalyticsEvent.UploadNetworkError(
                    reason = if (Prefs.uploadWifiOnly) "wifi_required" else "no_network"
                )
            )
            return completed()
        }

        // Get items that are set into queued state.
        var results = emptyList<Evidence>()
        var successCount = 0
        var failedCount = 0
        var totalCount = 0

        // Get initial batch
        val initialBatch = mediaRepository.getQueue()

        if (initialBatch.isNotEmpty()) {
            // Track upload session started (1+ files)
            val sessionSize = initialBatch.size
            val totalSizeMB = initialBatch.sumOf { it.contentLength } / (1024 * 1024)
            analyticsManager.trackEvent(
                AnalyticsEvent.UploadSessionStarted(
                    count = sessionSize,
                    totalSizeMB = totalSizeMB
                )
            )
        }

        while (mKeepUploading &&
            mediaRepository.getQueue()
                .also { results = it }
                .isNotEmpty()
        ) {
            val datePublish = DateUtils.nowDateTime

            for (media in results) {
                totalCount++
                var updatedMedia = media
                if (updatedMedia.status != EvidenceStatus.UPLOADING) {
                    updatedMedia = updatedMedia.copy(
                        uploadedAt = datePublish,
                        progress = 0,
                        status = EvidenceStatus.UPLOADING,
                        statusMessage = ""
                    )
                }

                val project = projectRepository.getProject(updatedMedia.archiveId)
                updatedMedia = updatedMedia.copy(
                    licenseUrl = project?.licenseUrl,
                    vaultId = project?.vaultId ?: 0L
                )

                // Persist updated state before starting upload
                mediaRepository.updateEvidence(updatedMedia)

                // Update submission upload date if not already set.
                // This "closes" the submission bucket in the repository,
                // ensuring that any subsequent imports start a new submission.
                val submission = collectionRepository.getCollection(updatedMedia.submissionId)
                if (submission != null && submission.uploadDate == null) {
                    collectionRepository.updateCollection(submission.copy(uploadDate = datePublish))
                }

                try {
                    AppLogger.i("Started uploading", updatedMedia)
                    val uploadSuccess = upload(updatedMedia)
                    if (uploadSuccess) {
                        successCount++
                    } else {
                        failedCount++
                    }
                } catch (ioe: IOException) {
                    AppLogger.e(ioe)

                    updatedMedia = updatedMedia.copy(
                        statusMessage = "error in uploading media: " + ioe.message,
                        status = EvidenceStatus.ERROR
                    )
                    mediaRepository.updateEvidence(updatedMedia)

                    UploadEventBus.emitChanged(
                        projectId = updatedMedia.archiveId,
                        collectionId = updatedMedia.submissionId,
                        mediaId = updatedMedia.id,
                        progress = -1,
                        isUploaded = false
                    )
                    failedCount++
                }

                if (!mKeepUploading) break // Time to end this.
            }
        }

        AppLogger.i("Uploads completed")

        // Track upload session completed (if any uploads were attempted)
        if (totalCount > 0) {
            val sessionDuration = (System.currentTimeMillis() - batchStartTime) / 1000
            analyticsManager.trackEvent(
                AnalyticsEvent.UploadSessionCompleted(
                    count = totalCount,
                    successCount = successCount,
                    failedCount = failedCount,
                    durationSeconds = sessionDuration
                )
            )
        }

        mRunning = false
        completed()
    }

    @Throws(IOException::class)
    private suspend fun upload(media: Evidence): Boolean {
        val updatedMedia = media.copy(status = EvidenceStatus.UPLOADING)
        AppLogger.i("${updatedMedia.id} - media status changed to uploading")
        mediaRepository.updateEvidence(updatedMedia)

        BroadcastManager.postChange(this, updatedMedia.submissionId, updatedMedia.id)
        UploadEventBus.emitChanged(
            projectId = updatedMedia.archiveId,
            collectionId = updatedMedia.submissionId,
            mediaId = updatedMedia.id,
            progress = 0,
            isUploaded = false
        )

        val conduit = Conduit.get(updatedMedia, this)
        if (conduit == null) {
            AppLogger.e("Conduit is null")
            return false
        }

        // Final check: if it was deleted from DB, don't start the upload
        if (mediaRepository.getEvidence(updatedMedia.id) == null) {
            AppLogger.i("Media ${updatedMedia.id} was deleted from database, skipping upload")
            return false
        }

        val vault = spaceRepository.getSpaceById(updatedMedia.vaultId)
        CleanInsightsManager.measureEvent("upload", "try_upload", vault?.type?.friendlyName)

        synchronized(mConduits) {
            mConduits.add(conduit)
        }
        
        conduit.upload()
        
        synchronized(mConduits) {
            mConduits.remove(conduit)
        }

        return true
    }

    private fun cancelConduitForMedia(mediaId: Long) {
        synchronized(mConduits) {
            val iterator = mConduits.iterator()
            while (iterator.hasNext()) {
                val conduit = iterator.next()
                if (conduit.id == mediaId) {
                    AppLogger.i("Cancelling active conduit for media $mediaId due to deletion")
                    conduit.cancel()
                    iterator.remove()
                }
            }
        }
    }

    /**
     * Check if online, and connected to the appropriate network type.
     */
    private fun shouldUpload(): Boolean {
        if (Prefs.isMigrationInProgress) {
            AppLogger.i("migration in progress, upload paused")
            return false
        }

        val requireUnmetered = Prefs.uploadWifiOnly

        if (isNetworkAvailable(requireUnmetered)) return true

        val type =
            if (requireUnmetered) JobInfo.NETWORK_TYPE_UNMETERED else JobInfo.NETWORK_TYPE_ANY

        // Try again when there is a network.
        val job = JobInfo.Builder(
            UploadJobConfig.JOB_ID,
            ComponentName(this, UploadService::class.java)
        )
            .setRequiredNetworkType(type)
            .setRequiresCharging(false)
            .build()

        (getSystemService(JOB_SCHEDULER_SERVICE) as? JobScheduler)?.schedule(job)

        return false
    }

    private fun isNetworkAvailable(requireUnmetered: Boolean): Boolean {
        val cm =
            getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false

        val cap = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false

        when {
            cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                return true
            }

            cap.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                return !requireUnmetered
            }

            cap.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                return true
            }
        }

        return false
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID, getString(R.string.uploads),
            NotificationManager.IMPORTANCE_LOW
        )

        channel.description = getString(R.string.uploads_notification_descriptions)
        channel.enableLights(false)
        channel.enableVibration(false)
        channel.setShowBadge(false)
        channel.lockscreenVisibility = Notification.VISIBILITY_SECRET

        val notificationManager = (getSystemService(NOTIFICATION_SERVICE) as? NotificationManager)
        notificationManager?.createNotificationChannel(channel)
    }

    private fun prepNotification(): Notification {

        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, HomeActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_app_notify)
            .setContentTitle(getString(R.string.uploading))
            .setDefaults(Notification.DEFAULT_LIGHTS)
            .setContentIntent(pendingIntent)
            .build()
    }
}
