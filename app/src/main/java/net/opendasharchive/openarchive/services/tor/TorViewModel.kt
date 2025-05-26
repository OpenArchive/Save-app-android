package net.opendasharchive.openarchive.services.tor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.flow.StateFlow

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
        val workRequest = OneTimeWorkRequestBuilder<TorService>()
            .setConstraints(Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build())
            .build()
        WorkManager.getInstance(application).enqueueUniqueWork(
            "tor_worker",
            ExistingWorkPolicy.KEEP,
            workRequest)
    }

    private fun stopTor() {
       WorkManager.getInstance(application).cancelUniqueWork("tor_worker")
    }
}