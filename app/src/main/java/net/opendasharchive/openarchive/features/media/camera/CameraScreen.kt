package net.opendasharchive.openarchive.features.media.camera

import android.content.Context
import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.common.util.concurrent.ListenableFuture
import androidx.compose.ui.res.stringResource
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
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraState by viewModel.state.collectAsState()
    
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
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
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Camera preview
            var previewView by remember { mutableStateOf<PreviewView?>(null) }
            
            // Setup camera when preview view is ready or camera settings change
            LaunchedEffect(previewView, cameraState.isFrontCamera) {
                previewView?.let { preview ->
                    setupCamera(
                        context = context,
                        previewView = preview,
                        lifecycleOwner = lifecycleOwner,
                        cameraState = cameraState,
                        onCameraReady = { provider, imgCapture, vidCapture ->
                            cameraProvider = provider
                            imageCapture = imgCapture
                            videoCapture = vidCapture
                        },
                        onFlashSupportChanged = { isSupported ->
                            viewModel.updateFlashSupport(isSupported)
                        }
                    )
                }
            }
            
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
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
                imageVector = Icons.Default.Close,
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
                            imageVector = if (cameraState.showGrid) Icons.Default.GridOn else Icons.Default.GridOff,
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
                    ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                    ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                    else -> Icons.Default.FlashOff
                }
                Icon(
                    imageVector = flashIcon,
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

private fun setupCamera(
    context: Context,
    previewView: PreviewView?,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    cameraState: CameraState,
    onCameraReady: (ProcessCameraProvider, ImageCapture, VideoCapture<Recorder>) -> Unit,
    onFlashSupportChanged: (Boolean) -> Unit
) {
    val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        try {
            val cameraProvider = cameraProviderFuture.get()
            bindCamera(cameraProvider, previewView, lifecycleOwner, cameraState, onCameraReady, onFlashSupportChanged)
        } catch (e: Exception) {
            AppLogger.e("Failed to get camera provider", e)
        }
    }, ContextCompat.getMainExecutor(context))
}

private fun bindCamera(
    cameraProvider: ProcessCameraProvider,
    previewView: PreviewView?,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    cameraState: CameraState,
    onCameraReady: (ProcessCameraProvider, ImageCapture, VideoCapture<Recorder>) -> Unit,
    onFlashSupportChanged: (Boolean) -> Unit
) {
    try {
        cameraProvider.unbindAll()
        
        val cameraSelector = if (cameraState.isFrontCamera) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        
        val preview = Preview.Builder().build().also {
            previewView?.let { pv ->
                it.setSurfaceProvider(pv.surfaceProvider)
            }
        }
        
        val imageCapture = ImageCapture.Builder()
            .setFlashMode(cameraState.flashMode)
            .build()
        
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HD))
            .build()
        val videoCapture = VideoCapture.withOutput(recorder)
        
        val camera = cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageCapture,
            videoCapture
        )
        
        // Check flash support
        val flashSupported = camera.cameraInfo.hasFlashUnit()
        onFlashSupportChanged(flashSupported)
        
        onCameraReady(cameraProvider, imageCapture, videoCapture)
        
    } catch (e: Exception) {
        AppLogger.e("Failed to bind camera", e)
    }
}