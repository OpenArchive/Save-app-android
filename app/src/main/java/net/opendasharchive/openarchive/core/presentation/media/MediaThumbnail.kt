package net.opendasharchive.openarchive.core.presentation.media

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.request.ImageRequest
import coil3.request.error
import coil3.video.VideoFrameDecoder
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.db.Media
import java.io.File

/**
 * Unified media thumbnail component that handles all media types.
 * Can be used in both grid (PreviewMedia) and list (UploadManager) layouts.
 *
 * @param media The media item to display
 * @param modifier Modifier for the thumbnail container
 * @param isSelected Whether the item is selected (for grid view)
 * @param alpha Alpha value for the thumbnail
 * @param showStatusOverlay Whether to show upload status overlay
 * @param placeholderPadding Padding around placeholder icons
 * @param onTitleVisibilityChanged Callback for when title should be shown/hidden
 */
@Composable
fun MediaThumbnail(
    media: Media,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    alpha: Float = 1f,
    showStatusOverlay: Boolean = true,
    placeholderPadding: Dp = 24.dp,
    pdfMaxDimensionPx: Int = 400,
    onTitleVisibilityChanged: ((Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
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
                modifier = modifier
                    .fillMaxSize()
                    .alpha(alpha)
            ) {
                val state = painter.state
                if (state is AsyncImagePainter.State.Loading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                } else {
                    SubcomposeAsyncImageContent()
                }
            }
            onTitleVisibilityChanged?.invoke(false)
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
                modifier = modifier
                    .fillMaxSize()
                    .alpha(alpha)
            ) {
                val state = painter.state
                if (state is AsyncImagePainter.State.Loading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                } else {
                    SubcomposeAsyncImageContent()
                }
            }
            onTitleVisibilityChanged?.invoke(false)
        }

        media.mimeType.startsWith("video") -> {
            MediaPlaceholderIcon(
                drawableRes = R.drawable.ic_video,
                isSelected = isSelected,
                alpha = alpha,
                padding = placeholderPadding,
                modifier = modifier
            )
            onTitleVisibilityChanged?.invoke(true)
        }

        media.mimeType.startsWith("image") -> {
            MediaPlaceholderIcon(
                drawableRes = R.drawable.ic_image,
                isSelected = isSelected,
                alpha = alpha,
                padding = placeholderPadding,
                modifier = modifier
            )
            onTitleVisibilityChanged?.invoke(true)
        }

        media.mimeType == "application/pdf" -> {
            PdfThumbnailView(
                uri = media.fileUri,
                placeholderRes = R.drawable.ic_pdf,
                maxDimensionPx = pdfMaxDimensionPx,
                contentScale = ContentScale.Crop,
                modifier = modifier
                    .fillMaxSize()
                    .alpha(alpha),
                onPlaceholder = { onTitleVisibilityChanged?.invoke(true) },
                onResult = { success -> onTitleVisibilityChanged?.invoke(!success) }
            )
        }

        media.mimeType.startsWith("audio") -> {
            MediaPlaceholderIcon(
                drawableRes = R.drawable.ic_music,
                isSelected = isSelected,
                alpha = alpha,
                padding = placeholderPadding,
                modifier = modifier
            )
            onTitleVisibilityChanged?.invoke(true)
        }

        media.mimeType.startsWith("application") -> {
            MediaPlaceholderIcon(
                drawableRes = R.drawable.ic_unknown_file,
                isSelected = isSelected,
                alpha = alpha,
                padding = placeholderPadding,
                modifier = modifier
            )
            onTitleVisibilityChanged?.invoke(true)
        }

        else -> {
            MediaPlaceholderIcon(
                drawableRes = R.drawable.ic_unknown_file,
                isSelected = isSelected,
                alpha = alpha,
                padding = placeholderPadding,
                modifier = modifier
            )
            onTitleVisibilityChanged?.invoke(true)
        }
    }
}

/**
 * Shared placeholder icon component for media types without thumbnails.
 *
 * @param drawableRes Resource ID of the icon to display
 * @param modifier Modifier for the icon container
 * @param isSelected Whether the item is selected (changes tint color)
 * @param alpha Alpha value for the icon
 * @param padding Padding around the icon
 */
@Composable
fun MediaPlaceholderIcon(
    drawableRes: Int,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    alpha: Float = 1f,
    padding: Dp = 24.dp
) {
    val tint = if (isSelected) {
        colorResource(R.color.colorOnPrimaryContainer)
    } else {
        colorResource(R.color.colorOnSurfaceVariant)
    }

    Icon(
        painter = painterResource(id = drawableRes),
        contentDescription = null,
        tint = tint,
        modifier = modifier
            .fillMaxSize()
            .padding(padding)
            .alpha(alpha)
    )
}
