package net.opendasharchive.openarchive.upload

import android.content.res.ColorStateList
import android.content.res.Configuration
import android.text.format.Formatter
import android.widget.ImageView
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.error
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.MontserratFontFamily
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.util.PdfThumbnailLoader
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun UploadManagerScreen(
    viewModel: UploadManagerViewModel,
    onClose: () -> Unit,
    onShowRetryDialog: (Media, Int) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is UploadManagerEvent.Close -> onClose()
                is UploadManagerEvent.ShowRetryDialog -> onShowRetryDialog(
                    event.media,
                    event.position
                )
            }
        }
    }

    // Auto-dismiss when all items are deleted
    LaunchedEffect(state.mediaList.size) {
        if (state.mediaList.isEmpty()) {
            onClose()
        }
    }

    UploadManagerContent(
        state = state,
        onAction = viewModel::onAction,
        onShowRetryDialog = onShowRetryDialog
    )
}

@Composable
private fun UploadManagerContent(
    state: UploadManagerState,
    onAction: (UploadManagerAction) -> Unit,
    onShowRetryDialog: (Media, Int) -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        onAction(UploadManagerAction.MoveItem(from.index, to.index))
        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                color = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        // Drag Handle (Material 3 style)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(4.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }

        // Top Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.edit_queue),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = MontserratFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.uploading_is_paused),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = MontserratFontFamily,
                        fontWeight = FontWeight.Medium
                    ),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            TextButton(
                onClick = { onAction(UploadManagerAction.Close) },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Text(
                    text = stringResource(R.string.done).uppercase(),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = MontserratFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Reorderable List
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(
                items = state.mediaList,
                key = { _, media -> media.id }
            ) { index, media ->
                ReorderableItem(reorderableState, key = media.id) { isDragging ->
                    UploadMediaItem(
                        media = media,
                        isDragging = isDragging,
                        onDelete = {
                            if (media.sStatus == Media.Status.Error) {
                                onShowRetryDialog(media, index)
                            } else {
                                onAction(UploadManagerAction.DeleteItem(index))
                            }
                        },
                        modifier = Modifier
                            .draggableHandle()
                            .longPressDraggableHandle()
                    )
                }
            }
        }
    }
}

@Composable
private fun UploadMediaItem(
    media: Media,
    isDragging: Boolean,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val elevation by animateDpAsState(if (isDragging) 4.dp else 0.dp)

    val alpha = if (!isDragging) 1f else 0.7f

    Surface(
        shadowElevation = elevation
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(70.dp)
                .padding(5.dp)
                .background(
                    color = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(4.dp)
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Delete Button
            Box(
                modifier = Modifier
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onDelete,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_trash),
                        contentDescription = stringResource(R.string.menu_delete),
                        tint = colorResource(R.color.colorDanger),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Thumbnail Container
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .padding(horizontal = 8.dp)
            ) {
                MediaThumbnail(
                    media = media,
                    alpha = alpha
                )

                // Overlay for status
                MediaStatusOverlay(media = media)
            }

            // Title and File Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp, end = 8.dp),
                verticalArrangement = Arrangement.Center
            ) {
                val titleText = buildString {
                    if (media.sStatus == Media.Status.Error) {
                        append(stringResource(R.string.error))
                        append(": ")
                    }
                    append(media.title)
                }

                if (titleText.isNotBlank()) {
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = MontserratFontFamily,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                val fileInfoText = getFileInfoText(media)
                if (fileInfoText.isNotBlank()) {
                    Text(
                        text = fileInfoText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = MontserratFontFamily
                        ),
                        maxLines = 1,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            // Drag Handle
            Icon(
                painter = painterResource(id = R.drawable.ic_reorder_black_24dp),
                contentDescription = stringResource(R.string.uploads),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}

@Composable
private fun MediaThumbnail(
    media: Media,
    alpha: Float
) {
    val context = LocalContext.current

    when {
        media.mimeType.startsWith("image") -> {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(media.fileUri)
                    .error(R.drawable.ic_image)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                error = {
                    PlaceholderIcon(
                        drawableRes = R.drawable.ic_image,
                        alpha = alpha
                    )
                },
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(alpha)
            )
        }

        media.mimeType.startsWith("video") -> {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(media.fileUri)
                    .error(R.drawable.ic_video)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                error = {
                    PlaceholderIcon(
                        drawableRes = R.drawable.ic_video,
                        alpha = alpha
                    )
                },
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(alpha)
            )
        }

        media.mimeType.startsWith("audio") -> {
            PlaceholderIcon(
                drawableRes = R.drawable.ic_music,
                alpha = alpha
            )
        }

        media.mimeType == "application/pdf" -> {
            PdfThumbnail(
                media = media,
                alpha = alpha
            )
        }

        media.mimeType.startsWith("application") -> {
            PlaceholderIcon(
                drawableRes = R.drawable.ic_unknown_file,
                alpha = alpha
            )
        }

        else -> {
            PlaceholderIcon(
                drawableRes = R.drawable.ic_unknown_file,
                alpha = alpha
            )
        }
    }
}

@Composable
private fun PlaceholderIcon(
    drawableRes: Int,
    alpha: Float
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = drawableRes),
            contentDescription = null,
            tint = colorResource(R.color.colorOnSurfaceVariant),
            modifier = Modifier
                .size(48.dp)
                .alpha(alpha)
        )
    }
}

