package net.opendasharchive.openarchive.features.media.camera

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.os.Build
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.features.core.BaseComposeActivity
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme

class CameraActivity : BaseComposeActivity() {
    
    companion object {
        const val EXTRA_CAMERA_CONFIG = "camera_config"
        const val EXTRA_CAPTURED_URIS = "captured_uris"
        const val REQUEST_CODE_CAMERA = 1001
        
        fun createIntent(
            activity: Activity,
            config: CameraConfig = CameraConfig()
        ): Intent {
            return Intent(activity, CameraActivity::class.java).apply {
                putExtra(EXTRA_CAMERA_CONFIG, config)
            }
        }
    }
    
    private var cameraConfig: CameraConfig? = null
    private var showPermissionScreen by mutableStateOf(false)
    private var isCameraPermissionPermanentlyDenied by mutableStateOf(false)
    private var isAudioPermissionPermanentlyDenied by mutableStateOf(false)
    private var hasCameraPermissionBeenRequested by mutableStateOf(false)
    private var hasAudioPermissionBeenRequested by mutableStateOf(false)
    
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        AppLogger.d("Camera permission result: $granted")
        hasCameraPermissionBeenRequested = true
        
        if (granted) {
            showPermissionScreen = false
            isCameraPermissionPermanentlyDenied = false
            // If camera permission is granted, request audio permission for video if needed
            requestAudioPermissionIfNeeded()
        } else {
            // Check if permission was permanently denied (only after we've requested it)
            isCameraPermissionPermanentlyDenied = !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
            showPermissionScreen = true
        }
    }
    
    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        AppLogger.d("Audio permission result: $granted")
        hasAudioPermissionBeenRequested = true
        
        if (!granted) {
            // Check if audio permission was permanently denied (only after we've requested it)
            isAudioPermissionPermanentlyDenied = !shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)
        } else {
            isAudioPermissionPermanentlyDenied = false
        }
        // Audio permission result doesn't affect UI state for now
        // Video recording will work without audio if needed
    }
    
    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestCameraPermission() {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }
    
    private fun requestAudioPermissionIfNeeded() {
        if (cameraConfig?.allowVideoCapture == true) {
            val audioGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            
            if (!audioGranted) {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }
    
    private fun setupEdgeToEdge() {
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ (API 30+) - Enhanced for Android 15
            windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            
            // Hide system bars but keep them accessible with gestures
            windowInsetsController.hide(
                WindowInsetsCompat.Type.statusBars() or 
                WindowInsetsCompat.Type.navigationBars()
            )
            
            // For Android 15+, ensure proper handling of display cutouts and camera cutouts
            if (Build.VERSION.SDK_INT >= 35) {
                // Android 15 (API 35) specific enhancements
                // The display cutout padding in Compose will handle camera notches
                AppLogger.d("Android 15+ detected - using enhanced edge-to-edge with cutout support")
            }
        } else {
            // Legacy approach for older Android versions
            windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        }
        
        // Make status bar and navigation bar transparent
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        
        // For Android 15+, also handle the navigation bar appearance
        window.isNavigationBarContrastEnforced = false
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enhanced edge-to-edge setup for Android 15+ and camera cutouts
        setupEdgeToEdge()

        // Get camera config from intent
        cameraConfig = intent.getSerializableExtra(EXTRA_CAMERA_CONFIG) as? CameraConfig
            ?: CameraConfig()

        // Keep screen on during camera use
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Optionally override screen brightness based on configuration
        if (cameraConfig?.overrideScreenBrightness == true) {
            val layoutParams = window.attributes
            layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
            window.attributes = layoutParams
            AppLogger.d("Screen brightness overridden to maximum")
        } else {
            AppLogger.d("Using system screen brightness")
        }
        
        // Check camera permission and request if needed
        if (checkCameraPermission()) {
            showPermissionScreen = false
            // If camera permission is granted, request audio permission for video if needed
            requestAudioPermissionIfNeeded()
        } else {
            // For first launch, we don't know if it's permanently denied yet
            // Just show permission screen and let user try to grant
            isCameraPermissionPermanentlyDenied = false
            isAudioPermissionPermanentlyDenied = false
            
            // Show permission screen immediately
            showPermissionScreen = true
        }
        
        setContent {
            SaveAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (showPermissionScreen) {
                        CameraPermissionScreen(
                            isCameraPermissionPermanentlyDenied = isCameraPermissionPermanentlyDenied,
                            isAudioPermissionPermanentlyDenied = isAudioPermissionPermanentlyDenied,
                            needsAudioPermission = cameraConfig?.allowVideoCapture == true,
                            onRequestPermissions = { requestCameraPermission() },
                            onOpenSettings = { 
                                // Open app settings
                                val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = android.net.Uri.fromParts("package", packageName, null)
                                }
                                startActivity(intent)
                            },
                            onCancel = { finishWithResult(Activity.RESULT_CANCELED, emptyList()) }
                        )
                    } else {
                        CameraScreen(
                            config = cameraConfig ?: CameraConfig(),
                            onCaptureComplete = { uris ->
                                finishWithResult(Activity.RESULT_OK, uris)
                            },
                            onCancel = {
                                finishWithResult(Activity.RESULT_CANCELED, emptyList())
                            }
                        )
                    }
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Re-apply immersive mode when returning to the activity
        setupEdgeToEdge()
        
        // Re-check permissions when returning from settings
        checkAndUpdatePermissionStates()
    }
    
    private fun checkAndUpdatePermissionStates() {
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
                !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
            showPermissionScreen = true
        }
        
        // Update audio permission state if video capture is enabled
        if (cameraConfig?.allowVideoCapture == true) {
            val isAudioGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            
            if (!isAudioGranted && hasAudioPermissionBeenRequested) {
                // Only consider it permanently denied if we've already requested it before
                isAudioPermissionPermanentlyDenied = !shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)
            } else if (isAudioGranted) {
                isAudioPermissionPermanentlyDenied = false
            }
        }
    }
    
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // Re-apply immersive mode when the window regains focus
            setupEdgeToEdge()
        }
    }
    
    private fun finishWithResult(resultCode: Int, uris: List<Uri>) {
        val resultIntent = Intent().apply {
            putExtra(EXTRA_CAPTURED_URIS, ArrayList(uris.map { it.toString() }))
        }
        setResult(resultCode, resultIntent)
        finish()
    }
}