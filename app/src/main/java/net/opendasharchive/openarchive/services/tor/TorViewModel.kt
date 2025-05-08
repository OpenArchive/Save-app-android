package net.opendasharchive.openarchive.services.tor

import android.app.Application
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.core.infrastructure.client.enqueueResult
import net.opendasharchive.openarchive.services.SaveClient
import net.opendasharchive.openarchive.util.Prefs
import okhttp3.Request
import timber.log.Timber
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

class TorViewModel(
    private val application: Application,
    private val torRepository: ITorRepository
) : AndroidViewModel(application) {

    val torStatus: StateFlow<TorStatus> = torRepository.torStatus

    init {
        viewModelScope.launch {
            torStatus.collect { status ->
                if (status == TorStatus.CONNECTED) {
                   verifyTorStatus()
                    //verifyTorStatusWithAPI()
                }
            }
        }
    }

    private fun verifyTorStatus() {
        val thread = HandlerThread("VerifyThread").apply { start() }
        Handler(thread.looper).post {
            val check = canConnectToSocket(torRepository.httpTunnelPort) &&
                    isServerSocketInUse(torRepository.httpTunnelPort)
            if (check) {
                torRepository.updateTorStatus(TorStatus.VERIFIED)
            }
        }
    }

    private fun canConnectToSocket(port: Int): Boolean {
        try {
            val socket = Socket();
            socket.connect(InetSocketAddress("localhost", port), 120);
            socket.close();
            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun isServerSocketInUse( port: Int): Boolean {
        try {
            ServerSocket(port).close();
            return false
        } catch (e: Exception) {
            // Could not connect.
            return true
        }
    }

    // API is broken on backend: https://forum.torproject.org/t/is-https-check-torproject-org-api-ip-broken/11377
    private suspend fun verifyTorStatusWithAPI() {
        SaveClient.get(application).enqueueResult(
            Request.Builder()
                .url("https://check.torproject.org/api/ip")
                .method("GET", null)
                .build()
        ) { response ->
            try {
                val check = Gson().fromJson(response.body?.string(), CheckTorResponse::class.java)
                if (check.IsTor) {
                    torRepository.updateTorStatus(TorStatus.VERIFIED)
                }
                Timber.tag("TorViewModel").d("Verified Tor: ${check.IsTor}")
                Result.success(response.isSuccessful)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    fun toggleTorServiceState() {
        if (!Prefs.useTor) {
            startTor()
        } else {
            stopTor()
        }
    }

    fun updateTorServiceState() {
        if (Prefs.useTor) {
            startTor()
        } else {
            stopTor()
        }
    }

    private fun startTor() {
        Intent(getApplication(), TorForegroundService::class.java).also { intent ->
            getApplication<Application>().startForegroundService(intent)
        }
    }

    private fun stopTor() {
        Intent(getApplication(), TorForegroundService::class.java).also { intent ->
            getApplication<Application>().stopService(intent)
        }
    }
}