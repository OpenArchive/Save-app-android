package net.opendasharchive.openarchive.features.media

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.content.res.ColorStateList
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.request.ImageRequest
import coil3.video.VideoFrameDecoder
import kotlinx.coroutines.Job
import net.opendasharchive.openarchive.R
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
import net.opendasharchive.openarchive.features.core.dialog.showDialog
import net.opendasharchive.openarchive.features.core.dialog.DialogType
import net.opendasharchive.openarchive.features.media.camera.CameraConfig
import net.opendasharchive.openarchive.features.settings.passcode.AppConfig
import net.opendasharchive.openarchive.util.PermissionManager
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.PdfThumbnailLoader
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File

class PreviewMediaFragment : BaseFragment(), ToolbarConfigurable {

    private val args: PreviewMediaFragmentArgs by navArgs()
    private val appConfig by inject<AppConfig>()
    private val previewViewModel: PreviewMediaViewModel by viewModel()

    private lateinit var permissionManager: PermissionManager
    private lateinit var mediaLaunchers: MediaLaunchers
    private var project: Project? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

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
                        onRequestAddMenu = { showAddMenu() },
                        onRequestUpload = { uploadMedia() },
                        onShowBatchHint = { showFirstTimeBatch() },
                        onCloseScreen = { findNavController().popBackStack() }
                    )
                }
            }
        }

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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_preview, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_upload -> {
                uploadMedia()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun launchAddMore() {
        permissionManager.checkMediaPermissions {
            Picker.pickMedia(mediaLaunchers.galleryLauncher)
        }
    }

    private fun showAddMenu() {
        if (!Picker.canPickFiles(requireContext())) {
            launchAddMore()
            return
        }
        val modalBottomSheet = ContentPickerFragment { action ->
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
        modalBottomSheet.show(parentFragmentManager, ContentPickerFragment.TAG)
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
private fun PreviewMediaScreen(
    viewModel: PreviewMediaViewModel,
    onNavigateToReview: (List<Media>, Media?, Boolean) -> Unit,
    onRequestAddMore: () -> Unit,
    onRequestAddMenu: () -> Unit,
    onShowBatchHint: () -> Unit,
    onCloseScreen: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PreviewMediaEvent.NavigateToReview -> onNavigateToReview(
                    event.media,
                    event.selected,
                    event.batchMode
                )

                PreviewMediaEvent.ShowBatchHint -> onShowBatchHint()
                PreviewMediaEvent.RequestAddMore -> onRequestAddMore()
                PreviewMediaEvent.RequestAddMenu -> onRequestAddMenu()
                PreviewMediaEvent.CloseScreen -> onCloseScreen()
            }
        }
    }

    PreviewMediaContent(
        state = state,
        onAction = viewModel::onAction
    )
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
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(
                start = 6.dp,
                end = 6.dp,
                top = 8.dp,
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
                onAddMenu = { onAction(PreviewMediaAction.ShowAddMenu) }
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
        Button(
            onClick = onAddMore,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = ThemeDimensions.touchable)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { onAddMenu() }
                    )
                },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = colorResource(R.color.black)
            )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_add_24),
                contentDescription = null,
                tint = colorResource(R.color.black),
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = stringResource(R.string.add_more),
                style = MaterialTheme.typography.titleLarge
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        IconButton(
            onClick = onAddMenu
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_more_vert),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground
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
            .padding(horizontal = 8.dp, vertical = 24.dp),
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
            contentDescription = stringResource(R.string.delete),
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
        modifier = Modifier.heightIn(min = ThemeDimensions.touchable),
        colors = ButtonDefaults.buttonColors(
            containerColor = colorResource(R.color.selection_button_glass),
            contentColor = colorResource(R.color.colorTertiary)
        ),
        shape = RoundedCornerShape(dimensionResource(R.dimen.selection_button_corner_radius)),
        border = androidx.compose.foundation.BorderStroke(
            width = dimensionResource(R.dimen.selection_button_stroke_width),
            color = colorResource(R.color.selection_button_stroke)
        ),
        contentPadding = PaddingValues(
            horizontal = horizontalPadding,
            vertical = verticalPadding
        )
    ) {
        Icon(
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
        modifier = Modifier.heightIn(min = ThemeDimensions.touchable),
        colors = ButtonDefaults.buttonColors(
            containerColor = colorResource(R.color.selection_button_glass),
            contentColor = colorResource(R.color.colorTertiary)
        ),
        shape = RoundedCornerShape(dimensionResource(R.dimen.selection_button_corner_radius)),
        border = androidx.compose.foundation.BorderStroke(
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
                width = if (isInSelectionMode && media.selected) 2.dp else 0.dp,
                color = if (isInSelectionMode && media.selected) colorResource(R.color.c23_teal) else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            )
            .background(
                color = if (isInSelectionMode && media.selected) Color(0x4D00B4A6) else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            )
            .pointerInput(isInSelectionMode, media.selected) {
                detectTapGestures(
                    onLongPress = { onLongPress() },
                    onTap = { onClick() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        MediaThumbnail(
            media = media,
            isSelected = isInSelectionMode && media.selected,
            onShowTitle = { showTitle = it }
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

        if (showTitle && !media.title.isNullOrEmpty()) {
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

        MediaStatusOverlay(media = media)
    }
}

@Composable
private fun MediaStatusOverlay(media: Media) {
    when (media.sStatus) {
        Media.Status.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorResource(R.color.transparent_loading_overlay)),
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

        Media.Status.Queued -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorResource(R.color.transparent_loading_overlay)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(42.dp),
                    strokeWidth = 4.dp
                )
            }
        }

        Media.Status.Uploading -> {
            val progressValue = media.uploadPercentage ?: 0
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorResource(R.color.transparent_loading_overlay)),
                contentAlignment = Alignment.Center
            ) {
                if (progressValue > 2) {
                    CircularProgressIndicator(
                        progress = { progressValue / 100f },
                        color = MaterialTheme.colorScheme.tertiary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(42.dp),
                        strokeWidth = 4.dp
                    )
                } else {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(42.dp),
                        strokeWidth = 4.dp
                    )
                }
                Text(
                    text = "$progressValue%",
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = MontserratFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                )
            }
        }

        else -> Unit
    }
}

