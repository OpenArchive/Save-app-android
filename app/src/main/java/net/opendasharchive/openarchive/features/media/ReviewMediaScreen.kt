package net.opendasharchive.openarchive.features.media

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.core.view.MenuProvider
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.error
import coil3.video.VideoFrameDecoder
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.DefaultScaffoldPreview
import net.opendasharchive.openarchive.core.presentation.theme.MontserratFontFamily
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.core.presentation.theme.ThemeDimensions
import net.opendasharchive.openarchive.core.domain.Evidence
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.features.core.ToolbarConfigurable
import net.opendasharchive.openarchive.features.core.UiColor
import net.opendasharchive.openarchive.features.core.UiImage
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.dialog.showDialog
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.CustomTextField
import net.opendasharchive.openarchive.util.Prefs
import org.koin.androidx.compose.koinViewModel

class ReviewMediaFragment : BaseFragment(), ToolbarConfigurable {

    private val args: ReviewMediaFragmentArgs by navArgs()
    private var hasUnsavedChanges = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                SaveAppTheme {
                    ReviewMediaScreen(
                        //onNavigateBack = { findNavController().popBackStack() }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addMenuProvider()
    }

    private fun addMenuProvider() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_review, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    android.R.id.home, R.id.menu_done -> {
                        findNavController().popBackStack()
                        true
                    }

                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun getToolbarTitle(): String {
        return if (args.batchMode) {
            getString(R.string.bulk_edit_media_info)
        } else {
            getString(R.string.edit_media_info)
        }
    }

    override fun shouldShowBackButton() = true
}

@Composable
fun ReviewMediaScreen(
    viewModel: ReviewMediaViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->

        }
    }

    ReviewMediaContent(
        state = state,
        onAction = viewModel::onAction
    )
}

@Composable
private fun ReviewMediaContent(
    state: ReviewMediaState,
    onAction: (ReviewMediaAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Preview Section (55% height)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.60f)
        ) {
            if (state.isBatchMode) {
                BatchPreviewSection(state = state)
            } else {
                SinglePreviewSection(state = state)
            }

            // Overlay Actions
            PreviewOverlayActions(
                state = state,
                onAction = onAction
            )
        }

        Spacer(modifier = Modifier.fillMaxHeight(0.05f))

        // Bottom Metadata Section (45% height)
        MetadataSection(
            state = state,
            onAction = onAction,
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.35f)
                .alpha(0.85f)
        )
    }
}

