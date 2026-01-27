package net.opendasharchive.openarchive.services.tor.vpn

import android.app.Notification
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
import android.net.VpnService
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.system.OsConstants
import androidx.core.content.IntentCompat
import androidx.lifecycle.Observer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.BuildConfig
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.services.tor.TorConnectionInfo
import net.opendasharchive.openarchive.services.tor.TorConstants
import net.opendasharchive.openarchive.util.Prefs
import org.torproject.onionmasq.ConnectivityHandler
import org.torproject.onionmasq.OnionMasq
import org.torproject.onionmasq.events.BootstrapEvent
import org.torproject.onionmasq.events.ClosedConnectionEvent
import org.torproject.onionmasq.events.ConnectivityEvent
import org.torproject.onionmasq.events.FailedConnectionEvent
import org.torproject.onionmasq.events.NewConnectionEvent
import org.torproject.onionmasq.events.NewDirectoryEvent
import org.torproject.onionmasq.events.OnionmasqEvent
import java.lang.ref.WeakReference
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * VPN Service that wraps OnionMasq (Arti-based Tor) for the Save app.
 *
 * IMPORTANT: This service routes ONLY Save app traffic through Tor using
 * addAllowedApplication(). Other apps on the device are completely unaffected.
 */
class SaveTorVpnService : VpnService() {

    companion object {
        private const val TAG = "SaveTorVpnService"

        const val ACTION_START_VPN = "net.opendasharchive.openarchive.tor.START_VPN"
        const val ACTION_STOP_VPN = "net.opendasharchive.openarchive.tor.STOP_VPN"
        const val FOREGROUND_SERVICE_SET = 1
    }

    private lateinit var notificationManager: VpnNotificationManager
    private lateinit var connectivityHandler: ConnectivityHandler

    private val binder: IBinder = SaveTorVpnServiceBinder(WeakReference(this))

