package net.opendasharchive.openarchive.features.media.camera

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.util.rememberComposePermissionManager

/**
 * Wrapper composable that replicates all the logic from CameraActivity.
 *
 * Handles:
 * - Camera and audio permission checking and requesting
 * - Permission launcher registration
 * - Showing CameraPermissionScreen when permissions are denied
 * - Showing CameraScreen when permissions are granted
 * - Permission state management (permanently denied, etc.)
 * - Checking permissions on resume (when returning from settings)
 *
 * @param config Camera configuration
 * @param onCaptureComplete Called when user confirms captured media
 * @param onCancel Called when user cancels
 */
@Composable
fun CameraScreenWrapper(
    config: CameraConfig = CameraConfig(),
    onCaptureComplete: (List<Uri>) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val permissionManager = rememberComposePermissionManager()

    // Permission states
    var showPermissionScreen by remember { mutableStateOf(false) }
    var isCameraPermissionPermanentlyDenied by remember { mutableStateOf(false) }
    var isAudioPermissionPermanentlyDenied by remember { mutableStateOf(false) }
    var hasCameraPermissionBeenRequested by remember { mutableStateOf(false) }
    var hasAudioPermissionBeenRequested by remember { mutableStateOf(false) }

    // Helper functions are provided by ComposePermissionManager

    // Request camera permission function
    val requestCameraPermission = {
        hasCameraPermissionBeenRequested = true
        permissionManager.requestCameraPermission()
    }

    // Open app settings
    val openSettings = {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }

    // Check and update permission states (for when returning from settings)
    val checkAndUpdatePermissionStates = {
        val wasCameraPermissionGranted = permissionManager.isCameraGranted()

        if (wasCameraPermissionGranted && showPermissionScreen) {
            // Camera permission was granted while we were showing permission screen
            showPermissionScreen = false
            isCameraPermissionPermanentlyDenied = false

            // If camera permission is now granted, request audio permission for video if needed
            if (config.allowVideoCapture) {
                hasAudioPermissionBeenRequested = true
                permissionManager.requestAudioPermissionIfNeeded(config.allowVideoCapture)
            }
        } else if (!wasCameraPermissionGranted && !showPermissionScreen) {
            // Camera permission was revoked while we were showing camera
            // Only consider it permanently denied if we've already requested it before
            isCameraPermissionPermanentlyDenied = hasCameraPermissionBeenRequested &&
                !permissionManager.shouldShowCameraRationale()
            showPermissionScreen = true
        }

        // Update audio permission state if video capture is enabled
        if (config.allowVideoCapture) {
            val isAudioGranted = permissionManager.isAudioGranted()

            if (!isAudioGranted && hasAudioPermissionBeenRequested) {
                // Only consider it permanently denied if we've already requested it before
                isAudioPermissionPermanentlyDenied = !permissionManager.shouldShowAudioRationale()
            } else if (isAudioGranted) {
                isAudioPermissionPermanentlyDenied = false
            }
        }
    }

    LaunchedEffect(permissionManager.cameraStatus()) {
        if (permissionManager.isCameraGranted()) {
            AppLogger.d("Camera permission result: true")
            hasCameraPermissionBeenRequested = true
            showPermissionScreen = false
            isCameraPermissionPermanentlyDenied = false

            if (config.allowVideoCapture && !permissionManager.isAudioGranted()) {
                hasAudioPermissionBeenRequested = true
                permissionManager.requestAudioPermission()
            }
        } else if (hasCameraPermissionBeenRequested) {
            AppLogger.d("Camera permission result: false")
            isCameraPermissionPermanentlyDenied = !permissionManager.shouldShowCameraRationale()
            showPermissionScreen = true
        }
    }

    LaunchedEffect(permissionManager.audioStatus()) {
        if (permissionManager.isAudioGranted()) {
            AppLogger.d("Audio permission result: true")
            hasAudioPermissionBeenRequested = true
            isAudioPermissionPermanentlyDenied = false
        } else if (hasAudioPermissionBeenRequested) {
            AppLogger.d("Audio permission result: false")
            isAudioPermissionPermanentlyDenied = !permissionManager.shouldShowAudioRationale()
        }
    }

    // Initial permission check
    LaunchedEffect(Unit) {
        if (permissionManager.isCameraGranted()) {
            showPermissionScreen = false
            // If camera permission is granted, request audio permission for video if needed
            if (config.allowVideoCapture) {
                hasAudioPermissionBeenRequested = true
                permissionManager.requestAudioPermissionIfNeeded(config.allowVideoCapture)
            }
        } else {
            // For first launch, we don't know if it's permanently denied yet
            // Just show permission screen and let user try to grant
            isCameraPermissionPermanentlyDenied = false
            isAudioPermissionPermanentlyDenied = false
            showPermissionScreen = true
        }
    }

    // Re-check permissions when composition becomes active (simulates onResume)
    // This handles the case when user returns from settings
    DisposableEffect(Unit) {
        onDispose { }
    }

    // Use a lifecycle observer to handle resume events
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                checkAndUpdatePermissionStates()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Show appropriate screen based on permission state
    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        if (showPermissionScreen) {
            CameraPermissionScreen(
                isCameraPermissionPermanentlyDenied = isCameraPermissionPermanentlyDenied,
                isAudioPermissionPermanentlyDenied = isAudioPermissionPermanentlyDenied,
                needsAudioPermission = config.allowVideoCapture,
                onRequestPermissions = { requestCameraPermission() },
                onOpenSettings = { openSettings() },
                onCancel = onCancel
            )
        } else {
            CameraScreen(
                config = config,
                onCaptureComplete = onCaptureComplete,
                onCancel = onCancel
            )
        }
    }
}
