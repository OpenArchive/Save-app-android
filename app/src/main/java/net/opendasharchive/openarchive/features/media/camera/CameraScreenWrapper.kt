package net.opendasharchive.openarchive.features.media.camera

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
        } else if (!wasCameraPermissionGranted && !showPermissionScreen) {
            // Camera permission was revoked while we were showing camera
            // Only consider it permanently denied if we've already requested it before
            isCameraPermissionPermanentlyDenied = hasCameraPermissionBeenRequested &&
                !permissionManager.shouldShowCameraRationale()
            showPermissionScreen = true
        }

        // We no longer proactively check audio here as it's deferred to CameraScreen
    }

    LaunchedEffect(permissionManager.cameraStatus()) {
        if (permissionManager.isCameraGranted()) {
            AppLogger.d("Camera permission result: true")
            hasCameraPermissionBeenRequested = true
            showPermissionScreen = false
            isCameraPermissionPermanentlyDenied = false
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
        } else {
            // For first launch, we don't know if it's permanently denied yet
            // Just show permission screen and let user try to grant
            isCameraPermissionPermanentlyDenied = false
            isAudioPermissionPermanentlyDenied = false
            showPermissionScreen = true
        }
    }

    // Window management: Keep screen on, brightness override, and immersive mode
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val window = activity?.window

        if (window != null) {
            AppLogger.d("CameraScreenWrapper: Applying window flags and immersive mode")

            // 1. Keep screen on
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            // 2. Brightness override
            val originalBrightness = window.attributes.screenBrightness
            if (config.overrideScreenBrightness) {
                val layoutParams = window.attributes
                layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
                window.attributes = layoutParams
            }

            // 3. Immersive mode setup
            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
            val originalSystemBarsBehavior = windowInsetsController.systemBarsBehavior

            windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

            // Android 15+ handling if needed (though mostly handled by navigationBarsPadding in Compose)
            if (Build.VERSION.SDK_INT >= 35) {
                window.isNavigationBarContrastEnforced = false
            }

            onDispose {
                AppLogger.d("CameraScreenWrapper: Resetting window flags and immersive mode")

                // Reset keep screen on
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                // Reset brightness
                val layoutParams = window.attributes
                layoutParams.screenBrightness = originalBrightness
                window.attributes = layoutParams

                // Reset immersive mode
                windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
                windowInsetsController.systemBarsBehavior = originalSystemBarsBehavior
            }
        } else {
            onDispose { }
        }
    }

    // Re-check permissions when composition becomes active (simulates onResume)
    // This handles the case when user returns from settings
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
                isAudioPermissionPermanentlyDenied = false, // Defer audio check
                needsAudioPermission = false, // Don't mention audio in initial setup
                onRequestPermissions = { requestCameraPermission() },
                onOpenSettings = { openSettings() },
                onCancel = onCancel
            )
        } else {
            CameraScreen(
                config = config,
                onCaptureComplete = onCaptureComplete,
                onCancel = onCancel,
                permissionManager = permissionManager
            )
        }
    }
}
