package net.opendasharchive.openarchive.services.tor

import android.app.Activity
import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import info.guardianproject.netcipher.proxy.OrbotHelper
import info.guardianproject.netcipher.proxy.OrbotHelper.InstallCallback
import info.guardianproject.netcipher.proxy.StatusCallback
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber


class TorViewModel(
    private val application: Application,
    private val torRepository: ITorRepository,
) : AndroidViewModel(application), InstallCallback {

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
                addStatusCallback(torRepository)
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

    fun requestTorStatus() {
        val intent = Intent(OrbotHelper.ACTION_STATUS);
        intent.setPackage(OrbotHelper.ORBOT_PACKAGE_NAME);
        intent.putExtra(OrbotHelper.EXTRA_PACKAGE_NAME, application.packageName);
        application.sendBroadcast(intent)
    }

    override fun onInstalled() {
        OrbotHelper.get(application).removeInstallCallback(this)
        OrbotHelper.requestStartTor(application)
    }

    override fun onInstallTimeout() {
        Timber.e("timeout on orbot install")
    }
}