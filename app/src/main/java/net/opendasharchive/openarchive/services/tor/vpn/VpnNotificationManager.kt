package net.opendasharchive.openarchive.services.tor.vpn

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.features.main.MainActivity
import net.opendasharchive.openarchive.services.tor.TorConstants

/**
 * Manages notifications for the Tor VPN service.
 */
class VpnNotificationManager(private val context: Context) {

    companion object {
        const val NOTIFICATION_ID = TorConstants.TOR_NOTIFICATION_ID
    }

    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    fun buildForegroundServiceNotification(): Notification {
        return createNotification(
            title = context.getString(R.string.tor_notification_title),
            text = context.getString(R.string.tor_notification_connecting),
            progress = -1
        )
    }

    fun updateNotification(
        state: ConnectionState,
        dataUsage: DataUsage,
        hasConnectivity: Boolean
    ) {
        val (title, text, progress) = when (state) {
            ConnectionState.INIT -> Triple(
                context.getString(R.string.tor_notification_title),
                context.getString(R.string.tor_notification_idle),
                -1
            )
            ConnectionState.CONNECTING -> {
                val bootstrapProgress = VpnStatusObservable.bootstrapProgress.value
                Triple(
                    context.getString(R.string.tor_notification_title),
                    if (bootstrapProgress > 0) {
                        context.getString(R.string.tor_notification_bootstrap_progress, bootstrapProgress)
                    } else {
                        context.getString(R.string.tor_notification_connecting)
                    },
                    if (bootstrapProgress > 0) bootstrapProgress else -1
                )
            }
            ConnectionState.CONNECTED -> {
                val usageText = formatDataUsage(dataUsage)
                Triple(
                    context.getString(R.string.tor_notification_title),
                    if (hasConnectivity) {
                        context.getString(R.string.tor_notification_connected_with_usage, usageText)
                    } else {
                        context.getString(R.string.tor_notification_no_internet)
                    },
                    -1
                )
            }
            ConnectionState.DISCONNECTING -> Triple(
                context.getString(R.string.tor_notification_title),
                context.getString(R.string.tor_notification_disconnecting),
                -1
            )
            ConnectionState.DISCONNECTED -> Triple(
                context.getString(R.string.tor_notification_title),
                context.getString(R.string.tor_notification_disconnected),
                -1
            )
            ConnectionState.CONNECTION_ERROR -> Triple(
                context.getString(R.string.tor_notification_title),
                context.getString(R.string.tor_notification_error),
                -1
            )
        }

        val notification = createNotification(title, text, progress)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotification(title: String, text: String, progress: Int): Notification {
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, TorConstants.TOR_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_tor)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setShowWhen(false)

        if (progress >= 0) {
            builder.setProgress(100, progress, false)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        }

        return builder.build()
    }

    private fun formatDataUsage(dataUsage: DataUsage): String {
        val down = formatBytes(dataUsage.bytesReceived)
        val up = formatBytes(dataUsage.bytesSent)
        return "$down / $up"
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }

    fun cancelNotifications() {
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
