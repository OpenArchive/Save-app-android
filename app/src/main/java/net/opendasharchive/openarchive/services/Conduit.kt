package net.opendasharchive.openarchive.services

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.webkit.MimeTypeMap
import com.google.common.net.UrlEscapers
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.analytics.api.AnalyticsEvent
import net.opendasharchive.openarchive.analytics.api.AnalyticsManager
import net.opendasharchive.openarchive.analytics.api.session.SessionTracker
import net.opendasharchive.openarchive.core.domain.Evidence
import net.opendasharchive.openarchive.core.domain.EvidenceStatus
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.core.repositories.MediaRepository
import net.opendasharchive.openarchive.core.repositories.ProjectRepository
import net.opendasharchive.openarchive.core.repositories.SpaceRepository
import net.opendasharchive.openarchive.services.internetarchive.IaConduit
import net.opendasharchive.openarchive.services.webdav.WebDavConduit
import net.opendasharchive.openarchive.upload.BroadcastManager
import net.opendasharchive.openarchive.upload.UploadEventBus
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.toJavaDate
import okhttp3.HttpUrl
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.GlobalContext
import org.witness.proofmode.storage.DefaultStorageProvider
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale

abstract class Conduit(
    protected var mEvidence: Evidence,
    protected val mContext: Context
) : KoinComponent {

    val id: Long get() = mEvidence.id

    protected val mediaRepository: MediaRepository by inject()
    protected val projectRepository: ProjectRepository by inject()
    protected val spaceRepository: SpaceRepository by inject()

    protected val analyticsManager: AnalyticsManager by inject()
    protected val sessionTracker: SessionTracker by inject()
    protected val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @SuppressLint("SimpleDateFormat")
    protected val mDateFormat = SimpleDateFormat(FOLDER_DATETIME_FORMAT)

    protected var mCancelled = false

    // Track upload start time for analytics
    private var uploadStartTime: Long = System.currentTimeMillis()

    init {
        // Track upload started
        trackUploadStarted()
    }

    private fun trackUploadStarted() {
        uploadStartTime = System.currentTimeMillis()

        scope.launch {
            val vault = spaceRepository.getSpaceById(mEvidence.vaultId)
            val backendType = vault?.type?.friendlyName ?: "Unknown"
            val fileSizeKB = mEvidence.contentLength / 1024
            val fileType = getFileType(mEvidence.mimeType)

            // Add breadcrumb for crash analysis
            AppLogger.breadcrumb("Upload Started", "$fileType to $backendType (${fileSizeKB}KB)")

            analyticsManager.trackUploadStarted(
                backendType = backendType,
                fileType = fileType,
                fileSizeKB = fileSizeKB
            )
        }
    }

    private fun getFileType(mimeType: String?): String {
        return when {
            mimeType == null -> "unknown"
            mimeType.startsWith("image/") -> "image"
            mimeType.startsWith("video/") -> "video"
            mimeType.startsWith("audio/") -> "audio"
            mimeType.startsWith("application/pdf") -> "document"
            mimeType.startsWith("application/") -> "document"
            mimeType.startsWith("text/") -> "text"
            else -> "other"
        }
    }

    /**
     * Gives a SiteController a chance to add metadata to the intent resulting from the ChooseAccounts process
     * that gets passed to each SiteController during publishing
     */
    @Throws(IOException::class)
    abstract suspend fun upload(): Boolean

    abstract suspend fun createFolder(url: String)

    open fun cancel() {
        mCancelled = true
    }

    fun getProof(): Array<out File> {
        if (!Prefs.useProofMode) return emptyArray()
        try {
            // Here we are simply fetching the files. Don't generate proof here. This is only called during upload.
            // Generating Proof here won't make sense because the file can be created well before it could be uploaded.
            val files = DefaultStorageProvider(mContext).getHashStorageDir(mEvidence.mediaHashString)
                ?.listFiles() ?: emptyArray()
            return files
        } catch (exception: FileNotFoundException) {
            AppLogger.e(exception)
            return emptyArray()
        } catch (exception: SecurityException) {
            AppLogger.e(exception)
            return emptyArray()
        }
    }

    /**
     * result is a site specific unique id that we can use to fetch the data,
     * build an embed tag, etc. for some sites this might be a URL
     */
    suspend fun jobSucceeded() {
        mEvidence = mEvidence.copy(
            progress = mEvidence.contentLength,
            status = EvidenceStatus.UPLOADED,
            uploadPercentage = 100
        )
        mediaRepository.updateEvidence(mEvidence)
        AppLogger.i("media item ${mEvidence.id} is uploaded and saved")

            // Track successful upload analytics
            val uploadDuration = (System.currentTimeMillis() - uploadStartTime) / 1000
            val fileSizeKB = mEvidence.contentLength / 1024
            val vault = spaceRepository.getSpaceById(mEvidence.vaultId)
            val backendType = vault?.type?.friendlyName ?: "Unknown"
            val fileType = getFileType(mEvidence.mimeType)

            // Calculate upload speed
            val uploadSpeedKBps = if (uploadDuration > 0) fileSizeKB / uploadDuration else 0

            // Add breadcrumb for crash analysis
            AppLogger.breadcrumb(
                "Upload Completed",
                "$fileType (${uploadDuration}s, ${uploadSpeedKBps}KB/s)"
            )

            analyticsManager.trackUploadCompleted(
                backendType = backendType,
                fileType = fileType,
                fileSizeKB = fileSizeKB,
                durationSeconds = uploadDuration,
                uploadSpeedKBps = uploadSpeedKBps
            )

            // Track in session
            sessionTracker.trackUploadCompleted()

            BroadcastManager.postSuccess(
                context = mContext,
                collectionId = mEvidence.submissionId,
                mediaId = mEvidence.id
            )
            UploadEventBus.emitChanged(
                projectId = mEvidence.archiveId,
                collectionId = mEvidence.submissionId,
                mediaId = mEvidence.id,
                progress = 100,
                isUploaded = true
            )
    }

    suspend fun jobFailed(exception: Throwable) {
        // If an upload was cancelled, track and return.
        if (mCancelled) {
            AppLogger.i("Upload cancelled", exception)

            // Add breadcrumb
            val vault = spaceRepository.getSpaceById(mEvidence.vaultId)
            val backendType = vault?.type?.friendlyName ?: "Unknown"
            val fileType = getFileType(mEvidence.mimeType)
            AppLogger.breadcrumb("Upload Cancelled", "$fileType to $backendType")

            // Track upload cancellation
            analyticsManager.trackEvent(
                AnalyticsEvent.UploadCancelled(
                    backendType = backendType,
                    fileType = fileType,
                    reason = "user_cancelled"
                )
            )

            return
        }

        mEvidence = mEvidence.copy(
            statusMessage = exception.localizedMessage ?: exception.message ?: exception.toString(),
            status = EvidenceStatus.ERROR
        )
        mediaRepository.updateEvidence(mEvidence)

        AppLogger.e(exception)

            // Track failed upload analytics (GDPR-compliant - no PII)
            val vault = spaceRepository.getSpaceById(mEvidence.vaultId)
            val backendType = vault?.type?.friendlyName ?: "Unknown"
            val fileType = getFileType(mEvidence.mimeType)
            val fileSizeKB = mEvidence.contentLength / 1024

            // Categorize error
            val errorCategory = when (exception) {
                is IOException -> "network"
                is FileNotFoundException -> "file_not_found"
                is SecurityException -> "permission"
                else -> "unknown"
            }

            analyticsManager.trackUploadFailed(
                backendType = backendType,
                fileType = fileType,
                errorCategory = errorCategory,
                fileSizeKB = fileSizeKB
            )

            // Track in session
            sessionTracker.trackUploadFailed()

            // Track error for drop-off analysis
            analyticsManager.trackError(
                errorCategory = errorCategory,
                screenName = "Upload",
                backendType = backendType
            )

            BroadcastManager.postChange(
                context = mContext,
                collectionId = mEvidence.submissionId,
                mediaId = mEvidence.id
            )
            UploadEventBus.emitChanged(
                projectId = mEvidence.archiveId,
                collectionId = mEvidence.submissionId,
                mediaId = mEvidence.id,
                progress = -1,
                isUploaded = false
            )
    }

    /**
     * Non-suspend version of jobFailed for use in callbacks.
     */
    fun jobFailedAsync(exception: Throwable) {
        scope.launch { jobFailed(exception) }
    }

    private var lastReportedProgress: Int? = null

    fun jobProgress(uploadedBytes: Long) {
        mEvidence = mEvidence.copy(progress = uploadedBytes)
        val progress = if (uploadedBytes > 0) (uploadedBytes.toFloat() / mEvidence.contentLength * 100).toInt() else 0
        if (progress > (lastReportedProgress ?: 0) + 1) {
            lastReportedProgress = progress
            AppLogger.i("Media Item ${mEvidence.id} progress: $progress/100")
            BroadcastManager.postProgress(
                context = mContext,
                collectionId = mEvidence.submissionId,
                mediaId = mEvidence.id,
                progress = progress,
            )
            UploadEventBus.emitChanged(
                projectId = mEvidence.archiveId,
                collectionId = mEvidence.submissionId,
                mediaId = mEvidence.id,
                progress = progress,
                isUploaded = false
            )
        }
    }

    /**
     * workaround to deal with some quirks in our data model?
     *
     * reads some values from mMedia and copies them to some other fields of mMedia
     */
    protected suspend fun sanitize() {
        val length = mEvidence.file.length()
        var updatedEvidence = mEvidence
        if (length > 0) updatedEvidence = updatedEvidence.copy(contentLength = length)

        val tags = updatedEvidence.tags.toMutableList()

        if (updatedEvidence.isFlagged) {
            val flagText = getFlagText()
            if (!tags.contains(flagText)) tags.add(flagText)
        } else {
            tags.remove(getFlagText())
        }

        updatedEvidence = updatedEvidence.copy(tags = tags)

        // Update to the latest project license.
        val project = projectRepository.getProject(mEvidence.archiveId)
        updatedEvidence = updatedEvidence.copy(licenseUrl = project?.licenseUrl)

        mEvidence = updatedEvidence
    }

    protected suspend fun getPath(): List<String>? {
        val project = projectRepository.getProject(mEvidence.archiveId)
        val projectName = project?.description ?: return null

        val submission = projectRepository.getActiveSubmission(mEvidence.archiveId)
        val collectionDate =
            submission.uploadDate ?: mEvidence.createDate ?: net.opendasharchive.openarchive.util.DateUtils.nowDateTime

        val javaDate = collectionDate.toJavaDate()
        val collectionName = mDateFormat.format(javaDate)

        val path = mutableListOf(projectName, collectionName)

        if (mEvidence.isFlagged) {
            path.add(getFlagText())
        }

        return path
    }

    protected suspend fun createFolders(base: HttpUrl?, path: List<String>) {
        val tmp = mutableListOf<String>()

        for (segment in path) {
            tmp.add(segment)

            if (mCancelled) throw Exception("Cancelled")

            val url = construct(base, tmp)

            createFolder(url)
        }
    }

    /**
     * Constructs, either a full URL or a path from the given arguments, depending if a `base` is given.
     *
     * If there's only a path to be constructed, then the path will have a leading slash and will
     * not be escaped, as the Dropbox client likes it like that.
     *
     * If there's a full URL to be constructed, it *will* be escaped properly.
     */
    protected fun construct(base: HttpUrl?, path: List<String>, file: String? = null): String {
        val builder = base?.newBuilder() ?: HttpUrl.Builder().scheme("http").host("ignored")

        path.forEach { builder.addPathSegment(it) }

        if (!file.isNullOrBlank()) builder.addPathSegment(file)

        return if (base != null) {
            builder.toString()
        } else {
            "/${builder.build().pathSegments.joinToString("/")}"
        }
    }

    protected fun construct(path: List<String>, file: String? = null): String {
        return construct(null, path, file)
    }

    /**
     * Generate JSON encoded string of metadata corresponding Media currently
     * stored in `this.mMedia`.
     */
    protected fun getMetadata(): String {
        val gson = GsonBuilder()
            .setPrettyPrinting()
            .excludeFieldsWithoutExposeAnnotation()
            .create()

        return gson.toJson(this.mEvidence, Evidence::class.java)
    }

    /**
     * Always use english, since this affects the target server, not the local device.
     */
    private fun getFlagText(): String {
        val conf = Configuration(mContext.resources.configuration)
        conf.setLocale(Locale.US)

        return mContext.createConfigurationContext(conf).getString(R.string.status_flagged)
    }

    companion object {
        const val FOLDER_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'GMT'ZZZZZ"

        /**
         * 2 MByte
         */
        const val CHUNK_SIZE: Long = 2 * 1024 * 1024

        /**
         * 10 MByte
         */
        const val CHUNK_FILESIZE_THRESHOLD = 10 * 1024 * 1024

        suspend fun get(evidence: Evidence, context: Context): Conduit? {
            val spaceRepository: SpaceRepository = GlobalContext.get().get()
            val vault = spaceRepository.getSpaceById(evidence.vaultId) ?: return null

            return when (vault.type) {
                net.opendasharchive.openarchive.core.domain.VaultType.INTERNET_ARCHIVE -> IaConduit(evidence, context)
                net.opendasharchive.openarchive.core.domain.VaultType.PRIVATE_SERVER -> WebDavConduit(evidence, context)
                else -> null
            }
        }

        fun getUploadFileName(evidence: Evidence, escapeTitle: Boolean = false): String {
            var ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(evidence.mimeType)
            if (ext.isNullOrEmpty()) {
                ext = when {
                    evidence.mimeType.startsWith("image") -> "jpg"

                    evidence.mimeType.startsWith("video") -> "mp4"

                    evidence.mimeType.startsWith("audio") -> "m4a"

                    else -> "txt"
                }
            }

            var title = evidence.title

            if (title.isBlank()) title = evidence.mediaHashString

            if (escapeTitle) {
                title = UrlEscapers.urlPathSegmentEscaper().escape(title) ?: title
            }

            if (!title.endsWith(".$ext")) {
                return "$title.$ext"
            }

            return title
        }
    }
}
