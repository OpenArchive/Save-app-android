package net.opendasharchive.openarchive.features.main

import android.app.Activity
import android.content.IntentSender
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import net.opendasharchive.openarchive.BuildConfig
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.logger.AppLogger

/** Handles Google Play in-app update flows and keeps MainActivity lean. */
internal class InAppUpdateCoordinator(
    private val activity: Activity,
    private val rootView: View,
    private val updateLauncher: ActivityResultLauncher<IntentSenderRequest>
) {

    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(activity)
    private var installStateListener: InstallStateUpdatedListener? = null
    private var flexibleUpdateSnackbar: Snackbar? = null

    fun onResume() {
        checkForAppUpdates()
    }

    fun onDestroy() {
        installStateListener?.let(appUpdateManager::unregisterListener)
        installStateListener = null
        flexibleUpdateSnackbar?.dismiss()
        flexibleUpdateSnackbar = null
    }

    private fun checkForAppUpdates() {
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { info ->
                when (info.installStatus()) {
                    InstallStatus.DOWNLOADED -> {
                        showFlexibleUpdateDownloadedSnackbar()
                        return@addOnSuccessListener
                    }

                    InstallStatus.INSTALLED -> dismissFlexibleUpdateSnackbar()
                    else -> Unit
                }

                when (info.updateAvailability()) {
                    UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                        if (info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                            startUpdateFlow(info, AppUpdateType.IMMEDIATE)
                        }
                    }

                    UpdateAvailability.UPDATE_AVAILABLE -> handleUpdateAvailability(info)
                    else -> Unit
                }
            }
            .addOnFailureListener { throwable ->
                AppLogger.w("Failed to load in-app update info", throwable)
            }
    }

    private fun handleUpdateAvailability(appUpdateInfo: AppUpdateInfo) {
        val availableVersionCode = appUpdateInfo.availableVersionCode()

        val versionGap = availableVersionCode - BuildConfig.VERSION_CODE
        if (versionGap <= 0) {
            AppLogger.d("No newer version detected for in-app update flow. Current gap: $versionGap")
            return
        }

        val immediateAllowed = versionGap >= IMMEDIATE_UPDATE_VERSION_GAP &&
            appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
        val flexibleAllowed = versionGap <= FLEXIBLE_UPDATE_MAX_GAP &&
            appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)

        when {
            immediateAllowed -> {
                AppLogger.i("Triggering immediate update flow. Version gap: $versionGap")
                startUpdateFlow(appUpdateInfo, AppUpdateType.IMMEDIATE)
            }

            flexibleAllowed -> {
                AppLogger.i("Triggering flexible update flow. Version gap: $versionGap")
                registerFlexibleUpdateListener()
                startUpdateFlow(appUpdateInfo, AppUpdateType.FLEXIBLE)
            }

            appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> {
                AppLogger.i(
                    "Falling back to flexible update flow despite larger gap. Version gap: $versionGap"
                )
                registerFlexibleUpdateListener()
                startUpdateFlow(appUpdateInfo, AppUpdateType.FLEXIBLE)
            }

            else -> AppLogger.w("Update available but no compatible update types allowed on this device")
        }
    }

    private fun registerFlexibleUpdateListener() {
        if (installStateListener != null) return

        installStateListener = InstallStateUpdatedListener { state ->
            when (state.installStatus()) {
                InstallStatus.DOWNLOADED -> showFlexibleUpdateDownloadedSnackbar()
                InstallStatus.INSTALLED -> dismissFlexibleUpdateSnackbar()
                else -> Unit
            }
        }

        installStateListener?.let(appUpdateManager::registerListener)
    }

    private fun showFlexibleUpdateDownloadedSnackbar() {
        if (flexibleUpdateSnackbar?.isShown == true) return

        flexibleUpdateSnackbar = Snackbar.make(
            rootView,
            R.string.in_app_update_ready,
            Snackbar.LENGTH_INDEFINITE
        ).setAction(R.string.in_app_update_restart) {
            dismissFlexibleUpdateSnackbar()
            appUpdateManager.completeUpdate()
        }

        flexibleUpdateSnackbar?.show()
    }

    private fun dismissFlexibleUpdateSnackbar() {
        flexibleUpdateSnackbar?.dismiss()
        flexibleUpdateSnackbar = null
    }

    private fun startUpdateFlow(appUpdateInfo: AppUpdateInfo, updateType: Int) {
        val options = AppUpdateOptions.newBuilder(updateType).build()

        try {
            appUpdateManager.startUpdateFlowForResult(appUpdateInfo, updateLauncher, options)
        } catch (exception: IntentSender.SendIntentException) {
            AppLogger.e("Failed to launch in-app update flow", exception)
        }
    }

    private companion object {
        const val IMMEDIATE_UPDATE_VERSION_GAP = 3
        const val FLEXIBLE_UPDATE_MAX_GAP = 2
    }
}