    private val job = SupervisorJob()
    private var collectionJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + job)
    private var startCoroutineScope = CoroutineScope(Dispatchers.IO.limitedParallelism(1) + job)
    private var observer: Observer<OnionmasqEvent>? = null
    private var dataUsageTimer: Timer? = null

    private val vpnStateFlow = combine(
        VpnStatusObservable.statusLiveData,
        VpnStatusObservable.dataUsage,
        VpnStatusObservable.hasInternetConnectivity
    ) { connectionState, dataUsage, hasInternetConnectivity ->
        Triple(connectionState, dataUsage, hasInternetConnectivity)
    }.stateIn(
        scope = coroutineScope,
        started = SharingStarted.Eagerly,
        initialValue = Triple(
            VpnStatusObservable.statusLiveData.value,
            VpnStatusObservable.dataUsage.value,
            VpnStatusObservable.hasInternetConnectivity.value
        )
    )

    private val mainHandler: Handler by lazy {
        Handler(Looper.getMainLooper())
    }

    override fun onBind(intent: Intent?): IBinder {
        AppLogger.d("$TAG: Service bound")
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        AppLogger.d("$TAG: Service unbound")
        return super.onUnbind(intent)
    }

    override fun onCreate() {
        super.onCreate()
        AppLogger.d("$TAG: Service created")
        notificationManager = VpnNotificationManager(this)
        connectivityHandler = ConnectivityHandler(this)
        connectivityHandler.register()
        OnionMasq.bindVPNService(SaveTorVpnService::class.java)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        AppLogger.d("$TAG: onStartCommand - action: ${intent?.action}")

        // Show foreground notification immediately to avoid ANR
        val notification: Notification = notificationManager.buildForegroundServiceNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(TorConstants.TOR_NOTIFICATION_ID, notification, FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(TorConstants.TOR_NOTIFICATION_ID, notification)
        }

        // Send confirmation to handler if present
        val handler = intent?.let {
            IntentCompat.getParcelableExtra(it, "handler", Messenger::class.java)
        }
        handler?.let {
            try {
                val message = Message()
                message.what = FOREGROUND_SERVICE_SET
                it.send(message)
            } catch (e: Exception) {
                AppLogger.w("$TAG: Failed to send message to handler", e)
            }
        }

        val action = intent?.action ?: ""
        when (action) {
            ACTION_START_VPN -> {
                VpnStatusObservable.update(ConnectionState.CONNECTING)
                establishVpn()
            }
            ACTION_STOP_VPN -> {
                stop(onError = false)
            }
            else -> {
                // Always-on VPN boot
                if (intent == null || intent.component?.packageName != packageName) {
                    VpnStatusObservable.isAlwaysOnBooting.set(true)
                    VpnStatusObservable.update(ConnectionState.CONNECTING)
                    establishVpn()
                }
            }
        }

        return START_STICKY
    }

    override fun onRevoke() {
        AppLogger.d("$TAG: VPN permission revoked")
        stop(onError = false)
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        AppLogger.d("$TAG: Service destroyed")
        notificationManager.cancelNotifications()
        if (VpnStatusObservable.statusLiveData.value !== ConnectionState.CONNECTION_ERROR) {
            VpnStatusObservable.update(ConnectionState.DISCONNECTED)
        }
        connectivityHandler.unregister()
    }

    private fun stop(onError: Boolean) {
        AppLogger.d("$TAG: Stopping VPN (onError: $onError)")

        if (onError) {
            VpnStatusObservable.update(ConnectionState.CONNECTION_ERROR)
        } else {
            VpnStatusObservable.update(ConnectionState.DISCONNECTING)
        }

        startCoroutineScope.cancel()
        // Recreate the coroutine scope for potential restart
        startCoroutineScope = CoroutineScope(Dispatchers.IO.limitedParallelism(1) + job)

        OnionMasq.stop()
        removeObservers()

        // Cancel and nullify timer
        dataUsageTimer?.cancel()
        dataUsageTimer = null

        VpnStatusObservable.reset()

        try {
            OnionMasq.unbindVPNService()
        } catch (e: IllegalArgumentException) {
            AppLogger.w("$TAG: Error unbinding VPN service", e)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createObservers() {
        observer = Observer<OnionmasqEvent> { event: OnionmasqEvent ->
            when (event) {
                is BootstrapEvent -> {
                    AppLogger.d("$TAG: Bootstrap progress: ${event.bootstrapPercent}% - ${event.bootstrapStatus}")
                    VpnStatusObservable.updateBootstrapProgress(event.bootstrapPercent)
                    if (event.isReadyForTraffic) {
                        AppLogger.i("$TAG: Tor is ready for traffic")
                        VpnStatusObservable.update(ConnectionState.CONNECTED)
                    }
                }
                is NewConnectionEvent -> {
                    AppLogger.d("$TAG: New connection: ${event.proxySrc} -> ${event.torDst}")
                }
                is FailedConnectionEvent -> {
                    AppLogger.w("$TAG: Connection failed: ${event.error}")
                }
                is ClosedConnectionEvent -> {
                    if (event.error != null) {
                        AppLogger.w("$TAG: Connection closed with error: ${event.error}")
                    }
                }
                is NewDirectoryEvent -> {
                    AppLogger.d("$TAG: New directory info received")
                }
                is ConnectivityEvent -> {
                    VpnStatusObservable.updateInternetConnectivity(event.hasInternetConnectivity)
                }
            }
        }
    }

    private fun startListeningObservers() {
        observer?.let {
            mainHandler.post { OnionMasq.getEventObservable().observeForever(it) }
        }

        collectionJob = coroutineScope.launch {
            vpnStateFlow.collect { triple ->
                notificationManager.updateNotification(
                    state = triple.first,
                    dataUsage = triple.second,
                    hasConnectivity = triple.third
                )
            }
        }
    }

    private fun removeObservers() {
        observer?.let {
            mainHandler.post { OnionMasq.getEventObservable().removeObserver(it) }
        }
        collectionJob?.cancel()
    }

    private fun prepareVpnProfile(): Builder {
        val builder = Builder()

        try {
            builder.addAllowedApplication(BuildConfig.APPLICATION_ID)
            AppLogger.i("$TAG: VPN configured for Save app only (${BuildConfig.APPLICATION_ID})")
        } catch (e: Exception) {
            AppLogger.e("$TAG: Failed to add allowed application", e)
            throw e
        }

        builder.setSession(getString(R.string.tor_vpn_session_name))
        builder.addRoute("0.0.0.0", 0)
        builder.addRoute("::", 0)
        builder.addAddress("169.254.42.1", 16)
        builder.addAddress("fc00::", 7)
        builder.addDnsServer("169.254.42.53")
        builder.addDnsServer("fe80::53")
        builder.allowFamily(OsConstants.AF_INET)
        builder.allowFamily(OsConstants.AF_INET6)
        builder.setMtu(1500)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setMetered(false)
        }

        return builder
    }

    private fun establishVpn() {
        try {
            AppLogger.d("$TAG: Establishing VPN...")

            if (OnionMasq.isRunning()) {
                AppLogger.d("$TAG: Stopping previous Tor session...")
                OnionMasq.stop()
                AppLogger.d("$TAG: Previous Tor session stopped")
            } else {
                AppLogger.d("$TAG: Tor session not yet running, starting...")
                createObservers()
                startListeningObservers()

                // Create a new timer for data usage updates
                dataUsageTimer = Timer()
                dataUsageTimer?.schedule(object : TimerTask() {
                    override fun run() {
                        try {
                            VpnStatusObservable.updateDataUsage(
                                OnionMasq.getBytesReceived(),
                                OnionMasq.getBytesSent()
                            )
                        } catch (e: Exception) {
                            AppLogger.w("$TAG: Error updating data usage", e)
                        }
                    }
                }, 1000, 1000)
            }

            val builder = prepareVpnProfile()
            val fd = builder.establish()

            if (fd == null) {
                throw IllegalStateException("VPN permission not granted or another VPN is active")
            }

            val waitCoroutineStarted = CountDownLatch(1)
            startCoroutineScope.launch {
                waitCoroutineStarted.countDown()
                try {
                    val bridgeLines = getBridgeLines()
                    if (bridgeLines != null) {
                        val lineCount = bridgeLines.lines().filter { it.isNotBlank() }.size
                        AppLogger.i("$TAG: Starting OnionMasq with bridges enabled (${Prefs.bridgeType}, $lineCount bridge lines)")
                    } else {
                        AppLogger.i("$TAG: Starting OnionMasq without bridges")
                    }
                    OnionMasq.start(fd.detachFd(), bridgeLines)
                } catch (e: Exception) {
                    mainHandler.post { handleException(e, "OnionMasq error: ${e.message}") }
                }
            }

            val success = waitCoroutineStarted.await(15, TimeUnit.SECONDS)
            if (!success) {
                throw IllegalStateException("Timeout waiting for OnionMasq coroutine to start")
            }

        } catch (e: Exception) {
            handleException(e, "Failed to establish VPN")
        }
    }

    private fun handleException(e: Exception, msg: String) {
        AppLogger.e("$TAG: $msg", e)
        stop(onError = true)
    }

    private fun getBridgeLines(): String? {
        if (!Prefs.useBridges) {
            AppLogger.d("$TAG: Bridges disabled in preferences")
            return null
        }

        val bridgeType = Prefs.bridgeType
        val filename = when (bridgeType) {
            "obfs4" -> "obfs4.txt"
            "snowflake" -> "snowflake.txt"
            else -> {
                AppLogger.w("$TAG: Unknown bridge type: $bridgeType")
                return null
            }
        }

        return try {
            val content = assets.open(filename).bufferedReader().use { it.readText() }
            AppLogger.d("$TAG: Loaded $bridgeType bridges from $filename (${content.length} bytes)")
            content
        } catch (e: Exception) {
            AppLogger.e("$TAG: Failed to read bridge file: $filename", e)
            null
        }
    }

    fun getExitCountryCode(): String? {
        return try {
            val uid = applicationInfo.uid
            val countryCodes = OnionMasq.getCircuitCountryCodesForAppUid(uid)
            countryCodes.lastOrNull()?.countryCodes?.lastOrNull()
        } catch (e: Exception) {
            AppLogger.w("$TAG: Failed to get exit country code", e)
            null
        }
    }

    fun getConnectionInfo(): TorConnectionInfo? {
        if (VpnStatusObservable.statusLiveData.value != ConnectionState.CONNECTED) {
            return null
        }
        val exitCountry = getExitCountryCode()
        return TorConnectionInfo(
            exitIp = "VPN Mode",
            exitCountry = exitCountry
        )
    }
}
