package net.opendasharchive.openarchive.features.main.ui

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.media.MediaStatusOverlay
import net.opendasharchive.openarchive.core.presentation.media.MediaThumbnail
import net.opendasharchive.openarchive.core.presentation.theme.MontserratFontFamily
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.features.main.ui.components.SpaceIcon
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.widget.Toast
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.db.Space
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun MainMediaScreen(
    viewModel: MainMediaViewModel,
    homeState: HomeState,
    projectId: Long,
    onNavigateToPreview: (Long) -> Unit,
) {

    val mediaState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(projectId) {
        viewModel.onAction(MainMediaAction.LoadProject(projectId))
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is MainMediaEvent.NavigateToPreview -> onNavigateToPreview(event.projectId)
                is MainMediaEvent.ShowUploadManager -> Unit
                is MainMediaEvent.ShowErrorDialog -> Unit
                is MainMediaEvent.SelectionModeChanged -> Unit
                is MainMediaEvent.ShowFolderOptionsPopup -> Unit
                MainMediaEvent.ShowDeleteConfirmation -> Unit
                MainMediaEvent.FocusFolderNameInput -> Unit
            }
        }
    }

    MainMediaContent(
        state = mediaState,
        homeState = homeState,
        onAction = viewModel::onAction,
    )
}

