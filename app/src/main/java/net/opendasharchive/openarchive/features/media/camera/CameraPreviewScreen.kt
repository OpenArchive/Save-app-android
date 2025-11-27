package net.opendasharchive.openarchive.features.media.camera

import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import androidx.compose.ui.res.stringResource
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.logger.AppLogger

@Composable
fun CameraPreviewScreen(
    item: CapturedItem,
    config: CameraConfig,
    onConfirm: (CapturedItem) -> Unit,
    onRetake: (CapturedItem) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Preview content
        when (item.type) {
            CameraCaptureMode.PHOTO -> {
                // Photo preview
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(item.uri)
                        .build(),
                    contentDescription = stringResource(R.string.captured_photo),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
            CameraCaptureMode.VIDEO -> {
                // Video preview with playback capability
                VideoPreviewPlayer(
                    uri = item.uri,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Video duration overlay
                VideoDurationOverlay(
                    uri = item.uri,
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }
        }
        
        // Top controls
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .displayCutoutPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back/Cancel button
            IconButton(
                onClick = onCancel,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = Color.White
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Media type indicator
            Row(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = when (item.type) {
                        CameraCaptureMode.PHOTO -> Icons.Default.Photo
                        CameraCaptureMode.VIDEO -> Icons.Default.Videocam
                    },
                    contentDescription = item.type.name,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = when (item.type) {
                        CameraCaptureMode.PHOTO -> stringResource(R.string.photo_label)
                        CameraCaptureMode.VIDEO -> stringResource(R.string.video_label)
                    },
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        // Bottom controls - positioned above video player controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 100.dp) // Extra padding to sit above video player controls
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Retake button
            Button(
                onClick = { onRetake(item) },
                modifier = Modifier.height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(Color.White)
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.retake),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.retake),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Confirm/Use button
            Button(
                onClick = { onConfirm(item) },
                modifier = Modifier.height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Blue,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = if (config.allowMultipleCapture) stringResource(R.string.use) else stringResource(R.string.done),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (config.allowMultipleCapture) stringResource(R.string.use) else stringResource(R.string.done),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoDurationOverlay(
    uri: Uri,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var duration by remember { mutableLongStateOf(0L) }
    
    LaunchedEffect(uri) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            retriever.release()
        } catch (e: Exception) {
            AppLogger.e("Error getting video duration", e)
            duration = 0L
        }
    }
    
    if (duration > 0) {
        val seconds = duration / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        
        Text(
            text = String.format("%02d:%02d", minutes, remainingSeconds),
            modifier = modifier
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun VideoPreviewPlayer(
    uri: Uri,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Create ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(uri))
                prepare()
                playWhenReady = false
                repeatMode = Player.REPEAT_MODE_OFF // Play once and stop
            }
    }
    
    // Cleanup player when composable is disposed
    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }
    
    Box(modifier = modifier) {
        // Video player view with native controls
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    controllerAutoShow = true // Show native controls
                    controllerShowTimeoutMs = 5000 // Hide after 5 seconds of inactivity
                    useController = true // Enable native playback controls
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}