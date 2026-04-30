package net.opendasharchive.openarchive.core.presentation.components

import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors

/**
 * A reusable QR code scanner component using CameraX and ML Kit.
 * Features a stylish "WhatsApp-like" UI with an animated scanning line.
 */
@Composable
fun QRScanner(
    modifier: Modifier = Modifier,
    onQrCodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    
    var surfaceRequest by remember { mutableStateOf<SurfaceRequest?>(null) }
    var lastScannedTime by remember { mutableLongStateOf(0L) }
    val isScanSuccess = lastScannedTime > 0L && (System.currentTimeMillis() - lastScannedTime < 300L)
    
    // ProcessCameraProvider is a singleton
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    // Process scan result with feedback
    val processScanResult: (String) -> Unit = remember(onQrCodeScanned) {
        { result ->
            lastScannedTime = System.currentTimeMillis()
            onQrCodeScanned(result)
        }
    }

    LaunchedEffect(Unit) {
        val cameraProvider = cameraProviderFuture.get()
        
        val preview = Preview.Builder().build().apply {
            setSurfaceProvider { request ->
                surfaceRequest = request
            }
        }

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .apply {
                setAnalyzer(
                    cameraExecutor,
                    QRImageAnalyzer { result ->
                        processScanResult(result)
                    }
                )
            }

        // Use the back camera explicitly
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )
        } catch (e: Exception) {
            // Handle binding errors
        }
    }

    Box(modifier = modifier) {
        surfaceRequest?.let { request ->
            CameraXViewfinder(
                surfaceRequest = request,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Add the scanning overlay
        QRScannerOverlay(
            modifier = Modifier.fillMaxSize(),
            scanAreaSize = 200.dp,
            isSuccess = isScanSuccess
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }
}

/**
 * A stylish overlay for the QR scanner with a cutout and animated scanning line.
 */
@Composable
fun QRScannerOverlay(
    modifier: Modifier = Modifier,
    scanAreaSize: Dp = 200.dp,
    scanLineColor: Color = Color.Cyan,
    isSuccess: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ScanningLine")
    val scanLineY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ScanLinePosition"
    )

    val successColor = Color.Green
    val cornerColor by animateColorAsState(
        targetValue = if (isSuccess) successColor else Color.White,
        animationSpec = tween(durationMillis = 200),
        label = "CornerColor"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val scanAreaPx = scanAreaSize.toPx()
        
        val left = (width - scanAreaPx) / 2
        val top = (height - scanAreaPx) / 2
        val right = left + scanAreaPx
        val bottom = top + scanAreaPx
        
        val scanRect = Rect(left, top, right, bottom)

        // 1. Draw semi-transparent overlay
        drawPath(
            path = Path().apply {
                addRect(Rect(0f, 0f, width, height))
                addRect(scanRect)
                fillType = PathFillType.EvenOdd
            },
            color = if (isSuccess) successColor.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.6f)
        )

        // 2. Draw border/corners of the scan area
        val cornerSize = 25.dp.toPx()
        val strokeWidth = 3.dp.toPx()

        // Top-left corner
        drawLine(cornerColor, Offset(left, top), Offset(left + cornerSize, top), strokeWidth)
        drawLine(cornerColor, Offset(left, top), Offset(left, top + cornerSize), strokeWidth)
        
        // Top-right corner
        drawLine(cornerColor, Offset(right, top), Offset(right - cornerSize, top), strokeWidth)
        drawLine(cornerColor, Offset(right, top), Offset(right, top + cornerSize), strokeWidth)
        
        // Bottom-left corner
        drawLine(cornerColor, Offset(left, bottom), Offset(left + cornerSize, bottom), strokeWidth)
        drawLine(cornerColor, Offset(left, bottom), Offset(left, bottom - cornerSize), strokeWidth)
        
        // Bottom-right corner
        drawLine(cornerColor, Offset(right, bottom), Offset(right - cornerSize, bottom), strokeWidth)
        drawLine(cornerColor, Offset(right, bottom), Offset(right, bottom - cornerSize), strokeWidth)

        // 3. Draw animated scanning line
        val lineY = top + (scanAreaPx * scanLineY)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    scanLineColor.copy(alpha = 0f),
                    scanLineColor,
                    scanLineColor.copy(alpha = 0f)
                ),
                startY = lineY - 10.dp.toPx(),
                endY = lineY + 10.dp.toPx()
            ),
            topLeft = Offset(left + 2.dp.toPx(), lineY - 1.dp.toPx()),
            size = Size(scanAreaPx - 4.dp.toPx(), 2.dp.toPx())
        )
    }
}
