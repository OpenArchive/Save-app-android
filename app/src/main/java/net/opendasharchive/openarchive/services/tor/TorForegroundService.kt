package net.opendasharchive.openarchive.services.tor

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.features.main.MainActivity
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.torproject.jni.TorService
import timber.log.Timber

private const val TOR_SERVICE_ID = 2602
internal const val TOR_SERVICE_CHANNEL = "tor_service_channel"

class TorForegroundService : TorService(), KoinComponent {

    private val torRepo: ITorRepository by inject()

    inner class TorServiceBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Timber.d("intent = $intent")
            when (intent.action) {
                ACTION_ERROR -> {
                    val errorText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    Timber.d("error = $errorText")
                    torRepo.updateTorStatus(TorStatus.ERROR)
                }

                ACTION_STATUS -> {
                    val status = intent.getStringExtra(EXTRA_STATUS)
                    Timber.d("Tor status = $status")

                    when (status) {
                        STATUS_ON -> {
                            updateNotification("Connected")
                            torRepo.updateTorStatus(TorStatus.CONNECTED)
                        }

                        STATUS_OFF -> {
                            torRepo.updateTorStatus(TorStatus.DISCONNECTED)
                        }

                        STATUS_STOPPING -> {
                            torRepo.updateTorStatus(TorStatus.DISCONNECTING)
                        }

                        STATUS_STARTING -> {
                            torRepo.updateTorStatus(TorStatus.CONNECTING)
                        }

                        else -> Timber.d("Got rogue action: ${intent.action}")
                    }
                }
            }
        }
    }

    private var receiver = TorServiceBroadcastReceiver()

    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        registerBroadcastRecivers(receiver)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(TOR_SERVICE_ID, createNotification("Tor is starting"), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)

        torRepo.updatePorts(this.httpTunnelPort, this.socksPort)

        return START_STICKY
    }

    private fun createNotification(text: String): Notification {
        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        return NotificationCompat.Builder(this, TOR_SERVICE_CHANNEL)
            .setContentTitle("Tor Service")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_tor)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    private fun registerBroadcastRecivers(receiver: BroadcastReceiver) {
        LocalBroadcastManager.getInstance(applicationContext)
            .registerReceiver(receiver, IntentFilter(ACTION_STATUS))
    }

    fun updateNotification(status: String) {
        val notification = createNotification(status)
        notificationManager.notify(TOR_SERVICE_ID, notification)
    }
}