package net.opendasharchive.openarchive.services.tor

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.features.main.MainActivity
import org.torproject.jni.TorService

/**
 * Foreground service that wraps the embedded Tor daemon.
 *
 * This service extends TorService directly from the tor-android library
 * and adds foreground notification support to comply with Android's
 * background service restrictions.
 *
 * SECURITY: Forces random SOCKS port allocation to prevent port-squatting attacks.
 */
class TorForegroundService : TorService() {

    private var statusReceiver: BroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()

        // SECURITY: Force random SOCKS port (never use predictable 9050)
        // This must be done before the service starts the Tor daemon
        val torrcFile = getTorrc(this)
        torrcFile.parentFile?.mkdirs()
        torrcFile.writeText(TorConstants.DEFAULT_TORRC_CONFIG.trimIndent())

        // Register for status updates to update notification
        statusReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == ACTION_STATUS) {
                    val status = intent.getStringExtra(EXTRA_STATUS)
                    updateNotificationForStatus(status)
                }
            }
        }

        val filter = IntentFilter(ACTION_STATUS)
        ContextCompat.registerReceiver(
            this,
            statusReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Handle notification update intent - show simple "connected" message (no sensitive IP info)
        if (intent?.action == ACTION_UPDATE_NOTIFICATION) {
            updateNotification(getString(R.string.tor_notification_connected))
            return START_STICKY
        }

        // Create and show foreground notification
        val notification = createNotification(getString(R.string.tor_notification_connecting))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                TorConstants.TOR_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(TorConstants.TOR_NOTIFICATION_ID, notification)
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun updateNotificationForStatus(status: String?) {
        val text = when (status) {
            STATUS_STARTING -> getString(R.string.tor_notification_connecting)
            STATUS_ON -> getString(R.string.tor_notification_connected)
            else -> return
        }
        updateNotification(text)
    }

    private fun updateNotification(contentText: String) {
        val notification = createNotification(contentText)
        // Use startForeground to update notification - this maintains the foreground service binding
        // and ensures the notification cannot be dismissed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                TorConstants.TOR_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(TorConstants.TOR_NOTIFICATION_ID, notification)
        }
    }

    private fun createNotification(contentText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, TorConstants.TOR_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.tor_notification_title))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_tor)
            .setContentIntent(pendingIntent)
            .setOngoing(false)  // Allow dismissal for privacy
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setShowWhen(false)  // Don't show timestamp

        // For Android 12+, ensure immediate foreground service notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        }

        return builder.build()
    }

    /**
     * Called when the app is swiped away from recent apps.
     * Clean up Tor service and notification.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Stop the service and clear notification when app is removed from recents
        clearNotificationAndStop()
    }

    override fun onDestroy() {
        statusReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (_: IllegalArgumentException) {
                // Receiver not registered
            }
        }
        clearNotificationAndStop()
        super.onDestroy()
    }

    private fun clearNotificationAndStop() {
        // Clear the notification
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(TorConstants.TOR_NOTIFICATION_ID)

        // Stop foreground and remove notification
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    companion object {
        const val ACTION_UPDATE_NOTIFICATION = "net.opendasharchive.openarchive.tor.UPDATE_NOTIFICATION"
        const val EXTRA_EXIT_IP = "exit_ip"
        const val EXTRA_EXIT_COUNTRY = "exit_country"
    }
}
