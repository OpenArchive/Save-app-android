package net.opendasharchive.openarchive.features.media.camera

import android.content.Context
import android.net.Uri
import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.delay
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.logger.AppLogger
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun CameraScreen(
    modifier: Modifier = Modifier,
    config: CameraConfig = CameraConfig(),
    onCaptureComplete: (List<Uri>) -> Unit,
    onCancel: () -> Unit,
    viewModel: CameraViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraState by viewModel.state.collectAsState()
    
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var cameraExecutor by remember { mutableStateOf<ExecutorService?>(null) }
    
    // Initialize camera executor
    LaunchedEffect(Unit) {
        cameraExecutor = Executors.newSingleThreadExecutor()
    }
    
    // Cleanup on disposal
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor?.shutdown()
        }
    }
    
    // Show preview screen when item is captured
    if (cameraState.showPreview && cameraState.currentPreviewItem != null) {
        CameraPreviewScreen(
            item = cameraState.currentPreviewItem!!,
            config = config,
            onConfirm = { item ->
                val uris = viewModel.confirmCapture(item)
                if (config.allowMultipleCapture) {
                    viewModel.hidePreview()
                } else {
                    onCaptureComplete(uris)
                }
            },
            onRetake = { item ->
                viewModel.retakeCapture(item)
            },
            onCancel = {
                viewModel.hidePreview(deleteFile = true)
            }
        )
    } else {
        // Main camera interface

        // ===== Idle Timeout State Management =====
        // Tracks user interaction to automatically pause camera after inactivity
        var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }
        var isIdle by remember { mutableStateOf(false) }

        /**
         * Resets the idle timer and resumes camera if it was paused.
         * Called whenever user interacts with the camera screen.
         */
        val resetIdleTimer = {
            lastInteractionTime = System.currentTimeMillis()
            if (isIdle) {
                isIdle = false
                AppLogger.d("Camera resumed from idle state")
            }
        }

        // ===== Idle Timeout Monitor =====
        // Monitors inactivity and pauses camera when timeout is reached
        if (config.enableIdleTimeout) {
            LaunchedEffect(lastInteractionTime) {
                // Wait for the configured timeout duration
                delay(config.idleTimeoutSeconds * 1000L)

                // Check if we're still inactive after the delay
                val currentTime = System.currentTimeMillis()
                val elapsedSeconds = (currentTime - lastInteractionTime) / 1000L

                if (!isIdle && elapsedSeconds >= config.idleTimeoutSeconds) {
                    // Unbind camera resources to save battery and reduce thermal load
                    cameraProvider?.unbindAll()
                    imageCapture = null
                    videoCapture = null
                    isIdle = true
                    AppLogger.d("Camera paused due to ${config.idleTimeoutSeconds}s inactivity")
                }
            }
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
                // Detect any touch events to reset idle timer
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitPointerEvent()
                        if (config.enableIdleTimeout) {
                            resetIdleTimer()
                        }
                    }
                }
        ) {
            // Camera preview
            var previewView by remember { mutableStateOf<PreviewView?>(null) }

            // ===== Camera Setup =====
            // Setup camera when preview view is ready, camera switches, or idle state changes
            // NOTE: Flash mode changes are handled separately to avoid unnecessary rebinding
            LaunchedEffect(previewView, cameraState.isFrontCamera, isIdle) {
                // Only setup camera if not in idle state
                if (!isIdle) {
                    previewView?.let { preview ->
                        setupCamera(
                            context = context,
                            config = config,
                            previewView = preview,
                            lifecycleOwner = lifecycleOwner,
                            cameraState = cameraState,
                            onCameraReady = { provider, cam, imgCapture, vidCapture ->
                                cameraProvider = provider
                                camera = cam
                                imageCapture = imgCapture
                                videoCapture = vidCapture
                            },
                            onFlashSupportChanged = { isSupported ->
                                viewModel.updateFlashSupport(isSupported)
                            }
                        )
                    }
                }
            }

            // ===== Flash Mode Update for Photo Mode =====
            // Rebind only ImageCapture when flash mode changes in photo mode
            LaunchedEffect(cameraState.flashMode, cameraState.captureMode) {
                if (cameraState.captureMode == CameraCaptureMode.PHOTO &&
                    cameraProvider != null &&
                    camera != null &&
                    !isIdle) {
                    try {
                        // Unbind only the old ImageCapture
                        imageCapture?.let { cameraProvider?.unbind(it) }

                        // Create new ImageCapture with updated flash mode
                        val newImageCapture = ImageCapture.Builder()
                            .setFlashMode(cameraState.flashMode)
                            .build()

                        // Rebind only ImageCapture (keeps preview and video running smoothly)
                        cameraProvider?.bindToLifecycle(
                            lifecycleOwner,
                            if (cameraState.isFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA
                            else CameraSelector.DEFAULT_BACK_CAMERA,
                            newImageCapture
                        )

                        imageCapture = newImageCapture
                        AppLogger.d("ImageCapture flash mode updated to: ${cameraState.flashMode}")
                    } catch (e: Exception) {
                        AppLogger.e("Failed to update ImageCapture flash mode", e)
                    }
                }
            }

            // ===== Torch Mode Control for Video Mode =====
            // Enable/disable torch (flashlight) in video mode based on flash mode
            // This provides continuous light during video recording or video mode preview
            LaunchedEffect(cameraState.flashMode, cameraState.captureMode, camera) {
                // Wait a bit for camera to be fully ready
                delay(100)

                camera?.let { cam ->
                    try {
                        // Check if this camera supports torch
                        val hasTorch = cam.cameraInfo.hasFlashUnit()

                        if (!hasTorch) {
                            AppLogger.w("Camera does not support torch/flash")
                            return@let
                        }

                        if (cameraState.captureMode == CameraCaptureMode.VIDEO) {
                            // In video mode, enable torch based on flash mode
                            val shouldEnableTorch = cameraState.flashMode != ImageCapture.FLASH_MODE_OFF

                            AppLogger.d("Video mode - Enabling torch: $shouldEnableTorch (flashMode=${cameraState.flashMode})")

                            cam.cameraControl.enableTorch(shouldEnableTorch).addListener({
                                val torchState = cam.cameraInfo.torchState.value
                                AppLogger.d("Torch enable completed - Torch state: $torchState")
                            }, ContextCompat.getMainExecutor(context))

                        } else {
                            // Photo mode - ensure torch is off
                            AppLogger.d("Photo mode - Disabling torch")
                            cam.cameraControl.enableTorch(false)
                        }
                    } catch (e: Exception) {
                        AppLogger.e("Failed to control torch mode", e)
                    }
                } ?: run {
                    AppLogger.w("Camera not available for torch control")
                }
            }

            // ===== Preview View Configuration =====
            // Uses configured implementation mode (PERFORMANCE or COMPATIBLE)
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER

                        // Use configured implementation mode for optimal performance
                        implementationMode = when (config.implementationMode) {
                            ImplementationMode.PERFORMANCE -> PreviewView.ImplementationMode.PERFORMANCE
                            ImplementationMode.COMPATIBLE -> PreviewView.ImplementationMode.COMPATIBLE
                        }

                        AppLogger.d("PreviewView initialized with ${config.implementationMode} mode")
                    }.also { preview ->
                        previewView = preview
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // Grid overlay
            if (cameraState.showGrid && config.showGridToggle) {
                CameraGridOverlay(modifier = Modifier.fillMaxSize())
            }
            
            // Top controls with system bars and display cutout padding
            CameraTopControls(
                config = config,
                cameraState = cameraState,
                onFlashToggle = { viewModel.toggleFlashMode() },
                onGridToggle = { viewModel.toggleGrid() },
                onCancel = onCancel,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .displayCutoutPadding()
            )

            // Bottom controls with navigation bars padding
            CameraBottomControls(
                config = config,
                cameraState = cameraState,
                onCameraSwitch = { viewModel.toggleCamera() },
                onCaptureModeChange = { mode ->
                    viewModel.updateCaptureMode(mode)
                },
                onPhotoCapture = {
                    imageCapture?.let { capture ->
                        viewModel.capturePhoto(
                            context = context,
                            imageCapture = capture,
                            onSuccess = { uri ->
                                AppLogger.d("Photo captured: $uri")
                            },
                            onError = { error ->
                                AppLogger.e("Photo capture failed", error)
                            }
                        )
                    }
                },
                onVideoStart = {
                    videoCapture?.let { capture ->
                        viewModel.startVideoRecording(
                            context = context,
                            videoCapture = capture,
                            onSuccess = { uri ->
                                AppLogger.d("Video captured: $uri")
                            },
                            onError = { error ->
                                AppLogger.e("Video capture failed", error)
                            }
                        )
                    }
                },
                onVideoStop = {
                    viewModel.stopVideoRecording()
                },
                onDone = {
                    val allUris = viewModel.getAllCapturedUris()
                    if (allUris.isNotEmpty()) {
                        onCaptureComplete(allUris)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            )

            // ===== Idle State Overlay =====
            // Shows when camera is paused due to inactivity
            if (isIdle && config.enableIdleTimeout) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .clickable {
                            resetIdleTimer()
                            AppLogger.d("Camera resuming from idle - user tapped screen")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Pause icon
                        Icon(
                            painter = painterResource(R.drawable.ic_pause),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(64.dp)
                        )

                        // Informational text
                        Text(
                            text = stringResource(R.string.camera_paused_message),
                            color = Color.White,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraTopControls(
    config: CameraConfig,
    cameraState: CameraState,
    onFlashToggle: () -> Unit,
    onGridToggle: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cancel button (left aligned)
        IconButton(
            onClick = onCancel,
            modifier = Modifier
                .size(48.dp)
                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.close),
                tint = Color.White
            )
        }

        // Spacer to push grid to center
        Spacer(modifier = Modifier.weight(1f))

        // Grid toggle OR Recording timer (centered)
        if (config.showGridToggle) {
            val recordingStartTime = cameraState.recordingStartTime
            AnimatedContent(
                targetState = cameraState.isRecording && recordingStartTime != null,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith
                            fadeOut(animationSpec = tween(300))
                },
                label = "grid_timer_transition"
            ) { isRecording ->
                if (isRecording && recordingStartTime != null) {
                    // Show compact recording timer
                    RecordingTimerCompact(startTime = recordingStartTime)
                } else {
                    // Show grid toggle button
                    IconButton(
                        onClick = onGridToggle,
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Icon(
                            painter = if (cameraState.showGrid) painterResource(R.drawable.ic_grid_on) else painterResource(R.drawable.ic_grid_off),
                            contentDescription = stringResource(R.string.grid),
                            tint = if (cameraState.showGrid) Color.Yellow else Color.White
                        )
                    }
                }
            }
        }

        // Spacer to push flash to right
        Spacer(modifier = Modifier.weight(1f))

        // Flash toggle (right aligned)
        if (config.showFlashToggle && cameraState.isFlashSupported) {
            IconButton(
                onClick = onFlashToggle,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
            ) {
                val flashIcon = when (cameraState.flashMode) {
                    ImageCapture.FLASH_MODE_ON -> painterResource(R.drawable.ic_flash_on)
                    ImageCapture.FLASH_MODE_AUTO -> painterResource(R.drawable.ic_flash_auto)
                    else -> painterResource(R.drawable.ic_flash_off)
                }
                Icon(
                    painter = flashIcon,
                    contentDescription = stringResource(R.string.flash),
                    tint = Color.White
                )
            }
        } else {
            // Empty space to balance layout when flash is not shown
            Spacer(modifier = Modifier.size(48.dp))
        }
    }
}

@Composable
private fun RecordingTimerCompact(
    startTime: Long,
    modifier: Modifier = Modifier
) {
    var elapsedTime by remember { mutableLongStateOf(0L) }

    // Update elapsed time every 100ms for smooth display
    LaunchedEffect(startTime) {
        while (true) {
            elapsedTime = System.currentTimeMillis() - startTime
            delay(100)
        }
    }

    // Pulsing animation for the red dot
    val infiniteTransition = rememberInfiniteTransition(label = "recording_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "recording_alpha"
    )

    // Format time as MM:SS
    val minutes = (elapsedTime / 1000) / 60
    val seconds = (elapsedTime / 1000) % 60
    val timeText = String.format("%02d:%02d", minutes, seconds)

    Row(
        modifier = modifier
            .background(
                color = Color.Black.copy(alpha = 0.5f),
                shape = CircleShape
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pulsing red dot
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(Color.Red.copy(alpha = alpha))
        )

        Spacer(modifier = Modifier.width(6.dp))

        // Timer text
        Text(
            text = timeText,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Sets up the camera by obtaining ProcessCameraProvider and binding camera use cases.
 *
 * @param context Application context
 * @param config Camera configuration with optimization settings
 * @param previewView PreviewView to display camera feed
 * @param lifecycleOwner Lifecycle owner for camera binding
 * @param cameraState Current camera state
 * @param onCameraReady Callback when camera is ready with provider, camera instance, and use cases
 * @param onFlashSupportChanged Callback when flash support is determined
 */
private fun setupCamera(
    context: Context,
    config: CameraConfig,
    previewView: PreviewView?,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    cameraState: CameraState,
    onCameraReady: (ProcessCameraProvider, Camera, ImageCapture, VideoCapture<Recorder>) -> Unit,
    onFlashSupportChanged: (Boolean) -> Unit
) {
    val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        try {
            val cameraProvider = cameraProviderFuture.get()
            bindCamera(
                cameraProvider,
                config,
                previewView,
                lifecycleOwner,
                cameraState,
                onCameraReady,
                onFlashSupportChanged
            )
        } catch (e: Exception) {
            AppLogger.e("Failed to get camera provider", e)
        }
    }, ContextCompat.getMainExecutor(context))
}

/**
 * Binds camera use cases to the lifecycle with configured optimization settings.
 *
 * This function creates and configures camera use cases based on the provided configuration:
 * - Preview: Uses configured resolution (HD/FHD/MAX) for optimal resource usage
 * - ImageCapture: Photo capture with flash mode support
 * - VideoCapture: Video recording with configured quality (SD/HD/FHD/UHD)
 *
 * The Camera instance is returned to allow torch mode control for video recording.
 *
 * @param cameraProvider Camera provider instance
 * @param config Camera configuration with resolution and quality settings
 * @param previewView PreviewView to display camera feed
 * @param lifecycleOwner Lifecycle owner for camera binding
 * @param cameraState Current camera state including flash mode
 * @param onCameraReady Callback when camera is ready (includes Camera instance for torch control)
 * @param onFlashSupportChanged Callback when flash support is determined
 */
private fun bindCamera(
    cameraProvider: ProcessCameraProvider,
    config: CameraConfig,
    previewView: PreviewView?,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    cameraState: CameraState,
    onCameraReady: (ProcessCameraProvider, Camera, ImageCapture, VideoCapture<Recorder>) -> Unit,
    onFlashSupportChanged: (Boolean) -> Unit
) {
    try {
        // Unbind all previous use cases
        cameraProvider.unbindAll()

        // Select camera (front or back)
        val cameraSelector = if (cameraState.isFrontCamera) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        // ===== Preview Configuration =====
        // Configure preview resolution based on config setting
        val previewSize = when (config.previewResolution) {
            PreviewResolution.HD -> Size(1280, 720)
            PreviewResolution.FHD -> Size(1920, 1080)
            PreviewResolution.MAX -> Size(Int.MAX_VALUE, Int.MAX_VALUE) // Use maximum available
        }

        val resolutionSelector = ResolutionSelector.Builder()
            .setResolutionStrategy(
                ResolutionStrategy(
                    previewSize,
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                )
            )
            .build()

        val preview = Preview.Builder()
            .setResolutionSelector(resolutionSelector)
            .build()
            .also {
                previewView?.let { pv ->
                    it.surfaceProvider = pv.surfaceProvider
                }
            }

        AppLogger.d("Preview configured with ${config.previewResolution} resolution (${previewSize.width}x${previewSize.height})")

        // ===== Image Capture Configuration =====
        val imageCapture = ImageCapture.Builder()
            .setFlashMode(cameraState.flashMode)
            .build()

        // ===== Video Capture Configuration =====
        // Configure video quality based on config setting
        val videoQuality = when (config.videoQuality) {
            VideoQuality.SD -> Quality.SD
            VideoQuality.HD -> Quality.HD
            VideoQuality.FHD -> Quality.FHD
            VideoQuality.UHD -> Quality.UHD
        }

        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(videoQuality))
            .build()

        val videoCapture = VideoCapture.withOutput(recorder)

        AppLogger.d("Video recording configured with ${config.videoQuality} quality")

        // ===== Bind Use Cases to Lifecycle =====
        val camera = cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageCapture,
            videoCapture
        )

        // ===== Flash Support Detection =====
        // Check if camera has flash hardware and update UI accordingly
        val flashSupported = camera.cameraInfo.hasFlashUnit()
        onFlashSupportChanged(flashSupported)

        // ===== Exposure Compensation =====
        // Set a neutral exposure to ensure preview isn't too dark
        // CameraX auto-exposure sometimes starts very dark
        try {
            val exposureState = camera.cameraInfo.exposureState
            if (exposureState.isExposureCompensationSupported) {
                // Set exposure to neutral (0) or slightly positive for better preview brightness
                camera.cameraControl.setExposureCompensationIndex(0)
                AppLogger.d("Exposure compensation set to neutral")
            }
        } catch (e: Exception) {
            AppLogger.e("Failed to set exposure compensation", e)
        }

        AppLogger.d("Camera bound successfully - Flash supported: $flashSupported")

        // Notify that camera is ready with all use cases (including Camera instance for torch control)
        onCameraReady(cameraProvider, camera, imageCapture, videoCapture)

    } catch (e: Exception) {
        AppLogger.e("Failed to bind camera use cases", e)
    }
}