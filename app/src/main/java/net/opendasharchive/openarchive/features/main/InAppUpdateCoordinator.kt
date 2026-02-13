package net.opendasharchive.openarchive.features.main

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.BuildConfig
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.logger.AppLogger

private const val IMMEDIATE_UPDATE_VERSION_GAP = 3
private const val FLEXIBLE_UPDATE_MAX_GAP = 2

@Composable
fun CheckForInAppUpdates(
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    val appUpdateManager = remember { AppUpdateManagerFactory.create(context) }

    val updateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            AppLogger.w("In-app update flow failed or cancelled: ${result.resultCode}")
        }
    }

    val result = stringResource(R.string.in_app_update_ready)
    val actionLabel = stringResource(R.string.in_app_update_restart)

    // Helper to show snackbar
    fun showFlexibleUpdateDownloadedSnackbar() {
        scope.launch {
            // Check if snackbar is already being shown or we just want to ensure it is visible?
            // SnackbarHostState puts new requests in queue. 
            // We'll just show it. If user dismisses, it's gone until next resume/trigger.
            val result = snackbarHostState.showSnackbar(
                message = result,
                actionLabel = actionLabel,
                duration = SnackbarDuration.Indefinite
            )
            if (result == SnackbarResult.ActionPerformed) {
                appUpdateManager.completeUpdate()
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val listener = InstallStateUpdatedListener { state ->
            if (state.installStatus() == InstallStatus.DOWNLOADED) {
                showFlexibleUpdateDownloadedSnackbar()
            }
        }
        
        // Register listener for flexible updates progress/completion
        appUpdateManager.registerListener(listener)

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                 checkForAppUpdates(
                    appUpdateManager, 
                    updateLauncher, 
                    ::showFlexibleUpdateDownloadedSnackbar
                )
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            appUpdateManager.unregisterListener(listener)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

private fun checkForAppUpdates(
    appUpdateManager: AppUpdateManager,
    updateLauncher: androidx.activity.result.ActivityResultLauncher<IntentSenderRequest>,
    onDownloaded: () -> Unit
) {
    appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
        if (info.installStatus() == InstallStatus.DOWNLOADED) {
            onDownloaded()
            return@addOnSuccessListener
        }

        if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
            if (info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                startUpdateFlow(appUpdateManager, info, AppUpdateType.IMMEDIATE, updateLauncher)
            }
        } else if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
             handleUpdateAvailability(appUpdateManager, info, updateLauncher)
        }
    }.addOnFailureListener { e ->
        AppLogger.w("Failed to load in-app update info", e)
    }
}

private fun handleUpdateAvailability(
    appUpdateManager: AppUpdateManager,
    info: AppUpdateInfo,
    updateLauncher: androidx.activity.result.ActivityResultLauncher<IntentSenderRequest>
) {
    val versionGap = info.availableVersionCode() - BuildConfig.VERSION_CODE
    if (versionGap <= 0) {
        AppLogger.d("No newer version detected. Gap: $versionGap")
        return
    }

    val immediateAllowed = versionGap >= IMMEDIATE_UPDATE_VERSION_GAP &&
            info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
    val flexibleAllowed = versionGap <= FLEXIBLE_UPDATE_MAX_GAP &&
            info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)

    when {
        immediateAllowed -> {
            AppLogger.i("Triggering immediate update flow. Version gap: $versionGap")
            startUpdateFlow(appUpdateManager, info, AppUpdateType.IMMEDIATE, updateLauncher)
        }
        flexibleAllowed -> {
            AppLogger.i("Triggering flexible update flow. Version gap: $versionGap")
            startUpdateFlow(appUpdateManager, info, AppUpdateType.FLEXIBLE, updateLauncher)
        }
        info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> {
            AppLogger.i("Falling back to flexible update flow. Version gap: $versionGap")
            startUpdateFlow(appUpdateManager, info, AppUpdateType.FLEXIBLE, updateLauncher)
        }
        else -> AppLogger.w("Update available but no compatible update types allowed.")
    }
}

private fun startUpdateFlow(
    appUpdateManager: AppUpdateManager,
    info: AppUpdateInfo,
    type: Int,
    launcher: androidx.activity.result.ActivityResultLauncher<IntentSenderRequest>
) {
     val options = AppUpdateOptions.newBuilder(type).build()
     try {
         appUpdateManager.startUpdateFlowForResult(info, launcher, options)
     } catch (e: Exception) {
         AppLogger.e("Failed to launch in-app update flow", e)
     }
}
