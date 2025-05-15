package net.opendasharchive.openarchive.services.tor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import org.torproject.jni.TorService
import timber.log.Timber

class TorStatusReceiver : BroadcastReceiver(), KoinComponent {
    private val torRepository: ITorRepository by inject(named("tor"))

    override fun onReceive( context: Context, intent: Intent) {
        when(intent.action) {
            TorService.ACTION_STATUS -> {
                val status = intent.getStringExtra(TorService.EXTRA_STATUS);
                when (status) {
                    TorService.STATUS_ON -> torRepository.updateTorStatus(TorStatus.CONNECTED)
                    TorService.STATUS_OFF -> torRepository.updateTorStatus(TorStatus.DISCONNECTED)
                    TorService.STATUS_STARTING -> torRepository.updateTorStatus(TorStatus.CONNECTING)
                    TorService.STATUS_STOPPING -> torRepository.updateTorStatus(TorStatus.DISCONNECTING)
                    else -> Timber.e("Unknown Tor status: $status")
                }
            }
            TorService.ACTION_ERROR -> {
                val errorText = intent.getStringExtra(Intent.EXTRA_TEXT)
                Timber.e("Tor error: $errorText")
                torRepository.updateTorStatus(TorStatus.ERROR)
            }
            else -> Timber.d("Got rogue action: ${intent.action}")
        }
    }
}
