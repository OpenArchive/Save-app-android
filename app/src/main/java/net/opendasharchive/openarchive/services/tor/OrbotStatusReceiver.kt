package net.opendasharchive.openarchive.services.tor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import info.guardianproject.netcipher.proxy.OrbotHelper
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import timber.log.Timber

class OrbotStatusReceiver : BroadcastReceiver(), KoinComponent {
    private val torRepository: ITorRepository by inject(named("tor"))

    override fun onReceive( context: Context, intent: Intent) {
        if (OrbotHelper.ACTION_STATUS == intent.action) {
            val status = intent.getStringExtra(OrbotHelper.EXTRA_STATUS);
            when (status) {
                OrbotHelper.STATUS_ON -> torRepository.updateTorStatus(TorStatus.CONNECTED)
                OrbotHelper.STATUS_OFF -> torRepository.updateTorStatus(TorStatus.DISCONNECTED)
                OrbotHelper.STATUS_STARTING -> torRepository.updateTorStatus(TorStatus.CONNECTING)
                OrbotHelper.STATUS_STOPPING -> torRepository.updateTorStatus(TorStatus.DISCONNECTING)
                else -> Timber.e("Unknown Tor status: $status")
            }
        }
    }
}
