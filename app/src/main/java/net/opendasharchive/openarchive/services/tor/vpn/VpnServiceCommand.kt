package net.opendasharchive.openarchive.services.tor.vpn

import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * Helper object for controlling the Tor VPN service.
 */
object VpnServiceCommand {

    fun prepareVpn(context: Context?): Intent? {
        return try {
            VpnService.prepare(context?.applicationContext)
        } catch (e: NullPointerException) {
            VpnStatusObservable.update(ConnectionState.CONNECTION_ERROR)
            null
        } catch (e: IllegalStateException) {
            VpnStatusObservable.update(ConnectionState.CONNECTION_ERROR)
            null
        }
    }

    fun startVpn(context: Context?) {
        launchTorVpnServiceWithAction(context, SaveTorVpnService.ACTION_START_VPN)
    }

    fun stopVpn(context: Context?) {
        launchTorVpnServiceWithAction(context, SaveTorVpnService.ACTION_STOP_VPN)
    }

    private fun launchTorVpnServiceWithAction(context: Context?, action: String) {
        if (context == null) return

        val data = Data.Builder()
            .putString(VpnServiceLauncher.COMMAND, action)
            .build()

        val work = OneTimeWorkRequestBuilder<VpnServiceLauncher>()
            .setInputData(data)
            .build()

        WorkManager.getInstance(context.applicationContext).enqueue(work)
    }
}
