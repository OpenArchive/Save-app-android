package net.opendasharchive.openarchive.services.tor

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.StateFlow
import org.torproject.jni.TorService


class TorViewModel(
    private val application: Application,
    torRepository: ITorRepository,
) : AndroidViewModel(application) {

    val torStatus: StateFlow<TorStatus> = torRepository.torStatus

    fun setTorServiceState(enabled: Boolean) {
        if (enabled) {
            startTor()
        } else {
            stopTor()
        }
    }

    private fun startTor() {
        val intent = Intent(application, TorService::class.java)
        application.startForegroundService(intent)
        requestTorStatus()
    }

    private fun stopTor() {
        val intent = Intent(application, TorService::class.java)
        application.stopService(intent)
        requestTorStatus()
    }

    fun requestTorStatus() {
        val intent = Intent(TorService.ACTION_STATUS).setPackage(application.packageName)
        application.sendBroadcast(intent)
    }
}