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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.common.util.concurrent.ListenableFuture
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
                viewModel.hidePreview()
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
                onCameraSwitch = { 
                    viewModel.toggleCamera()
                },
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
                onCaptureModeChange = { mode ->
                    viewModel.updateCaptureMode(mode)
                },
                onPhotoCapture = {
                    imageCapture?.let { capture ->
                        viewModel.capturePhoto(
                            context = context,
                            imageCapture = capture,
                            useCleanFilenames = config.useCleanFilenames,
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
                            useCleanFilenames = config.useCleanFilenames,
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
    onCameraSwitch: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cancel button
        IconButton(
            onClick = onCancel,
            modifier = Modifier
                .size(48.dp)
                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Cancel",
                tint = Color.White
            )
        }
        
        // Center controls (flash, grid)
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Flash toggle
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
                        contentDescription = "Flash",
                        tint = Color.White
                    )
                }
            }
            
            // Grid toggle
            if (config.showGridToggle) {
                IconButton(
                    onClick = onGridToggle,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (cameraState.showGrid) Icons.Default.GridOn else Icons.Default.GridOff,
                        contentDescription = "Grid",
                        tint = if (cameraState.showGrid) Color.Yellow else Color.White
                    )
                }
            }
        }
        
        // Camera switch button
        if (config.showCameraSwitch) {
            IconButton(
                onClick = onCameraSwitch,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraFront,
                    contentDescription = "Switch Camera",
                    tint = Color.White
                )
            }
        } else {
            Spacer(modifier = Modifier.size(48.dp))
        }
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