package net.opendasharchive.openarchive.services.tor.vpn

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Handler
import android.os.Handler.Callback
import android.os.IBinder
import android.os.Looper
import android.os.Messenger
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.core.logger.AppLogger
import java.io.Closeable
import java.lang.ref.WeakReference
import java.util.concurrent.BlockingQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * WorkManager-based launcher for the Tor VPN service.
 */
class VpnServiceLauncher(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val COMMAND = "command"
        private const val TAG = "VpnServiceLauncher"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO.limitedParallelism(1)) {
        val command = inputData.getString(COMMAND) ?: ""
        if (command != SaveTorVpnService.ACTION_START_VPN && command != SaveTorVpnService.ACTION_STOP_VPN) {
            return@withContext Result.success()
        }

        AppLogger.d("$TAG: Launching VPN service with action: $command")
        var vpnServiceConnection: TorVpnServiceConnection? = null
        val waitForForegroundSet = CountDownLatch(1)

        try {
            val appContext = context.applicationContext
            vpnServiceConnection = TorVpnServiceConnection(appContext)

            AppLogger.d("$TAG: Starting VPN service as foreground service...")
            val intent = Intent(appContext, SaveTorVpnService::class.java)
            val callback = Callback { msg ->
                if (msg.what == SaveTorVpnService.FOREGROUND_SERVICE_SET) {
                    waitForForegroundSet.countDown()
                    return@Callback true
                }
                return@Callback false
            }
            val handler = Handler(Looper.getMainLooper(), callback)
            intent.putExtra("handler", Messenger(handler))
            intent.putExtra("taskID", Random(System.currentTimeMillis()).nextInt())
            intent.action = command
            startServiceIntent(appContext, intent)

        } catch (e: InterruptedException) {
            AppLogger.e("$TAG: Interrupted while launching VPN service", e)
        } catch (e: IllegalStateException) {
            AppLogger.e("$TAG: Illegal state while launching VPN service", e)
        } finally {
            try {
                if (waitForForegroundSet.await(3, TimeUnit.SECONDS)) {
                    AppLogger.d("$TAG: Foreground service was set in SaveTorVpnService")
                } else {
                    AppLogger.w("$TAG: Timeout waiting for foreground service")
                }
            } catch (e: InterruptedException) {
                AppLogger.e("$TAG: Interrupted while waiting for foreground service", e)
            }
            vpnServiceConnection?.close()
        }

        Result.success()
    }

    private fun startServiceIntent(context: Context, intent: Intent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: IllegalStateException) {
            AppLogger.e("$TAG: Failed to start service", e)
        }
    }

    private class TorVpnServiceConnection(context: Context) : Closeable {
        private var context: Context?
        private var serviceConnection: ServiceConnection? = null
        private var service: WeakReference<SaveTorVpnService> = WeakReference(null)

        init {
            this.context = context
            initSynchronizedServiceConnection(context)
        }

        override fun close() {
            AppLogger.d("TorVpnServiceConnection: Unbinding service connection")
            serviceConnection?.let { connection ->
                try {
                    context?.unbindService(connection)
                } catch (e: IllegalArgumentException) {
                    AppLogger.w("TorVpnServiceConnection: Service not bound", e)
                }
                serviceConnection = null
                service.clear()
                context = null
            }
        }

        @Throws(InterruptedException::class)
        private fun initSynchronizedServiceConnection(context: Context) {
            val blockingQueue: BlockingQueue<SaveTorVpnService?> = LinkedBlockingQueue(1)

            this.serviceConnection = object : ServiceConnection {
                @Volatile
                var serviceBound: Boolean = false

                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    if (!serviceBound) {
                        serviceBound = true
                        try {
                            val binder = service as SaveTorVpnServiceBinder
                            blockingQueue.put(binder.service)
                        } catch (e: InterruptedException) {
                            AppLogger.e("TorVpnServiceConnection: Interrupted while binding service", e)
                        }
                    }
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    AppLogger.d("TorVpnServiceConnection: Service connection disconnected")
                }
            }

            serviceConnection?.let {
                val intent = Intent(context, SaveTorVpnService::class.java)
                context.bindService(intent, it, Context.BIND_AUTO_CREATE)
                service = WeakReference(blockingQueue.take())
            }
        }
    }
}
