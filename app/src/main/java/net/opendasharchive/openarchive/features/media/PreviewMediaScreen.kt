package net.opendasharchive.openarchive.features.media

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.flow.collectLatest
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.media.MediaStatusOverlay
import net.opendasharchive.openarchive.core.presentation.media.MediaThumbnail
import net.opendasharchive.openarchive.core.presentation.theme.MontserratFontFamily
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.core.presentation.theme.ThemeDimensions
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.features.core.ToolbarConfigurable
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.asUiImage
import net.opendasharchive.openarchive.features.core.asUiText
import net.opendasharchive.openarchive.features.core.dialog.DialogType
import net.opendasharchive.openarchive.features.core.dialog.showDialog
import net.opendasharchive.openarchive.features.media.camera.CameraConfig
import net.opendasharchive.openarchive.features.settings.passcode.AppConfig
import net.opendasharchive.openarchive.util.PermissionManager
import net.opendasharchive.openarchive.util.Prefs
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.androidx.compose.koinViewModel

class PreviewMediaFragment : BaseFragment(), ToolbarConfigurable {

    private val args: PreviewMediaFragmentArgs by navArgs()
    private val appConfig by inject<AppConfig>()
    private val previewViewModel: PreviewMediaViewModel by viewModel()

    private lateinit var permissionManager: PermissionManager
    private lateinit var mediaLaunchers: MediaLaunchers
    private var project: Project? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val activity = requireActivity() as BaseActivity
        permissionManager = PermissionManager(activity, dialogManager)
        project = Project.getById(args.projectId)

        val composeView = ComposeView(requireContext()).apply {
            setContent {
                SaveAppTheme {
                    PreviewMediaScreen(
                        viewModel = previewViewModel,
                        onNavigateToReview = { media, selected, batchMode ->
                            ReviewActivity.launchReviewScreen(requireContext(), media, selected, batchMode)
                        },
                        onRequestAddMore = { launchAddMore() },
                        onPickMedia = { handleMediaPick(it) },
                        onShowBatchHint = { showFirstTimeBatch() },
                        onCloseScreen = {
                            // Finish the activity to return to MainActivity
                            // This is the correct way to navigate between activities
                            requireActivity().finish()
                        }
                    )
                }
            }
        }

        addMenuProvider()

        mediaLaunchers = Picker.register(
            activity = activity,
            root = composeView,
            project = { project },
            completed = {
                previewViewModel.onAction(PreviewMediaAction.Refresh)
            }
        )

        return composeView
    }

    override fun onResume() {
        super.onResume()
        previewViewModel.onAction(PreviewMediaAction.Refresh)
    }

    override fun getToolbarTitle(): String = getString(R.string.preview_media)

    override fun shouldShowBackButton(): Boolean = true

    private fun addMenuProvider() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_preview, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.menu_upload -> {
                        uploadMedia()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun launchAddMore() {
        permissionManager.checkMediaPermissions {
            Picker.pickMedia(mediaLaunchers.galleryLauncher)
        }
    }

    private fun handleMediaPick(action: AddMediaType) {
        when (action) {
            AddMediaType.CAMERA -> {
                if (appConfig.useCustomCamera) {
                    val cameraConfig = CameraConfig(
                        allowVideoCapture = true,
                        allowPhotoCapture = true,
                        allowMultipleCapture = true,
                        enablePreview = true,
                        showFlashToggle = true,
                        showGridToggle = true,
                        showCameraSwitch = true
                    )
                    Picker.launchCustomCamera(
                        requireActivity(),
                        mediaLaunchers.customCameraLauncher,
                        cameraConfig
                    )
                } else {
                    permissionManager.checkCameraPermission {
                        Picker.takePhotoModern(
                            activity = requireActivity(),
                            launcher = mediaLaunchers.modernCameraLauncher
                        )
                    }
                }
            }

            AddMediaType.FILES -> Picker.pickFiles(mediaLaunchers.filePickerLauncher)
            AddMediaType.GALLERY -> launchAddMore()
        }
    }

    private fun showFirstTimeBatch() {
        dialogManager.showDialog(dialogManager.requireResourceProvider()) {
            icon = R.drawable.ic_media_new.asUiImage()
            iconColor = dialogManager.requireResourceProvider().getColor(R.color.colorTertiary)
            title = R.string.edit_multiple.asUiText()
            message = R.string.press_and_hold_to_select_and_edit_multiple_media.asUiText()
            positiveButton {
                text = UiText.StringResource(R.string.lbl_got_it)
                action = {
                    dialogManager.dismissDialog()
                }
            }
        }

        Prefs.batchHintShown = true
    }

    private fun uploadMedia() {
        val queue: () -> Unit = {
            previewViewModel.onAction(PreviewMediaAction.UploadAll)
        }

        if (Prefs.dontShowUploadHint) {
            queue()
            return
        }

        var doNotShowAgain = false

        dialogManager.showDialog(dialogManager.requireResourceProvider()) {
            type = DialogType.Warning
            iconColor = dialogManager.requireResourceProvider().getColor(R.color.colorTertiary)
            message = R.string.once_uploaded_you_will_not_be_able_to_edit_media.asUiText()
            showCheckbox = true
            checkboxText = UiText.StringResource(R.string.do_not_show_me_this_again)
            onCheckboxChanged = { isChecked ->
                doNotShowAgain = isChecked
            }
            positiveButton {
                text = UiText.DynamicString("Proceed to upload")
                action = {
                    Prefs.dontShowUploadHint = doNotShowAgain
                    queue()
                }
            }
            neutralButton {
                text = UiText.DynamicString("Actually, let me edit")
            }
        }
    }
}

