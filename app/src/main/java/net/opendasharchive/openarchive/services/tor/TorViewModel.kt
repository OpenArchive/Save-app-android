package net.opendasharchive.openarchive.services.tor

import android.app.Activity
import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import info.guardianproject.netcipher.proxy.OrbotHelper
import kotlinx.coroutines.flow.StateFlow
import net.opendasharchive.openarchive.util.Prefs
import timber.log.Timber


class TorViewModel(
    private val application: Application,
    private val torRepository: ITorRepository,
) : AndroidViewModel(application), OrbotHelper.InstallCallback {

    val torStatus: StateFlow<TorStatus> = torRepository.torStatus

    fun toggleTorServiceState(activity: Activity, enabled: Boolean) {
        if (enabled) {
            startTor(activity)
        } else {
            stopTor(activity)
        }
    }

    private fun startTor(activity: Activity) {
        if (OrbotHelper.isOrbotInstalled(application)) {
            OrbotHelper.requestStartTor(application)
        } else {
            OrbotHelper.get(application).addInstallCallback(this)
            OrbotHelper.get(application).installOrbot(activity)
        }
    }

    private fun stopTor(activity: Activity) {
        if (OrbotHelper.isOrbotInstalled(application)) {
            val intent = activity.packageManager.getLaunchIntentForPackage(OrbotHelper.ORBOT_PACKAGE_NAME)
            if (intent != null) {
                activity.startActivity(intent)
            } else {
                Timber.e("Orbot is not installed.")
            }

        }
    }

    fun requestTorStatus()  {
        OrbotHelper.get(application).init()
    }

    override fun onInstalled() {
        OrbotHelper.get(application).removeInstallCallback(this)
        OrbotHelper.requestStartTor(application)
    }

    override fun onInstallTimeout() {
        Timber.e("timeout on orbot install")
    }
}