@Composable
private fun MediaThumbnail(
    media: Media,
    isSelected: Boolean,
    onShowTitle: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val thumbnailAlpha = if (media.sStatus == Media.Status.Uploaded) 1f else 0.5f
    val imageExists = remember(media.originalFilePath) {
        runCatching { media.file.exists() }.getOrDefault(false)
    }
    val videoExists = remember(media.originalFilePath) {
        runCatching {
            val primary = media.originalFilePath.takeIf { it.isNotBlank() }?.let { File(it).exists() } ?: false
            val secondary = media.fileUri.path?.let { File(it).exists() } ?: false
            primary || secondary
        }.getOrDefault(false)
    }

    when {
        media.mimeType.startsWith("image") && imageExists -> {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(media.fileUri)
                    .error(R.drawable.ic_image)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(thumbnailAlpha)
            ) {
                val state = painter.state
                if (state is coil3.compose.AsyncImagePainter.State.Loading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    SubcomposeAsyncImageContent()
                }
            }
            onShowTitle(false)
        }

        media.mimeType.startsWith("video") && videoExists -> {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(media.originalFilePath.ifEmpty { media.fileUri })
                    .decoderFactory(VideoFrameDecoder.Factory())
                    .error(R.drawable.ic_video)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(thumbnailAlpha)
            ) {
                val state = painter.state
                if (state is coil3.compose.AsyncImagePainter.State.Loading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    SubcomposeAsyncImageContent()
                }
            }
            onShowTitle(false)
        }
        media.mimeType.startsWith("video") -> {
            PlaceholderIcon(
                drawableRes = R.drawable.ic_video,
                isSelected = isSelected,
                alpha = thumbnailAlpha
            )
            onShowTitle(true)
        }

        media.mimeType.startsWith("image") -> {
            PlaceholderIcon(
                drawableRes = R.drawable.ic_image,
                isSelected = isSelected,
                alpha = thumbnailAlpha
            )
            onShowTitle(true)
        }

        media.mimeType == "application/pdf" -> {
            PdfThumbnail(
                media = media,
                isSelected = isSelected,
                modifier = Modifier.alpha(thumbnailAlpha),
                onPlaceholder = { onShowTitle(true) },
                onResult = { success -> onShowTitle(!success) }
            )
        }

        media.mimeType.startsWith("audio") -> {
            PlaceholderIcon(
                drawableRes = R.drawable.ic_music,
                isSelected = isSelected,
                alpha = thumbnailAlpha
            )
            onShowTitle(true)
        }

        media.mimeType.startsWith("application") -> {
            PlaceholderIcon(
                drawableRes = R.drawable.ic_unknown_file,
                isSelected = isSelected,
                alpha = thumbnailAlpha
            )
            onShowTitle(true)
        }

        else -> {
            PlaceholderIcon(
                drawableRes = R.drawable.no_thumbnail,
                isSelected = isSelected,
                alpha = thumbnailAlpha
            )
            onShowTitle(true)
        }
    }
}

@Composable
private fun PlaceholderIcon(
    drawableRes: Int,
    isSelected: Boolean,
    alpha: Float
) {
    Icon(
        painter = painterResource(id = drawableRes),
        contentDescription = null,
        tint = if (isSelected) {
            colorResource(R.color.colorOnPrimaryContainer)
        } else {
            colorResource(R.color.colorOnSurfaceVariant)
        },
        modifier = Modifier
            .size(64.dp)
            .alpha(alpha)
    )
}

@Composable
private fun PdfThumbnail(
    media: Media,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onPlaceholder: () -> Unit,
    onResult: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val tintColor = if (isSelected) {
        colorResource(R.color.colorOnPrimaryContainer)
    } else {
        colorResource(R.color.colorOnSurfaceVariant)
    }
    var job by remember { mutableStateOf<Job?>(null) }
    val paddingPx = remember(density) { with(density) { 24.dp.roundToPx() } }

    AndroidView(
        factory = { ctx ->
            android.widget.ImageView(ctx).apply {
                tag = media.id
                scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
            }
        },
        modifier = modifier.fillMaxSize(),
        update = { imageView ->
            imageView.tag = media.id
            imageView.setImageDrawable(null)
            job?.cancel()
            job = PdfThumbnailLoader.loadThumbnail(
                imageView = imageView,
                uri = media.fileUri,
                placeholderRes = R.drawable.ic_pdf,
                scope = scope,
                maxDimensionPx = 512,
                context = context,
                requestKey = media.id,
                onPlaceholder = {
                    onPlaceholder()
                    imageView.imageTintList = ColorStateList.valueOf(tintColor.toArgb())
                }
            ) { success ->
                if (!success) {
                    imageView.imageTintList = ColorStateList.valueOf(tintColor.toArgb())
                }
                onResult(success)
            }
        }
    )

    DisposableEffect(media.id) {
        onDispose {
            job?.cancel()
        }
    }
}
