package net.opendasharchive.openarchive.upload

import android.content.res.Configuration
import android.text.format.Formatter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import net.opendasharchive.openarchive.core.presentation.theme.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.domain.Evidence
import net.opendasharchive.openarchive.core.domain.EvidenceStatus
import net.opendasharchive.openarchive.core.presentation.media.MediaStatusOverlay
import net.opendasharchive.openarchive.core.presentation.media.MediaThumbnail
import net.opendasharchive.openarchive.core.presentation.theme.MontserratFontFamily
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun UploadManagerScreen(
    viewModel: UploadManagerViewModel,
    onClose: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is UploadManagerEvent.Close -> onClose()
                is UploadManagerEvent.ShowRetryDialog -> {
                    // Logic moved to ViewModel action but we keep the event for compatibility/other uses if needed
                    // In this case, we prefer calling the action directly from the Screen to the ViewModel
                }
            }
        }
    }

    // Auto-dismiss when all items are deleted
    // UPDATED: Only auto-dismiss if we had items and now they are gone.
    // We'll use a local state to track if we've ever seen items.
    var hadItems by remember { mutableStateOf(false) }
    LaunchedEffect(state.mediaList) {
        if (state.mediaList.isNotEmpty()) {
            hadItems = true
        } else if (hadItems) {
            onClose()
        }
    }

    UploadManagerContent(
        state = state,
        onAction = viewModel::onAction
    )
}

@Composable
private fun UploadManagerContent(
    state: UploadManagerState,
    onAction: (UploadManagerAction) -> Unit
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
        // REMOVED custom drag handle - we'll use the one from ModalBottomSheet

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
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            itemsIndexed(
                items = state.mediaList,
                key = { _, evidence -> evidence.id }
            ) { index, evidence ->
                ReorderableItem(reorderableState, key = evidence.id) { isDragging ->
                    Column {
                        UploadEvidenceItem(
                            evidence = evidence,
                            isDragging = isDragging,
                            onDelete = {
                                if (evidence.status == EvidenceStatus.ERROR) {
                                    onAction(UploadManagerAction.RequestRetry(evidence, index))
                                } else {
                                    onAction(UploadManagerAction.DeleteItem(index))
                                }
                            },
                            modifier = Modifier
                                .draggableHandle()
                                .longPressDraggableHandle()
                        )

                        // Divider between items
                        if (index < state.mediaList.size - 1) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .height(0.5.dp)
                                    .background(colorResource(R.color.light_grey))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UploadEvidenceItem(
    evidence: Evidence,
    isDragging: Boolean,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val elevation by animateDpAsState(if (isDragging) 4.dp else 0.dp)

    val alpha = if (!isDragging) 1f else 0.7f

    Surface(
        shadowElevation = elevation,
        color = MaterialTheme.colorScheme.background
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
                    .size(50.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_trash),
                        contentDescription = stringResource(R.string.menu_delete),
                        tint = colorResource(R.color.colorDanger),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Thumbnail Container - 80dp square with 8dp padding = 64dp actual image
            Box(
                modifier = Modifier
                    .size(80.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    MediaThumbnail(
                        evidence = evidence,
                        alpha = alpha,
                        placeholderPadding = 12.dp,
                        pdfMaxDimensionPx = 400,
                        showStatusOverlay = false
                    )

                    // Overlay for status - show only Error, not Queued/Uploading (queue is paused)
                    MediaStatusOverlay(
                        evidence = evidence,
                        showProgressText = false,
                        backgroundColor = colorResource(R.color.transparent_black),
                        progressIndicatorSize = 32,
                        showQueuedState = false,
                        showUploadingState = false
                    )
                }
            }

            // Title and File Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp, end = 8.dp),
                verticalArrangement = Arrangement.Center
            ) {
                val titleText = buildString {
                    if (evidence.status == EvidenceStatus.ERROR) {
                        append(stringResource(R.string.error))
                        append(": ")
                    }
                    append(evidence.title)
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

                val fileInfoText = getFileInfoText(evidence)
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

// MediaThumbnail, PlaceholderIcon, PdfThumbnail, and MediaStatusOverlay
// have been moved to shared components in core.presentation.media package

@Composable
private fun getFileInfoText(evidence: Evidence): String {
    val context = LocalContext.current

    if (evidence.status == EvidenceStatus.ERROR && evidence.statusMessage.isNotBlank()) {
        return evidence.statusMessage
    }

    val file = evidence.file
    return if (file.exists()) {
        Formatter.formatShortFileSize(context, file.length())
    } else {
        if (evidence.contentLength > 0) {
            Formatter.formatShortFileSize(context, evidence.contentLength)
        } else {
            evidence.title // Fallback
        }
    }
}

@PreviewLightDark
@Composable
private fun UploadManagerContentPreview() {
    val sampleMedia = listOf(
        Evidence(
            originalFilePath = "",
            mimeType = "image/jpeg",
            title = "Image 1.jpg",
            status = EvidenceStatus.UPLOADING
        ).apply {
            // uploadPercentage is 0 by default in Evidence, let's keep it simple
        },
        Evidence(originalFilePath = "", mimeType = "video/mp4", title = "Video 1.mp4", status = EvidenceStatus.QUEUED),
        Evidence(
            originalFilePath = "",
            mimeType = "application/pdf",
            title = "Document 1.pdf",
            status = EvidenceStatus.ERROR,
            statusMessage = "Upload failed"
        )
    )

    SaveAppTheme {
        UploadManagerContent(
            state = UploadManagerState(mediaList = sampleMedia),
            onAction = {},
        )
    }
}