@Composable
private fun MainMediaContent(
    state: MainMediaState,
    homeState: HomeState,
    onAction: (MainMediaAction) -> Unit,
) {
    val context = LocalContext.current
    var showFolderOptions by remember { mutableStateOf(false) }
    var showRemoveConfirm by remember { mutableStateOf(false) }

    val space: Space? = homeState.currentSpace
    val project: Project? = homeState.projects.firstOrNull { it.id == state.projectId }

    val toggleArchive: () -> Unit = {
        project?.let {
            it.isArchived = !it.isArchived
            it.save()
            Toast.makeText(
                context,
                R.string.folder_archived,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val removeFolder: () -> Unit = {
        project?.let {
            it.delete()
            Toast.makeText(context, R.string.folder_removed, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(state.folderBarMode) {
        if (state.folderBarMode != FolderBarMode.INFO) {
            showFolderOptions = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Folder Bar
        FolderBar(
            space = space,
            project = project,
            totalMediaCount = state.totalMediaCount,
            folderBarMode = state.folderBarMode,
            onAction = onAction,
            onSelectMedia = { onAction(MainMediaAction.EnterSelectionMode) },
            onRename = { onAction(MainMediaAction.EditFolderClicked) },
            onToggleArchive = toggleArchive,
            onRemove = { showFolderOptions = false; showRemoveConfirm = true },
            onOpenOptions = { showFolderOptions = true },
            onDismissOptions = { showFolderOptions = false },
            showMenu = showFolderOptions,
            selectedMediaCount = state.selectedMediaIds.size
        )

        if (showRemoveConfirm) {
            AlertDialog(
                onDismissRequest = { showRemoveConfirm = false },
                title = { Text(text = stringResource(id = R.string.remove_from_app)) },
                text = { Text(text = stringResource(id = R.string.action_remove_project)) },
                confirmButton = {
                    Text(
                        text = stringResource(id = R.string.lbl_remove),
                        color = colorResource(R.color.colorError),
                        modifier = Modifier
                            .padding(8.dp)
                            .clickable {
                                showRemoveConfirm = false
                                removeFolder()
                            }
                    )
                },
                dismissButton = {
                    Text(
                        text = stringResource(id = R.string.lbl_Cancel),
                        modifier = Modifier
                            .padding(8.dp)
                            .clickable { showRemoveConfirm = false }
                    )
                }
            )
        }

        // Main Content
        Box(modifier = Modifier.fillMaxSize()) {
            if (state.sections.isEmpty()) {
                EmptyStateView(
                    title = stringResource(R.string.title_welcome),
                    showWelcome = space == null,
                    message = when {
                        space == null -> stringResource(R.string.tap_to_add_server)
                        project == null -> stringResource(R.string.tap_to_add_folder)
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
    val uploadedCount = section.media.count { it.sStatus == Media.Status.Uploaded }
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
    onMediaClick: (Media) -> Unit,
    onMediaLongPress: (Media) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        // 3-column grid (not 4!)
        val rows = section.media.chunked(3)
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                rowItems.forEach { media ->
                    MediaGridItem(
                        media = media,
                        isInSelectionMode = isInSelectionMode,
                        isSelected = selectedMediaIds.contains(media.id),
                        onClick = { onMediaClick(media) },
                        onLongClick = { onMediaLongPress(media) },
                        modifier = Modifier.weight(1f).aspectRatio(1f)
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
    media: Media,
    isInSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val thumbnailAlpha = if (media.sStatus == Media.Status.Uploaded) 1f else 0.5f

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        // Use shared MediaThumbnail component
        MediaThumbnail(
            media = media,
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
            media = media,
            modifier = Modifier.fillMaxSize(),
            showProgressText = false,
            backgroundColor = colorResource(R.color.transparent_black),
            progressIndicatorSize = 42,
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
            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
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

// Folder Bar Composables
@Composable
private fun FolderBar(
    space: Space?,
    project: Project?,
    totalMediaCount: Int,
    selectedMediaCount: Int,
    folderBarMode: FolderBarMode,
    onAction: (MainMediaAction) -> Unit,
    onSelectMedia: () -> Unit,
    onRename: () -> Unit,
    onToggleArchive: () -> Unit,
    onRemove: () -> Unit,
    onOpenOptions: () -> Unit,
    onDismissOptions: () -> Unit,
    showMenu: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (folderBarMode) {
            FolderBarMode.INFO -> FolderBarInfoMode(
                space = space,
                project = project,
                totalMediaCount = totalMediaCount,
                onSelectMedia = onSelectMedia,
                onRename = onRename,
                onToggleArchive = onToggleArchive,
                onRemove = onRemove,
                onOpenOptions = onOpenOptions,
                onDismissOptions = onDismissOptions,
                showMenu = showMenu
            )
            FolderBarMode.SELECTION -> FolderBarSelectionMode(
                selectedCount = selectedMediaCount,
                onAction = onAction
            )
            FolderBarMode.EDIT -> FolderBarEditMode(
                initialName = project?.description ?: "",
                onAction = onAction
            )
        }
    }
}

@Composable
private fun RowScope.FolderBarInfoMode(
    space: Space?,
    project: Project?,
    totalMediaCount: Int,
    onSelectMedia: () -> Unit,
    onRename: () -> Unit,
    onToggleArchive: () -> Unit,
    onRemove: () -> Unit,
    onOpenOptions: () -> Unit,
    onDismissOptions: () -> Unit,
    showMenu: Boolean
) {
    val hasProject = project != null
    Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Space Icon
        space?.let {

            SpaceIcon(
                type = it.tType,
                modifier = Modifier.size(28.dp)
            )

            // Arrow
            Icon(
                painter = painterResource(R.drawable.keyboard_arrow_right),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = colorResource(R.color.colorOnBackground)
            )

            // Folder Name
            Text(
                text = project?.description ?: "",
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = MontserratFontFamily),
                modifier = Modifier.weight(1f, fill = false)
            )
        }


    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (hasProject) {
            // Edit Button
            IconButton(
                onClick = { onOpenOptions() },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_edit_folder),
                    contentDescription = stringResource(R.string.edit),
                    tint = colorResource(R.color.colorTertiary),
                    modifier = Modifier.size(24.dp)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = onDismissOptions
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(id = R.string.lbl_select_media)) },
                    onClick = {
                        onDismissOptions()
                        onSelectMedia()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(id = R.string.lbl_rename_folder)) },
                    onClick = {
                        onDismissOptions()
                        onRename()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(id = R.string.popup_archive_folder)) },
                    onClick = {
                        onDismissOptions()
                        onToggleArchive()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(id = R.string.popup_remove_folder)) },
                    onClick = {
                        onDismissOptions()
                        onRemove()
                    }
                )
            }

            // Count Pill
            Box(
                modifier = Modifier
                    .background(
                        colorResource(R.color.colorPillTransparent),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = NumberFormat.getInstance().format(totalMediaCount),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun RowScope.FolderBarSelectionMode(
    selectedCount: Int,
    onAction: (MainMediaAction) -> Unit
) {
    Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Close Button
        IconButton(
            onClick = { onAction(MainMediaAction.CancelSelection) },
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.action_cancel),
                tint = colorResource(R.color.colorOnBackground)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // "Select Media" Text
        Text(
            text = stringResource(R.string.lbl_select_media),
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = MontserratFontFamily)
        )
    }

    // Remove Button (only show if items selected)
    if (selectedCount > 0) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onAction(MainMediaAction.DeleteSelected) }
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_trash),
                contentDescription = null,
                tint = colorResource(R.color.colorError),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.lbl_remove),
                color = colorResource(R.color.colorError),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun FolderBarEditMode(
    initialName: String,
    onAction: (MainMediaAction) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    var folderName by remember { mutableStateOf(initialName) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        // Show keyboard
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Close Button
        IconButton(
            onClick = {
                focusManager.clearFocus()
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(null, 0)
                onAction(MainMediaAction.CancelEditMode)
            },
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.action_cancel),
                tint = colorResource(R.color.colorOnBackground)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Text Input
        OutlinedTextField(
            value = folderName,
            onValueChange = { folderName = it },
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            textStyle = MaterialTheme.typography.titleMedium.copy(fontFamily = MontserratFontFamily),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(null, 0)
                    onAction(MainMediaAction.SaveFolderName(folderName))
                }
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent
            )
        )
    }
}

private fun formatUploadDate(date: Date): String {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy | h:mma", Locale.ENGLISH)
    val formatted = dateFormat.format(date)
    return formatted.replace("AM", "am").replace("PM", "pm")
}

@Preview(showBackground = true)
@Composable
private fun MainMediaScreenPreview() {
    SaveAppTheme {
        MainMediaContent(
            state = MainMediaState(),
            homeState = HomeState(),
            onAction = {},
        )
    }
}
