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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.features.main.HomeActivity
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
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()

        // SECURITY: Force random SOCKS port (never use predictable 9050)
        // This must be done before the service starts the Tor daemon
        val torrcFile = getTorrc(this)
        torrcFile.parentFile?.mkdirs()
        torrcFile.writeText(TorConstants.DEFAULT_TORRC_CONFIG.trimIndent())

        // Register for status updates to update notification
        statusReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context?,
                    intent: Intent?,
                ) {
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
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        // Workaround for tor-android 0.4.9.5.1 bug:
        // The library's event handler (lambda$new$0) was changed to wait for
        // keyword=="NOTICE" with "Bootstrapped 100%", but setEvents() still only
        // subscribes to ["CIRC"]. Tor never sends NOTICE events unless subscribed,
        // so STATUS_ON is never broadcast. We fix this by adding "NOTICE" to the
        // subscription after the library's control port thread finishes its own setup.
        serviceScope.launch { subscribeToBootstrapEvents() }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        // Handle notification update intent - show simple "connected" message (no sensitive IP info)
        if (intent?.action == ACTION_UPDATE_NOTIFICATION) {
            updateNotification(getString(R.string.tor_notification_connected))
            return START_STICKY
        }

        // Create and show foreground notification
        val notification = createNotification(getString(R.string.tor_notification_connecting))

        startForeground(
            TorConstants.TOR_NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )

        return super.onStartCommand(intent, flags, startId)
    }

    private fun updateNotificationForStatus(status: String?) {
        val text =
            when (status) {
                STATUS_STARTING -> getString(R.string.tor_notification_connecting)

                STATUS_ON -> getString(R.string.tor_notification_connected)

                STATUS_STOPPING, STATUS_OFF -> return

                // Don't update notification when stopping
                else -> return
            }
        try {
            updateNotification(text)
        } catch (e: Exception) {
            // Ignore notification update failures (e.g., if service is stopping)
            AppLogger.i("TorForegroundService", "Failed to update notification", e)
        }
    }

    private fun updateNotification(contentText: String) {
        val notification = createNotification(contentText)
        // Use startForeground to update notification - this maintains the foreground service binding
        // and ensures the notification cannot be dismissed
        startForeground(
            TorConstants.TOR_NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    private fun createNotification(contentText: String): Notification {
        val pendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, HomeActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        val builder =
            NotificationCompat
                .Builder(this, TorConstants.TOR_NOTIFICATION_CHANNEL_ID)
                .setContentTitle(getString(R.string.tor_notification_title))
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_tor)
                .setContentIntent(pendingIntent)
                .setOngoing(false) // Allow dismissal for privacy
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setShowWhen(false) // Don't show timestamp

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
        serviceScope.cancel()
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

    /**
     * Workaround for tor-android 0.4.9.5.1 bug: the library's bootstrap event handler
     * checks for NOTICE events but the control port thread only subscribes to CIRC.
     *
     * We wait for the library's control port setup to complete (signalled by socksPort > 0,
     * which is set AFTER setEvents in the control port thread), then re-subscribe with NOTICE
     * added so Tor starts forwarding bootstrap log messages to the control connection.
     *
     * We also handle the edge case where Tor already reached 100% before we subscribed,
     * by querying the bootstrap phase directly and sending the STATUS_ON broadcast manually.
     */
    private suspend fun subscribeToBootstrapEvents() {
        for (attempt in 1..120) {
            delay(1_000L)
            if (getSocksPort() <= 0) continue

            val conn = getTorControlConnection() ?: continue

            try {
                conn.setEvents(listOf("CIRC", "NOTICE"))
                AppLogger.d("TorForegroundService: Subscribed to CIRC+NOTICE events (attempt $attempt)")

                // Edge case: bootstrap already at 100% before we subscribed
                val phase = getInfo("status/bootstrap-phase")
                if (phase != null && phase.contains("PROGRESS=100")) {
                    AppLogger.d("TorForegroundService: Already bootstrapped — broadcasting STATUS_ON")
                    val intent = Intent(ACTION_STATUS).apply {
                        setPackage(packageName)
                        putExtra(EXTRA_STATUS, STATUS_ON)
                    }
                    sendBroadcast(intent)
                }
                return
            } catch (e: Exception) {
                AppLogger.w("TorForegroundService: setEvents attempt $attempt failed", e)
            }
        }
        AppLogger.e("TorForegroundService: Timed out waiting to subscribe to NOTICE events")
    }

    private fun clearNotificationAndStop() {
        // Clear the notification
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(TorConstants.TOR_NOTIFICATION_ID)

        // Stop foreground and remove notification
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    companion object {
        const val ACTION_UPDATE_NOTIFICATION =
            "net.opendasharchive.openarchive.tor.UPDATE_NOTIFICATION"
    }
}
