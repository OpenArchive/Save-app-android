package net.opendasharchive.openarchive.services.tor

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import androidx.work.Worker
import androidx.work.WorkerParameters
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.SaveApp.Companion.TOR_SERVICE_CHANNEL
import org.openarchive.SaveTor
import timber.log.Timber

class TorService(private val context: Context, params: WorkerParameters) : Worker(context, params) {
    private val id = 1;

    override fun doWork() = try {
            setForegroundAsync(createForegroundInfo(context.getString(R.string.prefs_use_tor_summary)))
            SaveTor.start(storage = context.cacheDir.absolutePath)
            Result.success()
        } catch (e: Throwable) {
        Timber.e(e, "Error starting Tor")
        Result.failure()
    }

    override fun onStopped() {
        super.onStopped()
    }

    private fun createForegroundInfo(progress: String): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, TOR_SERVICE_CHANNEL)
            .setContentTitle(context.getString(R.string.prefs_use_tor_title))
            .setContentText(progress)
            .setSmallIcon(R.drawable.savelogo)
            .build()
        return ForegroundInfo(id, notification)
    }
}
