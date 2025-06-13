package net.opendasharchive.openarchive.services.tor

import android.content.Context
import android.content.Intent
import info.guardianproject.netcipher.proxy.OrbotHelper
import info.guardianproject.netcipher.proxy.StatusCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface ITorRepository : StatusCallback {
    val torStatus: StateFlow<TorStatus>
}

class TorRepository(context: Context) : ITorRepository {
    private val _torStatus = MutableStateFlow(TorStatus.DISCONNECTED)
    override val torStatus: StateFlow<TorStatus> = _torStatus.asStateFlow()

    init {
        OrbotHelper.get(context).addStatusCallback(this)
    }
    override fun onEnabled(p0: Intent?) {
        _torStatus.value = TorStatus.CONNECTED
    }

    override fun onStarting() {
        _torStatus.value = TorStatus.CONNECTING
    }

    override fun onStopping() {
        _torStatus.value = TorStatus.DISCONNECTING
    }

    override fun onDisabled() {
        _torStatus.value = TorStatus.DISCONNECTED
    }

    override fun onStatusTimeout() {
        _torStatus.value = TorStatus.TIMEOUT
    }

    override fun onNotYetInstalled() {
        _torStatus.value = TorStatus.UNAVAILABLE
    }

}