@Composable
private fun PdfThumbnail(
    media: Media,
    alpha: Float
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val tintColor = colorResource(R.color.colorOnSurfaceVariant)
    var job by remember { mutableStateOf<Job?>(null) }
    val paddingPx = remember(density) { with(density) { 12.dp.roundToPx() } }

    AndroidView(
        factory = { ctx ->
            ImageView(ctx).apply {
                tag = media.id
                scaleType = ImageView.ScaleType.FIT_CENTER
                setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .alpha(alpha),
        update = { imageView ->
            imageView.tag = media.id
            imageView.setImageDrawable(null)
            job?.cancel()
            job = PdfThumbnailLoader.loadThumbnail(
                imageView = imageView,
                uri = media.fileUri,
                placeholderRes = R.drawable.ic_pdf,
                scope = scope,
                maxDimensionPx = 400,
                context = context,
                requestKey = media.id,
                onPlaceholder = {
                    imageView.imageTintList = ColorStateList.valueOf(tintColor.toArgb())
                }
            ) { success ->
                if (!success) {
                    imageView.imageTintList = ColorStateList.valueOf(tintColor.toArgb())
                }
            }
        }
    )

    DisposableEffect(media.id) {
        onDispose {
            job?.cancel()
        }
    }
}

@Composable
private fun MediaStatusOverlay(media: Media) {
    when (media.sStatus) {
        Media.Status.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorResource(R.color.transparent_black)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_error),
                    contentDescription = stringResource(R.string.error),
                    tint = colorResource(R.color.colorDanger),
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Media.Status.Queued,
        Media.Status.Uploading -> {
            // No overlay shown - queue is paused when screen is open
        }

        else -> Unit
    }
}

@Composable
private fun getFileInfoText(media: Media): String {
    val context = LocalContext.current

    if (media.sStatus == Media.Status.Error && media.statusMessage.isNotBlank()) {
        return media.statusMessage
    }

    val file = media.file
    return if (file.exists()) {
        Formatter.formatShortFileSize(context, file.length())
    } else {
        if (media.contentLength > 0) {
            Formatter.formatShortFileSize(context, media.contentLength)
        } else {
            media.formattedCreateDate
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun UploadManagerContentPreview() {
    val sampleMedia = listOf(
        Media(originalFilePath = "", mimeType = "image/jpeg", title = "Image 1.jpg").apply {
            status = Media.Status.Uploading.id
            uploadPercentage = 45
        },
        Media(originalFilePath = "", mimeType = "video/mp4", title = "Video 1.mp4").apply {
            status = Media.Status.Queued.id
        },
        Media(originalFilePath = "", mimeType = "application/pdf", title = "Document 1.pdf").apply {
            status = Media.Status.Error.id
            statusMessage = "Upload failed"
        }
    )

    SaveAppTheme {
        UploadManagerContent(
            state = UploadManagerState(mediaList = sampleMedia),
            onAction = {},
            onShowRetryDialog = { _, _ -> }
        )
    }
}
