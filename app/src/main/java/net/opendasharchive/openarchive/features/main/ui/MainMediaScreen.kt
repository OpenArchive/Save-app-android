package net.opendasharchive.openarchive.features.main.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.domain.Archive
import net.opendasharchive.openarchive.core.domain.Vault
import net.opendasharchive.openarchive.core.domain.Evidence
import net.opendasharchive.openarchive.core.domain.EvidenceStatus
import net.opendasharchive.openarchive.core.presentation.media.MediaStatusOverlay
import net.opendasharchive.openarchive.core.presentation.media.MediaThumbnail
import net.opendasharchive.openarchive.core.presentation.theme.MontserratFontFamily
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import java.text.NumberFormat
import java.util.Locale
import kotlinx.datetime.LocalDateTime
import net.opendasharchive.openarchive.util.format
import net.opendasharchive.openarchive.core.domain.VaultType
import net.opendasharchive.openarchive.core.presentation.theme.PreviewLight

/**
 * IMPROVED MainMediaScreen:
 * - Receives space and project from parent (HomeScreen)
 * - No longer needs homeState
 * - Cleaner data flow
 */
@Composable
fun MainMediaScreen(
    viewModel: MainMediaViewModel,
    refreshProjectId: Long?,
    refreshToken: Long,
    onNavigateToPreview: () -> Unit,
    onShowUploadManager: () -> Unit,
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(refreshToken, uiState.projectId, refreshProjectId) {
        val projectId = uiState.projectId ?: return@LaunchedEffect
        if (refreshProjectId == projectId) {
            viewModel.onAction(MainMediaAction.Refresh(projectId))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is MainMediaEvent.NavigateToPreview -> {
                    onNavigateToPreview()
                }

                is MainMediaEvent.ShowUploadManager -> {
                    onShowUploadManager()
                }
                is MainMediaEvent.SelectionModeChanged -> Unit
                MainMediaEvent.FocusFolderNameInput -> Unit
            }
        }
    }

    MainMediaContent(
        state = uiState,
        onAction = viewModel::onAction,
    )
}

/**
 * IMPROVED MainMediaContent:
 * - Reads space and project from state
 * - Archive/delete handled via ViewModel methods that emit events
 * - No direct Sugar ORM calls
 */
