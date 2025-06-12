package net.opendasharchive.openarchive.services.tor

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import info.guardianproject.netcipher.proxy.OrbotHelper
import info.guardianproject.netcipher.proxy.OrbotHelper.InstallCallback
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber


class TorViewModel(
    private val application: Application,
    torRepository: ITorRepository,
) : AndroidViewModel(application), InstallCallback {

    init {
        OrbotHelper.get(application).addStatusCallback(torRepository)
    }
    val torStatus: StateFlow<TorStatus> = torRepository.torStatus

    fun toggleTorServiceState(activity: Activity, enabled: Boolean) {
        if (enabled) {
            startTor(activity)
        }
    }

    fun requestOpenOrInstallOrbot(activity: Activity) {
        if (OrbotHelper.isOrbotInstalled(application)) {
            requestOpenOrbot(activity)
        } else {
            OrbotHelper.get(application).apply {
                addInstallCallback(this@TorViewModel)
                installOrbot(activity)
            }
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

    private fun requestOpenOrbot(activity: Activity): Boolean {
        if (!OrbotHelper.isOrbotInstalled(application)) {
            return false
        }
        val intent =
            activity.packageManager.getLaunchIntentForPackage(OrbotHelper.ORBOT_PACKAGE_NAME)
        if (intent == null) {
            Timber.e("Orbot is not installed.")
            return false
        }

        activity.startActivity(intent)
        return true
    }

    override fun onInstalled() {
        OrbotHelper.get(application).removeInstallCallback(this)
        OrbotHelper.requestStartTor(application)
    }

    override fun onInstallTimeout() {
        Timber.e("timeout on orbot install")
    }
}