package net.opendasharchive.openarchive.features.media.camera

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.painterResource
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left column - Done button (takes 1/3 of width)
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (config.allowMultipleCapture && cameraState.capturedItems.size > 0) {
                    CapturedItemsIndicator(
                        count = cameraState.capturedItems.size,
                        onDone = onDone
                    )
                }
            }

            // Center column - Capture button (takes 1/3 of width, always centered)
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CameraCaptureButton(
                    captureMode = cameraState.captureMode,
                    isRecording = cameraState.isRecording,
                    onPhotoCapture = onPhotoCapture,
                    onVideoStart = onVideoStart,
                    onVideoStop = onVideoStop
                )
            }

            // Right column - Camera switch button (takes 1/3 of width)
            Box(
                modifier = Modifier.weight(1f),
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
                            painter = if (cameraState.isFrontCamera) painterResource( R.drawable.ic_camera_rear) else painterResource(R.drawable.ic_camera_front),
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
            Text(
                text = when (mode) {
                    CameraCaptureMode.PHOTO -> stringResource(R.string.photo_label)
                    CameraCaptureMode.VIDEO -> stringResource(R.string.video_label)
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
                        painter = painterResource(R.drawable.ic_camera_alt),
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
                            painter = painterResource(R.drawable.ic_videocam),
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
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            // Done button
            Button(
                onClick = onDone,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Blue,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_done),
                        contentDescription = stringResource(R.string.done),
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Text(
                        text = stringResource(R.string.done),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Badge with count (positioned at top-right of button)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .heightIn(min = 20.dp)
                    .background(Color.White, CircleShape)
                    .padding(horizontal = 7.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = count.toString(),
                    color = Color.Black,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}