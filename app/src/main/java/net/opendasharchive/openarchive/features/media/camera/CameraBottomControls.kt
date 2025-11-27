package net.opendasharchive.openarchive.features.media.camera

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import net.opendasharchive.openarchive.R

@Composable
fun CameraBottomControls(
    config: CameraConfig,
    cameraState: CameraState,
    onCameraSwitch: () -> Unit,
    onCaptureModeChange: (CameraCaptureMode) -> Unit,
    onPhotoCapture: () -> Unit,
    onVideoStart: () -> Unit,
    onVideoStop: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Mode selector (Photo/Video)
        if (config.allowPhotoCapture && config.allowVideoCapture) {
            CameraModeSelector(
                currentMode = cameraState.captureMode,
                onModeChange = onCaptureModeChange
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Captured items count (left side) - fixed width to match right side
            Box(
                modifier = Modifier.width(80.dp),
                contentAlignment = Alignment.Center
            ) {
                if (config.allowMultipleCapture) {
                    CapturedItemsIndicator(
                        count = cameraState.capturedItems.size,
                        onDone = onDone
                    )
                }
            }

            // Main capture button (center) - properly centered
            CameraCaptureButton(
                captureMode = cameraState.captureMode,
                isRecording = cameraState.isRecording,
                onPhotoCapture = onPhotoCapture,
                onVideoStart = onVideoStart,
                onVideoStop = onVideoStop
            )

            // Camera switch button (right side) - fixed width to match left side
            Box(
                modifier = Modifier.width(80.dp),
                contentAlignment = Alignment.Center
            ) {
                if (config.showCameraSwitch) {
                    IconButton(
                        onClick = onCameraSwitch,
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (cameraState.isFrontCamera) Icons.Default.CameraFront else Icons.Default.CameraRear,
                            contentDescription = stringResource(R.string.switch_camera),
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraModeSelector(
    currentMode: CameraCaptureMode,
    onModeChange: (CameraCaptureMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                Color.Black.copy(alpha = 0.3f),
                RoundedCornerShape(20.dp)
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        CameraCaptureMode.entries.forEach { mode ->
            val isSelected = currentMode == mode
            val context = androidx.compose.ui.platform.LocalContext.current
            Text(
                text = when (mode) {
                    CameraCaptureMode.PHOTO -> context.getString(R.string.photo_label)
                    CameraCaptureMode.VIDEO -> context.getString(R.string.video_label)
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isSelected) Color.White else Color.Transparent
                    )
                    .clickable { onModeChange(mode) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = if (isSelected) Color.Black else Color.White,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun CameraCaptureButton(
    captureMode: CameraCaptureMode,
    isRecording: Boolean,
    onPhotoCapture: () -> Unit,
    onVideoStart: () -> Unit,
    onVideoStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    
    Box(
        modifier = modifier.size(80.dp),
        contentAlignment = Alignment.Center
    ) {
        when (captureMode) {
            CameraCaptureMode.PHOTO -> {
                // Photo capture button
                val scale by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                    label = "photo_button_scale"
                )
                
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(4.dp, Color.Gray, CircleShape)
                        .clickable { onPhotoCapture() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = stringResource(R.string.capture_photo),
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            CameraCaptureMode.VIDEO -> {
                // Video capture button with recording animation
                if (isRecording) {
                    val infiniteTransition = rememberInfiniteTransition(label = "recording_animation")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = EaseInOut),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse_scale"
                    )
                    
                    // Recording indicator with pulsing red circle
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(Color.Red)
                            .clickable { onVideoStop() },
                        contentAlignment = Alignment.Center
                    ) {
                        // Stop icon (square)
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color.White, RoundedCornerShape(4.dp))
                        )
                    }
                    
                    // Recording pulse effect
                    Canvas(
                        modifier = Modifier.size(80.dp)
                    ) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val radius = size.minDimension / 2
                        
                        // Outer pulsing circle
                        drawCircle(
                            color = Color.Red.copy(alpha = 0.3f),
                            radius = radius * pulseScale,
                            center = center,
                            style = Stroke(
                                width = with(density) { 2.dp.toPx() },
                                cap = StrokeCap.Round
                            )
                        )
                    }
                } else {
                    // Start recording button
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(Color.Red)
                            .border(4.dp, Color.White, CircleShape)
                            .clickable { onVideoStart() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = stringResource(R.string.start_recording),
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CapturedItemsIndicator(
    count: Int,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (count > 0) {
        Button(
            onClick = onDone,
            modifier = modifier,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Blue,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.done),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(R.string.done_with_count, count),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    } else {
        Box(modifier = modifier.size(80.dp, 40.dp))
    }
}