package net.opendasharchive.openarchive.services.tor

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.opendasharchive.openarchive.core.infrastructure.client.enqueueResult
import net.opendasharchive.openarchive.services.SaveClient
import okhttp3.Request

interface ITorRepository {
    val torStatus: StateFlow<TorStatus>
    fun updateTorStatus(status: TorStatus)
    fun updatePorts(http: Int, socks: Int)
    val httpTunnelPort: Int
    val socksPort: Int
}

class TorRepository() : ITorRepository {
    private val _torStatus = MutableStateFlow<TorStatus>(TorStatus.DISCONNECTED)
    override val torStatus: StateFlow<TorStatus> = _torStatus.asStateFlow()
    private var _httpTunnelPort = 8118
    private var _socksPort = 9050

    override val httpTunnelPort = _httpTunnelPort
    override val socksPort = _socksPort

    override fun updateTorStatus(status: TorStatus) {
        _torStatus.value = status
    }

    override fun updatePorts(http: Int, socks: Int) {
        _httpTunnelPort = http
        _socksPort = socks
    }
}