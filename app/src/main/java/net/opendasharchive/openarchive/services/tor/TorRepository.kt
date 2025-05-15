package net.opendasharchive.openarchive.services.tor

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface ITorRepository {
    val torStatus: StateFlow<TorStatus>
    fun updateTorStatus(status: TorStatus)
}

class TorRepository() : ITorRepository {
    private val _torStatus = MutableStateFlow<TorStatus>(TorStatus.DISCONNECTED)
    override val torStatus: StateFlow<TorStatus> = _torStatus.asStateFlow()

    override fun updateTorStatus(status: TorStatus) {
        _torStatus.value = status
    }
}