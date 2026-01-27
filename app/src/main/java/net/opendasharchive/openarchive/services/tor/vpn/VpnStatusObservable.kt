package net.opendasharchive.openarchive.services.tor.vpn

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import net.opendasharchive.openarchive.core.logger.AppLogger
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Connection states for the Tor VPN.
 */
enum class ConnectionState {
    /** Initial state - VPN not started */
    INIT,
    /** VPN is connecting to Tor network */
    CONNECTING,
    /** VPN is connected and ready for traffic */
    CONNECTED,
    /** VPN is disconnecting */
    DISCONNECTING,
    /** VPN is disconnected */
    DISCONNECTED,
    /** Connection error occurred */
    CONNECTION_ERROR
}

/**
 * Data usage tracking for the VPN connection.
 */
data class DataUsage(
    val bytesReceived: Long = 0L,
    val bytesSent: Long = 0L
)

/**
 * Observable singleton for VPN status updates.
 */
object VpnStatusObservable {

    private const val TAG = "VpnStatusObservable"

    private val _statusLiveData = MutableStateFlow(ConnectionState.INIT)
    val statusLiveData: StateFlow<ConnectionState> = _statusLiveData

    private val _dataUsage = MutableStateFlow(DataUsage())
    val dataUsage: StateFlow<DataUsage> = _dataUsage

    private val _hasInternetConnectivity = MutableStateFlow(true)
    val hasInternetConnectivity: StateFlow<Boolean> = _hasInternetConnectivity

    private val _bootstrapProgress = MutableStateFlow(0)
    val bootstrapProgress: StateFlow<Int> = _bootstrapProgress

    private var startTime = 0L

    var isAlwaysOnBooting = AtomicBoolean(false)

    fun update(status: ConnectionState) {
        AppLogger.d("$TAG: Status update: $status")
        if (status == ConnectionState.CONNECTED) {
            startTime = SystemClock.elapsedRealtime()
        } else if (status == ConnectionState.DISCONNECTED || status == ConnectionState.CONNECTION_ERROR) {
            startTime = 0L
        }
        _statusLiveData.update { status }
    }

    fun updateBootstrapProgress(progress: Int) {
        _bootstrapProgress.update { progress.coerceIn(0, 100) }
    }

    fun updateInternetConnectivity(isConnected: Boolean) {
        _hasInternetConnectivity.update { isConnected }
    }

    fun updateDataUsage(downstream: Long, upstream: Long) {
        _dataUsage.update { DataUsage(bytesReceived = downstream, bytesSent = upstream) }
    }

    fun getStartTimeBase(): Long = startTime

    fun reset() {
        _dataUsage.update { DataUsage() }
        _bootstrapProgress.update { 0 }
        _hasInternetConnectivity.update { true }
    }

    fun isVPNActive(): Boolean {
        val status = _statusLiveData.value
        return status == ConnectionState.CONNECTING || status == ConnectionState.CONNECTED
    }

    fun isConnected(): Boolean {
        return _statusLiveData.value == ConnectionState.CONNECTED
    }
}