@Composable
private fun SinglePreviewSection(state: ReviewMediaState) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        MediaPreview(
            media = state.currentMedia,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun BatchPreviewSection(state: ReviewMediaState) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val batchMedia = state.batchPreviewMedia

        // First card (back) - smaller, at bottom left
        batchMedia.getOrNull(0)?.let { media ->
            Card(
                modifier = Modifier
                    .padding(start = 16.dp, top = 16.dp)
                    .fillMaxWidth(0.8f)
                    .fillMaxHeight(0.7f)
                    .align(Alignment.TopStart),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                MediaPreview(
                    media = media,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Second card (middle) - offset from first card
        batchMedia.getOrNull(1)?.let { media ->
            Card(
                modifier = Modifier
                    .padding(start = 48.dp, top = 48.dp)
                    .fillMaxWidth(0.85f)
                    .fillMaxHeight(0.85f)
                    .align(Alignment.TopStart),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                MediaPreview(
                    media = media,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Third card (front) - largest, offset from second card
        batchMedia.getOrNull(2)?.let { media ->
            Card(
                modifier = Modifier
                    .padding(start = 80.dp, top = 80.dp, end = 24.dp)
                    .fillMaxWidth()
                    .fillMaxWidth(0.95f)
                    .align(Alignment.TopStart),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                MediaPreview(
                    media = media,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
private fun MediaPreview(
    media: Evidence?,
    modifier: Modifier = Modifier,
    background: Color = MaterialTheme.colorScheme.background,
    contentScale: ContentScale = ContentScale.Fit
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        when {
            media == null -> {
                Icon(
                    painter = painterResource(R.drawable.no_thumbnail),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.Gray
                )
            }

            media.mimeType.startsWith("image") -> {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(media.fileUri)
                        .error(R.drawable.ic_image)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale,
                    loading = {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    },
                    error = {
                        Icon(
                            painter = painterResource(R.drawable.ic_image),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                    }
                )
            }

            media.mimeType.startsWith("video") -> {
                val videoUri = when {
                    !media.originalFilePath.isNullOrBlank() -> media.originalFilePath.toUri()
                    else -> media.fileUri
                }

                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(videoUri)
                        .decoderFactory { result, options, _ ->
                            VideoFrameDecoder(result.source, options)
                        }
                        .error(R.drawable.ic_video)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale,
                    loading = {
                        Icon(
                            painter = painterResource(R.drawable.ic_video),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                    },
                    error = {
                        Icon(
                            painter = painterResource(R.drawable.ic_video),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                    }
                )
            }

            media.mimeType.startsWith("audio") -> {
                Icon(
                    painter = painterResource(R.drawable.ic_music),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.Gray
                )
            }

            media.mimeType == "application/pdf" -> {
                // TODO: Implement PDF preview using PdfThumbnailLoader
                Icon(
                    painter = painterResource(R.drawable.ic_pdf),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.Gray
                )
            }

            else -> {
                Icon(
                    painter = painterResource(R.drawable.no_thumbnail),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun PreviewOverlayActions(
    state: ReviewMediaState,
    onAction: (ReviewMediaAction) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Top gradient overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(102.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    )
                )
                .align(Alignment.TopCenter)
        )

        // Counter badge (top left)
        Surface(
            modifier = Modifier
                .padding(start = 16.dp, top = 16.dp)
                .align(Alignment.TopStart),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        ) {
            Text(
                text = state.counter,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }

        // Flag button (top right) - matching XML size
        IconButton(
            onClick = { onAction(ReviewMediaAction.ToggleFlag) },
            modifier = Modifier
                .padding(end = 4.dp, top = 4.dp)
                .size(42.dp)
                .align(Alignment.TopEnd)
        ) {
            Icon(
                painter = painterResource(
                    if (state.isFlagged) R.drawable.ic_flag_selected
                    else R.drawable.ic_flag_unselected
                ),
                contentDescription = stringResource(
                    if (state.isFlagged) R.string.status_flagged
                    else R.string.hint_flag
                ),
                tint = if (state.isFlagged) {
                    colorResource(R.color.orange_light)
                } else {
                    Color.White
                },
                modifier = Modifier.size(24.dp)
            )
        }

        // Navigation arrows (single mode only) with semi-transparent backgrounds
        AnimatedVisibility(
            visible = state.showBackButton,
            modifier = Modifier.align(Alignment.CenterStart),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                modifier = Modifier.padding(start = 8.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
            ) {
                IconButton(
                    onClick = { onAction(ReviewMediaAction.NavigatePrevious) },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_left_ios),
                        contentDescription = stringResource(R.string.previous),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = state.showForwardButton,
            modifier = Modifier.align(Alignment.CenterEnd),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                modifier = Modifier.padding(end = 8.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
            ) {
                IconButton(
                    onClick = { onAction(ReviewMediaAction.NavigateNext) },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_right_ios),
                        contentDescription = stringResource(R.string.next),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetadataSection(
    state: ReviewMediaState,
    onAction: (ReviewMediaAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val descriptionFocusRequester = remember { FocusRequester() }

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 16.dp)
    ) {
        // Location field (single line)
        CustomTextField(
            value = state.location,
            onValueChange = { onAction(ReviewMediaAction.UpdateLocation(it)) },
            placeholder = stringResource(R.string.add_a_location_optional),
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next,
            onImeAction = {
                descriptionFocusRequester.requestFocus()
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Description field (multiline)
        MultilineTextField(
            value = state.description,
            onValueChange = { onAction(ReviewMediaAction.UpdateDescription(it)) },
            placeholder = stringResource(R.string.add_notes_optional),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .focusRequester(descriptionFocusRequester)
        )
    }
}

@Composable
private fun MultilineTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontStyle = FontStyle.Italic,
                    fontSize = 13.sp,
                    fontFamily = MontserratFontFamily
                )
            )
        },
        shape = RoundedCornerShape(ThemeDimensions.roundedCorner),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.background,
            unfocusedContainerColor = MaterialTheme.colorScheme.background,
            focusedBorderColor = MaterialTheme.colorScheme.tertiary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            cursorColor = MaterialTheme.colorScheme.tertiary
        ),
        minLines = 3,
        maxLines = 5
    )
}

// Previews
//@Preview(showBackground = true, name = "Single Media Mode")
//@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Single Media Mode Dark")
@Composable
private fun ReviewMediaSingleModePreview() {
    DefaultScaffoldPreview {
        ReviewMediaContent(
            state = ReviewMediaState(
                mediaList = listOf(Evidence(id = 1)),
                currentIndex = 0,
                isBatchMode = false,
                description = "",
                location = "",
                isFlagged = false,
                counter = "1/10",
                showBackButton = true,
                showForwardButton = true
            ),
            onAction = {}
        )
    }
}

@Preview(showBackground = true, name = "Batch Mode")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Batch Mode Dark")
@Composable
private fun ReviewMediaBatchModePreview() {
    DefaultScaffoldPreview {
        ReviewMediaContent(
            state = ReviewMediaState(
                mediaList = listOf(Evidence(id = 1), Evidence(id = 2), Evidence(id = 3)),
                currentIndex = 0,
                isBatchMode = true,
                description = "Shared description",
                location = "New York, NY",
                isFlagged = true,
                counter = "3",
                showBackButton = false,
                showForwardButton = false
            ),
            onAction = {}
        )
    }
}

@Preview(showBackground = true, name = "Single Media with Data")
@Composable
private fun ReviewMediaWithDataPreview() {
    DefaultScaffoldPreview {
        ReviewMediaContent(
            state = ReviewMediaState(
                mediaList = listOf(Evidence(id = 1)),
                currentIndex = 0,
                isBatchMode = false,
                description = "A beautiful sunset captured at the beach during my vacation.",
                location = "40.7128, -74.0060",
                isFlagged = true,
                counter = "5/10",
                showBackButton = true,
                showForwardButton = true
            ),
            onAction = {}
        )
    }
}