@Composable
fun PreviewMediaScreen(
    viewModel: PreviewMediaViewModel = koinViewModel(),
    onNavigateToReview: (List<Media>, Media?, Boolean) -> Unit,
    onRequestAddMore: () -> Unit,
    onPickMedia: (AddMediaType) -> Unit,
    onShowBatchHint: () -> Unit,
    onCloseScreen: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showPicker by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is PreviewMediaEvent.NavigateToReview -> onNavigateToReview(
                    event.media,
                    event.selected,
                    event.batchMode
                )

                PreviewMediaEvent.ShowBatchHint -> onShowBatchHint()
                PreviewMediaEvent.RequestAddMore -> onRequestAddMore()
                PreviewMediaEvent.RequestAddMenu -> showPicker = true
                PreviewMediaEvent.CloseScreen -> onCloseScreen()
            }
        }
    }

    PreviewMediaContent(
        state = state,
        onAction = viewModel::onAction,
    )

    if (showPicker) {
        ContentPickerSheet(
            onDismiss = { showPicker = false },
            onMediaPicked = onPickMedia
        )
    }
}

@Composable
private fun PreviewMediaContent(
    state: PreviewMediaState,
    onAction: (PreviewMediaAction) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val bottomBarHeight = 84.dp

        LazyVerticalGrid(
            modifier = Modifier
                .fillMaxSize(),
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            contentPadding = PaddingValues(
                start = 0.dp,
                end = 0.dp,
                top = 0.dp,
                bottom = bottomBarHeight + WindowInsets.navigationBars
                    .only(WindowInsetsSides.Bottom)
                    .asPaddingValues()
                    .calculateBottomPadding()
            )
        ) {
            items(state.mediaList, key = { it.id ?: it.hashCode().toLong() }) { media ->
                MediaListItem(
                    media = media,
                    isInSelectionMode = state.isInSelectionMode,
                    isSelected = state.selectedIds.contains(media.id),
                    onClick = { onAction(PreviewMediaAction.MediaClicked(media.id)) },
                    onLongPress = { onAction(PreviewMediaAction.MediaLongPressed(media.id)) }
                )
            }
        }

        if (!state.isInSelectionMode && state.showAddMore) {
            AddMoreBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
                onAddMore = { onAction(PreviewMediaAction.AddMore) },
                onAddMenu = {
                    onAction(PreviewMediaAction.ShowAddMenu)
                }
            )
        }

        if (state.isInSelectionMode) {
            SelectionBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
                selectionCount = state.selectionCount,
                totalCount = state.mediaList.size,
                onBatchEdit = { onAction(PreviewMediaAction.BatchEdit) },
                onToggleSelectAll = { onAction(PreviewMediaAction.ToggleSelectAll) },
                onDelete = { onAction(PreviewMediaAction.RemoveSelected) }
            )
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewMediaContentPreview() {
    val sampleMedia = listOf(
        Media(originalFilePath = "", mimeType = "image/jpeg", title = "Image 1"),
        Media(originalFilePath = "", mimeType = "video/mp4", title = "Video 1"),
        Media(originalFilePath = "", mimeType = "application/pdf", title = "Doc 1"),
        Media(originalFilePath = "", mimeType = "audio/mp3", title = "Audio 1")
    )
    SaveAppTheme {
        PreviewMediaContent(
            state = PreviewMediaState(
                mediaList = sampleMedia,
                selectionCount = 0,
                showAddMore = true,
                selectedIds = emptySet()
            ),
            onAction = {},
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewMediaContentSelectionPreview() {
    val sampleMedia = listOf(
        Media(originalFilePath = "", mimeType = "image/jpeg", title = "Image 1"),
        Media(originalFilePath = "", mimeType = "video/mp4", title = "Video 1"),
        Media(originalFilePath = "", mimeType = "application/pdf", title = "Doc 1"),
        Media(originalFilePath = "", mimeType = "audio/mp3", title = "Audio 1")
    )
    SaveAppTheme {
        PreviewMediaContent(
            state = PreviewMediaState(
                mediaList = sampleMedia,
                selectionCount = 2,
                showAddMore = true,
                selectedIds = setOf(1, 2)
            ),
            onAction = {},
        )
    }
}

@Composable
private fun AddMoreBar(
    modifier: Modifier = Modifier,
    onAddMore: () -> Unit,
    onAddMenu: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {

        AddMoreButton(
            onClick = onAddMore,
            onLongClick = onAddMenu
        )
    }
}

@Composable
private fun AddMoreButton(
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .heightIn(min = ThemeDimensions.touchable)
            .background(
                color = MaterialTheme.colorScheme.tertiary,
                shape = RoundedCornerShape(8.dp) // or a fixed dp
            )
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            )
            .combinedClickable(
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_add_24),
                contentDescription = null,
                tint = colorResource(R.color.black),
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = stringResource(R.string.add_more),
                style = MaterialTheme.typography.titleLarge,
                color = colorResource(R.color.black)
            )
        }
    }
}