@Composable
fun MainMediaContent(
    state: MainMediaState,
    onAction: (MainMediaAction) -> Unit,
) {


    LaunchedEffect(state.folderBarMode) {
        if (state.folderBarMode != FolderBarMode.INFO) {
            onAction(MainMediaAction.ShowHideFolderOptionsPopup(false))
        }
    }


    val folderBarState = remember(
        state.folderBarMode,
        state.totalMediaCount,
        state.selectedMediaIds,
        state.showFolderOptionsPopup,
        state.currentProject,
        state.currentSpace
    ) {
        FolderBarState(
            mode = state.folderBarMode,
            spaceType = state.currentSpace?.type,
            projectName = state.currentProject?.description,
            totalMediaCount = state.totalMediaCount,
            selectedCount = state.selectedMediaIds.size,
            showOptionsPopup = state.showFolderOptionsPopup,
            canShowOptions = state.currentProject != null
        )
    }

    fun handleFolderIntent(intent: FolderBarIntent) {
        when (intent) {
            OptionsOpened -> onAction(MainMediaAction.ShowHideFolderOptionsPopup(true))
            OptionsDismissed -> onAction(MainMediaAction.ShowHideFolderOptionsPopup(false))

            SelectMedia -> onAction(MainMediaAction.EnterSelectionMode)
            RenameFolder -> onAction(MainMediaAction.EditFolderClicked)
            ToggleArchive -> onAction(MainMediaAction.OnArchiveProject)

            RemoveFolder -> onAction(MainMediaAction.ShowRemoveProjectDialog)

            CancelSelection -> onAction(MainMediaAction.CancelSelection)
            DeleteSelectedMediaRequest -> onAction(MainMediaAction.ShowDeleteSelectedMediaDialog)

            CancelEdit -> onAction(MainMediaAction.CancelEditMode)
            is SaveName -> onAction(MainMediaAction.SaveFolderName(intent.name))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Folder Bar
        FolderBar(
            state = folderBarState,
            onIntent = ::handleFolderIntent,
        )

        // Main Content
        Box(modifier = Modifier.fillMaxSize()) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.sections.isEmpty()) {
                EmptyStateView(
                    title = stringResource(R.string.title_welcome),
                    showWelcome = state.currentSpace == null,
                    message = when {
                        state.currentSpace == null -> stringResource(R.string.tap_to_add_server)
                        state.currentProject == null -> stringResource(R.string.tap_to_add_folder)
                        else -> stringResource(R.string.tap_to_add)
                    },
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    state.sections.forEach { section ->
                        item(key = "header_${section.collection.id}") {
                            CollectionHeaderView(section)
                        }

                        item(key = "grid_${section.collection.id}") {
                            CollectionSectionView(
                                section = section,
                                isInSelectionMode = state.isInSelectionMode,
                                selectedMediaIds = state.selectedMediaIds,
                                onMediaClick = { media ->
                                    onAction(MainMediaAction.MediaClicked(media))
                                },
                                onMediaLongPress = { media ->
                                    onAction(MainMediaAction.MediaLongPressed(media))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionHeaderView(section: CollectionSection) {
    val uploadingCount = section.media.count { it.isUploading }
    val uploadedCount = section.media.count { it.status == EvidenceStatus.UPLOADED }
    val totalCount = section.media.size

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Upload date or "Uploading"
        Text(
            text = if (uploadingCount > 0) {
                stringResource(R.string.uploading)
            } else {
                section.collection.uploadDate?.let { formatUploadDate(it) }
                    ?: "Ready to upload"
            },
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = MontserratFontFamily)
        )

        // Right: Count in pill shape
        Box(
            modifier = Modifier
                .background(colorResource(R.color.colorPillTransparent), RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (uploadingCount > 0) "$uploadedCount/$totalCount"
                else NumberFormat.getInstance().format(totalCount),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun CollectionSectionView(
    section: CollectionSection,
    isInSelectionMode: Boolean,
    selectedMediaIds: Set<Long>,
    onMediaClick: (Evidence) -> Unit,
    onMediaLongPress: (Evidence) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // 3-column grid (not 4!)
        val rows = section.media.chunked(3)
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                rowItems.forEach { evidence ->
                    MediaGridItem(
                        evidence = evidence,
                        isInSelectionMode = isInSelectionMode,
                        isSelected = selectedMediaIds.contains(evidence.id),
                        onClick = { onMediaClick(evidence) },
                        onLongClick = { onMediaLongPress(evidence) },
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                    )
                }
                if (rowItems.size < 3) {
                    repeat(3 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaGridItem(
    evidence: Evidence,
    isInSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val thumbnailAlpha = if (evidence.status == EvidenceStatus.UPLOADED) 1f else 0.5f

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        // Use shared MediaThumbnail component
        MediaThumbnail(
            evidence = evidence,
            isSelected = isInSelectionMode && isSelected,
            alpha = thumbnailAlpha,
            placeholderPadding = 28.dp,
            pdfMaxDimensionPx = 512,
            showStatusOverlay = false
        )

        // Selection border and background overlay (goes on top of thumbnail)
        if (isInSelectionMode && isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 2.dp,
                        color = colorResource(R.color.c23_teal),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .background(
                        color = Color(0x4D00B4A6),
                        shape = RoundedCornerShape(4.dp)
                    )
            )
        }

        // Use shared MediaStatusOverlay component (goes on top of everything)
        MediaStatusOverlay(
            evidence = evidence,
            modifier = Modifier.fillMaxSize(),
            showProgressText = false,
            backgroundColor = colorResource(R.color.transparent_black),
            progressIndicatorSize = 32,
            showQueuedState = true,
            showUploadingState = true
        )
    }
}

@Composable
private fun EmptyStateView(
    showWelcome: Boolean,
    title: String,
    message: String,
) {

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Welcome title (only for no space state)
            if (showWelcome) {
                Spacer(modifier = Modifier.height(120.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.displayLarge,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(R.color.colorOnSurface),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Spacer(modifier = Modifier.height(136.dp))
            }

            // Description text
            Text(
                text = message,
                style = MaterialTheme.typography.headlineSmall,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(R.color.c23_medium_grey),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 50.dp)
            )

            // Arrow pointing to add button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Image(
                    painter = painterResource(R.drawable.welcome_arrow),
                    contentDescription = stringResource(R.string.title_welcome),
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(2f)
                        .padding(horizontal = 48.dp, vertical = 8.dp),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(colorResource(R.color.c23_medium_grey))
                )
            }
        }
    }
}

private fun formatUploadDate(dateTime: LocalDateTime): String {
    val formatted = dateTime.format("MMM dd, yyyy | h:mma")
    return formatted.replace("AM", "am").replace("PM", "pm")
}

@PreviewLight
@Composable
private fun MainMediaScreenPreview() {
    SaveAppTheme {
        MainMediaContent(
            state = MainMediaState(
                currentSpace = Vault(id = 1, name = "My Vault", type = VaultType.PRIVATE_SERVER),
                currentProject = Archive(id = 1, description = "My Project", vaultId = 1),
                folderBarMode = FolderBarMode.INFO,
                totalMediaCount = 24
            ),
            onAction = {},
        )
    }
}

@PreviewLight
@Composable
private fun MainMediaScreenNoFolderPreview() {
    SaveAppTheme {
        MainMediaContent(
            state = MainMediaState(
                currentSpace = Vault(id = 1, name = "My Vault", type = VaultType.PRIVATE_SERVER),
                folderBarMode = FolderBarMode.INFO,
                totalMediaCount = 24
            ),
            onAction = {},
        )
    }
}

@PreviewLight
@Composable
private fun MainMediaScreenNoServerPreview() {
    SaveAppTheme {
        MainMediaContent(
            state = MainMediaState(),
            onAction = {},
        )
    }
}
