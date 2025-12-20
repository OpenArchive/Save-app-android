package net.opendasharchive.openarchive.features.media.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.features.main.HomeActivity

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
    val activity = context as? HomeActivity

    // Permission states
    var showPermissionScreen by remember { mutableStateOf(false) }
    var isCameraPermissionPermanentlyDenied by remember { mutableStateOf(false) }
    var isAudioPermissionPermanentlyDenied by remember { mutableStateOf(false) }
    var hasCameraPermissionBeenRequested by remember { mutableStateOf(false) }
    var hasAudioPermissionBeenRequested by remember { mutableStateOf(false) }

    // Helper functions
    val checkCameraPermission = {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    val checkAudioPermission = {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    val shouldShowCameraRationale = {
        activity?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(
                it,
                Manifest.permission.CAMERA
            )
        } ?: false
    }

    val shouldShowAudioRationale = {
        activity?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(
                it,
                Manifest.permission.RECORD_AUDIO
            )
        } ?: false
    }

    // Audio permission launcher
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        AppLogger.d("Audio permission result: $granted")
        hasAudioPermissionBeenRequested = true

        if (!granted) {
            // Check if audio permission was permanently denied (only after we've requested it)
            isAudioPermissionPermanentlyDenied = !shouldShowAudioRationale()
        } else {
            isAudioPermissionPermanentlyDenied = false
        }
        // Audio permission result doesn't affect UI state for now
        // Video recording will work without audio if needed
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        AppLogger.d("Camera permission result: $granted")
        hasCameraPermissionBeenRequested = true

        if (granted) {
            showPermissionScreen = false
            isCameraPermissionPermanentlyDenied = false

            // If camera permission is granted, request audio permission for video if needed
            if (config.allowVideoCapture && !checkAudioPermission()) {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        } else {
            // Check if permission was permanently denied (only after we've requested it)
            isCameraPermissionPermanentlyDenied = !shouldShowCameraRationale()
            showPermissionScreen = true
        }
    }

    // Request camera permission function
    val requestCameraPermission = {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // Request audio permission if needed
    val requestAudioPermissionIfNeeded = {
        if (config.allowVideoCapture) {
            val audioGranted = checkAudioPermission()
            if (!audioGranted) {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
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
        val wasCameraPermissionGranted = checkCameraPermission()

        if (wasCameraPermissionGranted && showPermissionScreen) {
            // Camera permission was granted while we were showing permission screen
            showPermissionScreen = false
            isCameraPermissionPermanentlyDenied = false

            // If camera permission is now granted, request audio permission for video if needed
            requestAudioPermissionIfNeeded()
        } else if (!wasCameraPermissionGranted && !showPermissionScreen) {
            // Camera permission was revoked while we were showing camera
            // Only consider it permanently denied if we've already requested it before
            isCameraPermissionPermanentlyDenied = hasCameraPermissionBeenRequested &&
                !shouldShowCameraRationale()
            showPermissionScreen = true
        }

        // Update audio permission state if video capture is enabled
        if (config.allowVideoCapture) {
            val isAudioGranted = checkAudioPermission()

            if (!isAudioGranted && hasAudioPermissionBeenRequested) {
                // Only consider it permanently denied if we've already requested it before
                isAudioPermissionPermanentlyDenied = !shouldShowAudioRationale()
            } else if (isAudioGranted) {
                isAudioPermissionPermanentlyDenied = false
            }
        }
    }

    // Initial permission check
    LaunchedEffect(Unit) {
        if (checkCameraPermission()) {
            showPermissionScreen = false
            // If camera permission is granted, request audio permission for video if needed
            requestAudioPermissionIfNeeded()
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