@Composable
private fun SelectionBar(
    modifier: Modifier = Modifier,
    selectionCount: Int,
    totalCount: Int,
    onBatchEdit: () -> Unit,
    onToggleSelectAll: () -> Unit,
    onDelete: () -> Unit
) {
    val selectAllText = if (totalCount > 1 && selectionCount == totalCount) {
        stringResource(R.string.deselect_all)
    } else {
        stringResource(R.string.select_all)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        SelectionButton(
            iconRes = R.drawable.ic_batchedit,
            contentDescription = stringResource(R.string.edit_multiple),
            onClick = onBatchEdit
        )

        SelectionTextButton(
            text = selectAllText,
            onClick = onToggleSelectAll
        )

        SelectionButton(
            iconRes = R.drawable.ic_delete,
            contentDescription = stringResource(R.string.menu_delete),
            onClick = onDelete
        )
    }
}

@Composable
private fun SelectionButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit
) {
    val horizontalPadding = dimensionResource(R.dimen.selection_button_icon_padding_horizontal)
    val verticalPadding = dimensionResource(R.dimen.selection_button_padding_vertical)
    Button(
        onClick = onClick,
        modifier = Modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = colorResource(R.color.selection_button_glass),
            contentColor = colorResource(R.color.colorTertiary)
        ),
        shape = RoundedCornerShape(50),
        border = BorderStroke(
            width = dimensionResource(R.dimen.selection_button_stroke_width),
            color = colorResource(R.color.selection_button_stroke)
        ),
        contentPadding = PaddingValues(
            horizontal = verticalPadding,
            vertical = verticalPadding
        )
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            tint = colorResource(R.color.colorTertiary)
        )
    }
}

@Composable
private fun SelectionTextButton(
    text: String,
    onClick: () -> Unit
) {
    val horizontalPadding = dimensionResource(R.dimen.selection_button_text_padding_horizontal)
    val verticalPadding = dimensionResource(R.dimen.selection_button_padding_vertical)
    Button(
        onClick = onClick,
        modifier = Modifier
            .heightIn(min = ThemeDimensions.touchable)
            .padding(horizontal = 4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colorResource(R.color.selection_button_glass),
            contentColor = colorResource(R.color.colorTertiary)
        ),
        shape = RoundedCornerShape(dimensionResource(R.dimen.selection_button_corner_radius)),
        border = BorderStroke(
            width = dimensionResource(R.dimen.selection_button_stroke_width),
            color = colorResource(R.color.selection_button_stroke)
        ),
        contentPadding = PaddingValues(
            horizontal = horizontalPadding,
            vertical = verticalPadding
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = MontserratFontFamily,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaListItem(
    media: Media,
    isInSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    var showTitle by remember(media.id) { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(3.dp)
            .background(MaterialTheme.colorScheme.background)
            .border(
                width = if (isInSelectionMode && isSelected) 2.dp else 0.dp,
                color = if (isInSelectionMode && isSelected) colorResource(R.color.c23_teal) else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            )
            .background(
                color = if (isInSelectionMode && isSelected) Color(0x4D00B4A6) else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            )
            .pointerInput(isInSelectionMode, isSelected) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongPress() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        MediaThumbnail(
            media = media,
            isSelected = isInSelectionMode && isSelected,
            alpha = if (isInSelectionMode && isSelected) 0.5f else 1f,
            placeholderPadding = 24.dp,
            pdfMaxDimensionPx = 512,
            showStatusOverlay = false,
            onTitleVisibilityChanged = { showTitle = it }
        )

        if (media.mimeType.startsWith("video")) {
            Icon(
                painter = painterResource(id = R.drawable.ic_videocam_black_24dp),
                contentDescription = stringResource(R.string.is_video),
                tint = colorResource(R.color.colorMediaOverlayIcon),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
            )
        }

        if (showTitle && media.title.isNotEmpty()) {
            Text(
                text = media.title,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(8.dp),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = MontserratFontFamily,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        MediaStatusOverlay(
            media = media,
            showProgressText = true,
            backgroundColor = colorResource(R.color.transparent_loading_overlay),
            progressIndicatorSize = 42,
            showQueuedState = true,
            showUploadingState = true
        )
    }
}