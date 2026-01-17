package net.opendasharchive.openarchive.services.tor

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.IBinder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.core.logger.AppLogger
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.torproject.jni.TorService
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.math.pow

/**
 * Singleton manager for the embedded Tor service.
 *
 * Provides:
 * - StateFlow for observing Tor status changes
 * - Dynamic SOCKS port retrieval (for security)
 * - Service lifecycle management (start/stop)
 *
 * Usage:
 * ```
 * val torManager: TorServiceManager = get() // from Koin
 *
 * // Observe status
 * torManager.torStatus.collect { status ->
 *     when (status) {
 *         is TorStatus.On -> // Tor is ready
 *         is TorStatus.Starting -> // Still connecting
 *         ...
 *     }
 * }
 *
 * // Get dynamic SOCKS port (only valid when status is On)
 * val port = torManager.socksPort.value
 *
 * // Control service
 * torManager.start()
 * torManager.stop()
 * ```
 */
class TorServiceManager(
    private val context: Context,
) {
    private val _torStatus = MutableStateFlow<TorStatus>(TorStatus.Idle)
    val torStatus: StateFlow<TorStatus> = _torStatus.asStateFlow()

    private val _socksPort = MutableStateFlow(0)
    val socksPort: StateFlow<Int> = _socksPort.asStateFlow()

    private val _httpTunnelPort = MutableStateFlow(0)
    val httpTunnelPort: StateFlow<Int> = _httpTunnelPort.asStateFlow()

    private var torService: TorService? = null
    private var isBound = false

    private val serviceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                service: IBinder?,
            ) {
                AppLogger.d("TorServiceManager: Service connected")
                torService = (service as? TorService.LocalBinder)?.service
                isBound = true

                // Get the dynamically allocated ports
                torService?.let { svc ->
                    _socksPort.value = svc.socksPort
                    _httpTunnelPort.value = svc.httpTunnelPort
                    AppLogger.d("TorServiceManager: SOCKS port = ${_socksPort.value}, HTTP tunnel port = ${_httpTunnelPort.value}")
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                AppLogger.d("TorServiceManager: Service disconnected")
                torService = null
                isBound = false
                _socksPort.value = 0
                _httpTunnelPort.value = 0
            }
        }

    private val torStatusReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                if (intent?.action != TorService.ACTION_STATUS) return

                val status = intent.getStringExtra(TorService.EXTRA_STATUS)
                AppLogger.d("TorServiceManager: Received status broadcast: $status")

                when (status) {
                    TorService.STATUS_STARTING -> {
                        _torStatus.value = TorStatus.Starting
                    }

                    TorService.STATUS_ON -> {
                        // Update ports when Tor is connected
                        torService?.let { svc ->
                            _socksPort.value = svc.socksPort
                            _httpTunnelPort.value = svc.httpTunnelPort
                        }
                        _torStatus.value = TorStatus.On
                    }

                    TorService.STATUS_OFF -> {
                        _torStatus.value = TorStatus.Off
                        _socksPort.value = 0
                        _httpTunnelPort.value = 0
                    }

                    TorService.STATUS_STOPPING -> {
                        _torStatus.value = TorStatus.Off
                    }

                    else -> {
                        AppLogger.w("TorServiceManager: Unknown status: $status")
                    }
                }
            }
        }

    init {
        // Register broadcast receiver for status updates
        val filter = IntentFilter(TorService.ACTION_STATUS)
        ContextCompat.registerReceiver(
            context,
            torStatusReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    /**
     * Starts the Tor service.
     * The service runs as a foreground service to comply with Android restrictions.
     */
    fun start() {
        AppLogger.d("TorServiceManager: Starting Tor service")
        _torStatus.value = TorStatus.Starting

        val serviceIntent = Intent(context, TorForegroundService::class.java)

        // Start as foreground service
        ContextCompat.startForegroundService(context, serviceIntent)

        // Bind to get service instance for port queries
        context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    /**
     * Stops the Tor service.
     */
    fun stop() {
        AppLogger.d("TorServiceManager: Stopping Tor service")

        // Unbind if bound
        if (isBound) {
            try {
                context.unbindService(serviceConnection)
            } catch (e: IllegalArgumentException) {
                AppLogger.w("TorServiceManager: Service not bound", e)
            }
            isBound = false
        }

        // Stop the service
        val serviceIntent = Intent(context, TorForegroundService::class.java)
        context.stopService(serviceIntent)

        _torStatus.value = TorStatus.Off
        _socksPort.value = 0
        _httpTunnelPort.value = 0
        torService = null
    }

    /**
     * Returns true if Tor is currently connected and ready for use.
     */
    fun isReady(): Boolean {
        val status = _torStatus.value
        return (status == TorStatus.On || status is TorStatus.Verified) && _socksPort.value > 0
    }

    /**
     * Fetches the country name for a given IP address using free geolocation APIs.
     * Routes the request through Tor for privacy.
     * Tries multiple HTTPS APIs as fallbacks since some have rate limits or block Tor.
     *
     * @param ip The IP address to look up
     * @param torClient OkHttpClient configured to use Tor SOCKS proxy
     * @return Country name or null if lookup fails
     */
    private suspend fun getExitCountry(
        ip: String,
        torClient: OkHttpClient,
    ): String? =
        withContext(Dispatchers.IO) {
            // Try primary API (ipwho.is - free, no rate limits, returns JSON)
            try {
                val request = Request.Builder().url("https://ipwho.is/$ip").build()

                val response = torClient.newCall(request).execute()
                val body = response.body?.string()

                if (response.isSuccessful && body != null) {
                    val json = JSONObject(body)
                    if (json.optBoolean("success", true)) {
                        val country = json.optString("country", "")
                        if (country.isNotEmpty()) {
                            AppLogger.d("TorServiceManager: Exit country resolved: $country")
                            return@withContext country
                        }
                    }
                }
                AppLogger.d("TorServiceManager: Primary geolocation API failed: ${response.code}")
            } catch (e: Exception) {
                AppLogger.w("TorServiceManager: ipwho.is country lookup failed", e)
            }

            // Try secondary API (freeipapi.com - free, generous limits)
            try {
                val request = Request.Builder().url("https://freeipapi.com/api/json/$ip").build()

                val response = torClient.newCall(request).execute()
                val body = response.body?.string()

                if (response.isSuccessful && body != null) {
                    val json = JSONObject(body)
                    val country = json.optString("countryName", "")
                    if (country.isNotEmpty()) {
                        AppLogger.d("TorServiceManager: Exit country resolved (fallback 1): $country")
                        return@withContext country
                    }
                }
                AppLogger.d("TorServiceManager: Secondary geolocation API failed: ${response.code}")
            } catch (e: Exception) {
                AppLogger.w("TorServiceManager: freeipapi.com country lookup failed", e)
            }

            // Try tertiary API (ipapi.co - returns plain text country name)
            try {
                val request = Request.Builder().url("https://ipapi.co/$ip/country_name/").build()

                val response = torClient.newCall(request).execute()
                val body = response.body?.string()?.trim()

                if (response.isSuccessful && !body.isNullOrEmpty() && !body.startsWith("{") &&
                    !body.contains(
                        "error",
                    )
                ) {
                    AppLogger.d("TorServiceManager: Exit country resolved (fallback 2): $body")
                    return@withContext body
                }
                AppLogger.d("TorServiceManager: Tertiary geolocation API failed: ${response.code}")
            } catch (e: Exception) {
                AppLogger.w("TorServiceManager: Tertiary country lookup failed", e)
            }

            AppLogger.w("TorServiceManager: All geolocation APIs failed")
            null
        }

    /**
     * Creates an OkHttpClient for verification with certificate pinning.
     *
     * SECURITY: Pins the certificate for check.torproject.org to prevent
     * man-in-the-middle attacks during verification.
     */
    private fun createVerificationClient(port: Int): OkHttpClient {
        // Certificate pinning for check.torproject.org
        // These are SHA-256 hashes of the certificate public keys
        val certificatePinner =
            CertificatePinner
                .Builder()
                .add(
                    "check.torproject.org",
                    // Primary certificate pin (Let's Encrypt)
                    "sha256/jQJTbIh0grw0/1TkHSumWb+Fs0Ggogr621gT3PvPKG0=",
                    // Backup pin (ISRG Root X1)
                    "sha256/C5+lpZ7tcVwmwQIMcRtPbsQtWLABXhQzejna0wHFr8M=",
                ).build()

        return OkHttpClient
            .Builder()
            .proxy(
                Proxy(
                    Proxy.Type.SOCKS,
                    InetSocketAddress(TorConstants.SOCKS5_PROXY_ADDRESS, port),
                ),
            ).certificatePinner(certificatePinner)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Verifies that traffic is actually routing through the Tor network.
     *
     * This makes a request to check.torproject.org through the SOCKS proxy
     * to confirm the connection is working properly.
     *
     * Features:
     * - Certificate pinning for check.torproject.org
     * - Exponential backoff retry on failure (up to MAX_VERIFICATION_RETRIES)
     *
     * @return TorVerificationResult containing verification status and exit IP
     */
    suspend fun verifyTorConnection(): TorVerificationResult =
        withContext(Dispatchers.IO) {
            val port = _socksPort.value
            if (port <= 0) {
                return@withContext TorVerificationResult(
                    isUsingTor = false,
                    error = "Tor SOCKS port not available",
                )
            }

            var lastError: String? = null
            var attempt = 0

            while (attempt < MAX_VERIFICATION_RETRIES) {
                try {
                    val result = attemptVerification(port)
                    if (result.isUsingTor || result.error == null) {
                        return@withContext result
                    }
                    lastError = result.error
                } catch (e: Exception) {
                    lastError = e.message ?: "Unknown error"
                    AppLogger.w("TorServiceManager: Verification attempt ${attempt + 1} failed", e)
                }

                attempt++
                if (attempt < MAX_VERIFICATION_RETRIES) {
                    // Exponential backoff: 1s, 2s, 4s, 8s... (capped at 30s)
                    val delayMs =
                        min(
                            INITIAL_RETRY_DELAY_MS * 2.0.pow(attempt - 1).toLong(),
                            MAX_RETRY_DELAY_MS,
                        )
                    AppLogger.d(
                        "TorServiceManager: Retrying verification in ${delayMs}ms (attempt ${attempt + 1}/$MAX_VERIFICATION_RETRIES)",
                    )
                    delay(delayMs)
                }
            }

            AppLogger.e("TorServiceManager: Verification failed after $MAX_VERIFICATION_RETRIES attempts")
            return@withContext TorVerificationResult(
                isUsingTor = false,
                error = lastError ?: "Verification failed after $MAX_VERIFICATION_RETRIES attempts",
            )
        }

    /**
     * Single verification attempt with certificate pinning.
     */
    private suspend fun attemptVerification(port: Int): TorVerificationResult =
        withContext(Dispatchers.IO) {
            val torClient = createVerificationClient(port)

            val request = Request.Builder().url(TOR_CHECK_API_URL).build()

            val response = torClient.newCall(request).execute()
            val body = response.body?.string()

            if (response.isSuccessful && body != null) {
                val json = JSONObject(body)
                val isTor = json.optBoolean("IsTor", false)
                val ip = json.optString("IP", "")

                AppLogger.d("TorServiceManager: Verification result - IsTor: $isTor")

                if (isTor && ip.isNotEmpty()) {
                    // Lookup exit country (non-blocking, country may be null)
                    val country = getExitCountry(ip, torClient)

                    // Create connection info with IP and country
                    val connectionInfo =
                        TorConnectionInfo(
                            exitIp = ip,
                            exitCountry = country,
                        )

                    // Update status to Verified with full connection info
                    _torStatus.value = TorStatus.Verified(connectionInfo)

                    // Update the notification to show verified status
                    val updateIntent =
                        Intent(context, TorForegroundService::class.java).apply {
                            action = TorForegroundService.ACTION_UPDATE_NOTIFICATION
                        }
                    context.startService(updateIntent)

                    return@withContext TorVerificationResult(
                        isUsingTor = true,
                        exitIp = ip,
                        exitCountry = country,
                    )
                }

                return@withContext TorVerificationResult(isUsingTor = isTor, exitIp = ip.ifEmpty { null })
            } else {
                return@withContext TorVerificationResult(
                    isUsingTor = false,
                    error = "Verification request failed: ${response.code}",
                )
            }
        }

    /**
     * Cleanup resources when the manager is no longer needed.
     * Call this in Application.onTerminate() or when shutting down.
     */
    fun cleanup() {
        stop()
        try {
            context.unregisterReceiver(torStatusReceiver)
        } catch (e: IllegalArgumentException) {
            AppLogger.w("TorServiceManager: Receiver not registered", e)
        }
    }

    companion object {
        /** Tor Project's official API to check if traffic is routing through Tor */
        private const val TOR_CHECK_API_URL = "https://check.torproject.org/api/ip"

        /** Maximum number of verification retry attempts */
        private const val MAX_VERIFICATION_RETRIES = 3

        /** Initial delay between retries (milliseconds) */
        private const val INITIAL_RETRY_DELAY_MS = 1000L

        /** Maximum delay between retries (milliseconds) */
        private const val MAX_RETRY_DELAY_MS = 30000L
    }
}
