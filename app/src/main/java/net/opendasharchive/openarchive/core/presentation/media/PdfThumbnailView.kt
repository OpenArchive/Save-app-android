package net.opendasharchive.openarchive.core.presentation.media

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import net.opendasharchive.openarchive.util.PdfThumbnailLoader

/**
 * Pure Compose PDF thumbnail viewer that uses ImageBitmap instead of AndroidView.
 * This is more Compose-native and avoids view interop overhead.
 */
@Composable
fun PdfThumbnailView(
    uri: Uri,
    modifier: Modifier = Modifier,
    maxDimensionPx: Int = 400,
    placeholderRes: Int,
    contentScale: ContentScale = ContentScale.Crop,
    onPlaceholder: (() -> Unit)? = null,
    onResult: ((Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    var thumbnail by remember(uri) { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember(uri) { mutableStateOf(true) }
    var loadFailed by remember(uri) { mutableStateOf(false) }

    LaunchedEffect(uri) {
        isLoading = true
        loadFailed = false
        thumbnail = PdfThumbnailLoader.loadPdfThumbnailBitmap(context, uri, maxDimensionPx)
        isLoading = false

        if (thumbnail == null) {
            loadFailed = true
            onPlaceholder?.invoke()
            onResult?.invoke(false)
        } else {
            onResult?.invoke(true)
        }
    }

    DisposableEffect(uri) {
        onDispose {
            // Cleanup if needed
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when {
            thumbnail != null -> {
                Image(
                    bitmap = thumbnail!!,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )
            }

            isLoading -> {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier
                )
            }

            loadFailed -> {
                Image(
                    painter = painterResource(id = placeholderRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )
            }
        }
    }
}
