package net.opendasharchive.openarchive.services.tor

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.analytics.api.AnalyticsEvent
import net.opendasharchive.openarchive.analytics.api.AnalyticsManager
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.services.tor.vpn.ConnectionState
import net.opendasharchive.openarchive.services.tor.vpn.VpnServiceCommand
import net.opendasharchive.openarchive.services.tor.vpn.VpnStatusObservable
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.math.pow

/**
 * Singleton manager for the Tor VPN service (OnionMasq).
 *
 * This implementation uses OnionMasq VPN to route app traffic through Tor.
 * Unlike the previous SOCKS proxy approach, this routes all app traffic
 * through Tor automatically via Android's VPN interface.
 *
 * Provides:
 * - StateFlow for observing Tor status changes
 * - Service lifecycle management (start/stop)
 * - Traffic verification via check.torproject.org
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
 * // Control service
 * torManager.start()
 * torManager.stop()
 * ```
 */
class TorServiceManager(
    private val context: Context,
    private val analyticsManager: AnalyticsManager,
) {
    private val _torStatus = MutableStateFlow<TorStatus>(TorStatus.Idle)
    val torStatus: StateFlow<TorStatus> = _torStatus.asStateFlow()

    // For backwards compatibility - VPN mode doesn't use SOCKS ports
    private val _socksPort = MutableStateFlow(0)
    val socksPort: StateFlow<Int> = _socksPort.asStateFlow()

    private val _httpTunnelPort = MutableStateFlow(0)
    val httpTunnelPort: StateFlow<Int> = _httpTunnelPort.asStateFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private var statusObserverJob: Job? = null

    /**
     * Circuit isolation purposes - maintained for API compatibility.
     * In VPN mode, circuit isolation is handled by OnionMasq internally.
     */
    enum class CircuitPurpose {
        VERIFICATION,
        GEOLOCATION,
        UPLOAD,
        GENERAL
    }

    init {
        // Start observing VPN status changes
        startStatusObserver()
    }

    private fun startStatusObserver() {
        statusObserverJob?.cancel()
        statusObserverJob = coroutineScope.launch {
            VpnStatusObservable.statusLiveData.collect { connectionState ->
                val newStatus = when (connectionState) {
                    ConnectionState.INIT -> TorStatus.Idle
                    ConnectionState.CONNECTING -> TorStatus.Starting
                    ConnectionState.CONNECTED -> TorStatus.On
                    ConnectionState.DISCONNECTING -> TorStatus.Off
                    ConnectionState.DISCONNECTED -> TorStatus.Off
                    ConnectionState.CONNECTION_ERROR -> TorStatus.Off
                }
                _torStatus.value = newStatus
                AppLogger.d("TorServiceManager: Status changed to $newStatus (VPN: $connectionState)")
            }
        }
    }

    /**
     * Starts the Tor VPN service.
     * Requires VPN permission to be granted first.
     * Call VpnServiceCommand.prepareVpn() to check/request permission.
     */
    fun start() {
        AppLogger.d("TorServiceManager: Starting Tor VPN service")
        _torStatus.value = TorStatus.Starting
        VpnServiceCommand.startVpn(context)
    }

    /**
     * Stops the Tor VPN service.
     */
    fun stop() {
        AppLogger.d("TorServiceManager: Stopping Tor VPN service")
        VpnServiceCommand.stopVpn(context)
        _torStatus.value = TorStatus.Off
    }

    /**
     * Returns true if Tor VPN is currently connected and ready for use.
     */
    fun isReady(): Boolean {
        val status = _torStatus.value
        return status == TorStatus.On || status is TorStatus.Verified
    }

    /**
     * Creates an OkHttpClient for network requests.
     * In VPN mode, no proxy configuration is needed - traffic routes through VPN automatically.
     *
     * @param purpose The purpose of this client (for logging/analytics)
     * @return Configured OkHttpClient, or null if Tor is not ready
     */
    fun createTorClient(purpose: CircuitPurpose = CircuitPurpose.GENERAL): OkHttpClient? {
        if (!isReady()) return null

        // In VPN mode, no SOCKS proxy needed - traffic routes through VPN
        return OkHttpClient
            .Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Fetches the country name for a given IP address using free geolocation APIs.
     * Routes the request through Tor automatically (via VPN).
     *
     * @param ip The IP address to look up
     * @param client OkHttpClient (traffic routed via VPN)
     * @return Country name or null if lookup fails
     */
    private suspend fun getExitCountry(
        ip: String,
        client: OkHttpClient,
    ): String? =
        withContext(Dispatchers.IO) {
            // Try primary API (ipwho.is - free, no rate limits, returns JSON)
            try {
                val request = Request.Builder().url("https://ipwho.is/$ip").build()
                val response = client.newCall(request).execute()
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
            } catch (e: Exception) {
                AppLogger.w("TorServiceManager: ipwho.is country lookup failed", e)
            }

            // Try secondary API (freeipapi.com)
            try {
                val request = Request.Builder().url("https://freeipapi.com/api/json/$ip").build()
                val response = client.newCall(request).execute()
                val body = response.body?.string()

                if (response.isSuccessful && body != null) {
                    val json = JSONObject(body)
                    val country = json.optString("countryName", "")
                    if (country.isNotEmpty()) {
                        return@withContext country
                    }
                }
            } catch (e: Exception) {
                AppLogger.w("TorServiceManager: freeipapi.com country lookup failed", e)
            }

            null
        }

    /**
     * Verifies that traffic is actually routing through the Tor network.
     *
     * This makes a request to check.torproject.org through the VPN
     * to confirm the connection is working properly.
     *
     * @return TorVerificationResult containing verification status and exit IP
     */
    suspend fun verifyTorConnection(): TorVerificationResult =
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()

            if (!isReady()) {
                trackVerificationMetrics(
                    success = false,
                    retryCount = 0,
                    durationMs = System.currentTimeMillis() - startTime,
                    errorType = "vpn_not_connected"
                )
                return@withContext TorVerificationResult(
                    isUsingTor = false,
                    error = "Tor VPN not connected",
                )
            }

            var lastError: String? = null
            var attempt = 0

            while (attempt < MAX_VERIFICATION_RETRIES) {
                try {
                    val result = attemptVerification()
                    if (result.isUsingTor || result.error == null) {
                        trackVerificationMetrics(
                            success = result.isUsingTor,
                            retryCount = attempt,
                            durationMs = System.currentTimeMillis() - startTime,
                            errorType = if (result.isUsingTor) null else "not_using_tor"
                        )
                        return@withContext result
                    }
                    lastError = result.error
                } catch (e: Exception) {
                    lastError = e.message ?: "Unknown error"
                    AppLogger.w("TorServiceManager: Verification attempt ${attempt + 1} failed", e)
                }

                attempt++
                if (attempt < MAX_VERIFICATION_RETRIES) {
                    val delayMs = min(
                        INITIAL_RETRY_DELAY_MS * 2.0.pow(attempt - 1).toLong(),
                        MAX_RETRY_DELAY_MS,
                    )
                    delay(delayMs)
                }
            }

            val errorType = categorizeError(lastError)
            trackVerificationMetrics(
                success = false,
                retryCount = attempt,
                durationMs = System.currentTimeMillis() - startTime,
                errorType = errorType
            )

            return@withContext TorVerificationResult(
                isUsingTor = false,
                error = lastError ?: "Verification failed after $MAX_VERIFICATION_RETRIES attempts",
            )
        }

    private fun categorizeError(error: String?): String {
        return when {
            error == null -> "unknown"
            error.contains("timeout", ignoreCase = true) -> "timeout"
            error.contains("connect", ignoreCase = true) -> "connection_failed"
            error.contains("certificate", ignoreCase = true) -> "certificate_error"
            error.contains("SSL", ignoreCase = true) -> "ssl_error"
            else -> "other"
        }
    }

    private suspend fun trackVerificationMetrics(
        success: Boolean,
        retryCount: Int,
        durationMs: Long,
        errorType: String?
    ) {
        try {
            analyticsManager.trackEvent(
                AnalyticsEvent.TorVerificationAttempt(
                    success = success,
                    retryCount = retryCount,
                    durationMs = durationMs,
                    errorType = errorType
                )
            )
        } catch (e: Exception) {
            AppLogger.w("TorServiceManager: Failed to track verification metrics", e)
        }
    }

    private suspend fun attemptVerification(): TorVerificationResult =
        withContext(Dispatchers.IO) {
            // In VPN mode, traffic routes through Tor automatically
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder().url(TOR_CHECK_API_URL).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string()

            if (response.isSuccessful && body != null) {
                val json = JSONObject(body)
                val isTor = json.optBoolean("IsTor", false)
                val ip = json.optString("IP", "")

                AppLogger.d("TorServiceManager: Verification result - IsTor: $isTor, IP: $ip")

                if (isTor && ip.isNotEmpty()) {
                    val country = getExitCountry(ip, client)
                    val connectionInfo = TorConnectionInfo(
                        exitIp = ip,
                        exitCountry = country,
                    )
                    _torStatus.value = TorStatus.Verified(connectionInfo)

                    return@withContext TorVerificationResult(
                        isUsingTor = true,
                        exitIp = ip,
                        exitCountry = country,
                    )
                }

                return@withContext TorVerificationResult(
                    isUsingTor = isTor,
                    exitIp = ip.ifEmpty { null }
                )
            } else {
                return@withContext TorVerificationResult(
                    isUsingTor = false,
                    error = "Verification request failed: ${response.code}",
                )
            }
        }

    /**
     * Cleanup resources when the manager is no longer needed.
     */
    fun cleanup() {
        stop()
        statusObserverJob?.cancel()
    }

    companion object {
        private const val TOR_CHECK_API_URL = "https://check.torproject.org/api/ip"
        private const val MAX_VERIFICATION_RETRIES = 3
        private const val INITIAL_RETRY_DELAY_MS = 1000L
        private const val MAX_RETRY_DELAY_MS = 30000L
    }
}
