Nice question — these two screens are screaming “make me a shared media UI kit” 😄

Let’s group what you can reuse into a few buckets:

⸻

1. Shared Media Thumbnail Kit

You already have very similar pieces in both screens:
•	MediaThumbnail (grid/list thumbnail)
•	PlaceholderIcon
•	PdfThumbnail / PdfThumbnailCompose
•	MediaStatusOverlay

You can pull these into something like core.presentation.media and parameterize what’s different.

1.1. One MediaThumbnail for both screens

Right now:
•	PreviewMedia has MediaThumbnail(media, isSelected, onShowTitle)
•	UploadManager has MediaThumbnail(media, alpha) + a different MediaStatusOverlay.

You can converge on a single composable:

@Composable
fun MediaThumbnail(
media: Media,
modifier: Modifier = Modifier,
isSelected: Boolean = false,
alpha: Float = 1f,
showStatusOverlay: Boolean = true,
onTitleVisibilityChanged: ((Boolean) -> Unit)? = null,
)

Internally it can:
•	Decide which renderer to use (image/video/pdf/audio/other)
•	Call shared PdfThumbnailView
•	Call shared PlaceholderIcon
•	Optionally show status overlay if showStatusOverlay == true.

Then:
•	PreviewMedia: MediaThumbnail(media, isSelected = isInSelectionMode && isSelected, onTitleVisibilityChanged = { showTitle = it })
•	UploadManager: MediaThumbnail(media, alpha = alpha, showStatusOverlay = false) and place MediaStatusOverlay separately in the list item.

1.2. Single PlaceholderIcon

You already have two variants:
•	UploadManager: PlaceholderIcon(drawableRes, alpha)
•	PreviewMedia: PlaceholderIcon(drawableRes, isSelected, alpha)

Unify:

@Composable
fun MediaPlaceholderIcon(
@DrawableRes drawableRes: Int,
modifier: Modifier = Modifier,
isSelected: Boolean = false,
alpha: Float = 1f,
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
            .padding(24.dp)
            .alpha(alpha)
    )
}

Then both screens just call MediaPlaceholderIcon(...) with the right flags.

1.3. Shared PdfThumbnail wrapper

You have:
•	PdfThumbnailCompose (upload manager)
•	PdfThumbnail (preview)

Both:
•	Use AndroidView(ImageView)
•	Use PdfThumbnailLoader.loadThumbnail
•	Manage a Job with remember and DisposableEffect

Extract to:

@Composable
fun PdfThumbnailView(
mediaId: Long,
uri: Uri,
modifier: Modifier = Modifier,
paddingDp: Dp,
tintColor: Color,
maxDimensionPx: Int,
onPlaceholder: (() -> Unit)? = null,
onResult: ((Boolean) -> Unit)? = null
)

Then:
•	UploadManager calls with smaller padding & 400 px max.
•	PreviewMedia calls with 24.dp and 512 px max + selection tint.

Implementation lives in one place; both screens just configure parameters.

⸻

2. Shared Media Status Overlay

You basically have two versions of “status overlay”:
•	UploadManager: overlay for Error / Queued / Uploading, etc.
•	PreviewMedia: similar overlay with error + progress indicators.

You can unify to something like:

@Composable
fun MediaStatusOverlay(
media: Media,
modifier: Modifier = Modifier,
showProgressText: Boolean = true,
backgroundColor: Color = colorResource(R.color.transparent_loading_overlay)
)

And inside:
•	For Status.Error → red icon overlay.
•	For Status.Queued → spinner or paused indicator.
•	For Status.Uploading → determinate/indeterminate circular progress + optional % label.

Then:
•	PreviewMedia uses showProgressText = true
•	UploadManager could use showProgressText = false if it wants simpler overlay, or pass a different backgroundColor if needed.

That way, any future change to upload status UX is automatically shared.

⸻

3. Shared Selection Button / Chip Style

You already have a consistent style between XML and Compose:
•	Pill-ish rounded shape (cornerSize 50% in XML style).
•	Glassy background + stroke + tertiary content color.
•	Icon-only variant and text variant.

You can extract a generic SelectionChip set:

@Composable
fun SelectionIconButton(
@DrawableRes iconRes: Int,
contentDescription: String,
modifier: Modifier = Modifier,
onClick: () -> Unit,
) {
val horizontalPadding = dimensionResource(R.dimen.selection_button_icon_padding_horizontal)
val verticalPadding = dimensionResource(R.dimen.selection_button_padding_vertical)

    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = ThemeDimensions.touchable),
        colors = ButtonDefaults.buttonColors(
            containerColor = colorResource(R.color.selection_button_glass),
            contentColor = colorResource(R.color.colorTertiary)
        ),
        shape = RoundedCornerShape(dimensionResource(R.dimen.selection_button_corner_radius)),
        border = BorderStroke(
            width = dimensionResource(R.dimen.selection_button_stroke_width),
            color = colorResource(R.color.selection_button_stroke)
        ),
        contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = verticalPadding),
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(dimensionResource(R.dimen.selection_button_icon_size)),
            tint = colorResource(R.color.colorTertiary)
        )
    }
}

@Composable
fun SelectionTextButton(
text: String,
modifier: Modifier = Modifier,
onClick: () -> Unit,
) { /* your existing impl reused */ }

Now both:
•	SelectionBar in PreviewMedia
•	Any “selection” / “edit queue” button in UploadManager

can use the same components. If you ever tweak paddings, stroke, or icon size, you do it in one place.

⸻

4. Shared Coil / file helpers

Both screens:
•	Build ImageRequest.Builder(context).data(media.fileUri) etc.
•	Do file existence checks (File.exists()).

You can make small helpers:

4.1. Coil builder helper

@Composable
fun rememberMediaImageRequest(
data: Any,
@DrawableRes errorRes: Int,
isVideo: Boolean = false
): ImageRequest {
val context = LocalContext.current
return remember(data, errorRes, isVideo) {
ImageRequest.Builder(context)
.data(data)
.apply {
if (isVideo) {
decoderFactory(VideoFrameDecoder.Factory())
}
}
.error(errorRes)
.build()
}
}

Then:
•	PreviewMedia + UploadManager both use it for image/video thumbnails → less repeated code, easier to tune later (e.g. caching keys, size, crossfade).

4.2. File existence memo

Instead of repeating:

val imageExists = remember(media.originalFilePath) { ... File(...).exists() ... }
val videoExists = remember(media.originalFilePath) { ... File(...).exists() ... }

Extract:

@Composable
fun rememberMediaFileExists(
primaryPath: String?,
fallbackPath: String?
): Boolean {
return remember(primaryPath, fallbackPath) {
runCatching {
val primary = primaryPath?.takeIf { it.isNotBlank() }?.let { File(it).exists() } ?: false
val fallback = fallbackPath?.let { File(it).exists() } ?: false
primary || fallback
}.getOrDefault(false)
}
}

Both screens can reuse this logic for image/video existence without duplicating I/O code.

⸻

5. Shared event / state patterns (lightweight)

Less about code reuse, more about pattern reuse:
•	Both screens use:
•	uiState: StateFlow<...> + collectAsStateWithLifecycle()
•	events: Flow<PreviewMediaEvent / UploadManagerEvent> handled in LaunchedEffect(Unit).

You can keep your current pattern but codify it as a convention:
•	Every screen: Screen(viewModel = ..., onEvent = {})
•	LaunchedEffect(Unit) composable that handles events in the same way.

You don’t need to abstract this into code, but mentally treating them as the same pattern keeps behavior consistent.

⸻

Recommended package split

Something like:

core/presentation/media/
MediaThumbnail.kt
MediaStatusOverlay.kt
PdfThumbnailView.kt
MediaPlaceholderIcon.kt
MediaImageRequest.kt

core/presentation/components/
SelectionButtons.kt   // SelectionIconButton + SelectionTextButton

Then:
•	PreviewMediaFragment and ComposeUploadManagerFragment both just assemble these building blocks with their own layout & behavior.

⸻

Short answer: your current AndroidView + PdfThumbnailLoader is already pretty good. A more “Compose-native” way is to render the PDF into an ImageBitmap in a LaunchedEffect and show it with Image. For pre-generation: I’d avoid BLOBs in SQLite and, if you need the extra speed, pre-generate small thumbnail files and store their paths instead of thumbnails in the DB.

Let’s go step by step.

⸻

1. More “Compose way” PDF thumbnail in a lazy list

Idea:
•	For each item, keep a State<ImageBitmap?>.
•	Use LaunchedEffect(uri) to:
•	Check an in-memory cache (optional).
•	If missing, generate thumbnail via PdfRenderer on Dispatchers.IO.
•	Show Image(bitmap = ...) when ready; show placeholder otherwise.
•	Works nicely inside LazyColumn / LazyVerticalGrid.

1.1. Thumbnail loader (suspending, Compose-friendly)

This mirrors your PdfThumbnailLoader.renderPdfFirstPage, but returns ImageBitmap:

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.net.toFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// You can keep your LruCache here if you want (key -> ImageBitmap)
suspend fun loadPdfThumbnailBitmap(
context: Context,
uri: Uri,
maxDimensionPx: Int = 400
): ImageBitmap? = withContext(Dispatchers.IO) {
val descriptor = openDescriptor(context, uri) ?: return@withContext null

    try {
        PdfRenderer(descriptor).use { renderer ->
            if (renderer.pageCount == 0) return@withContext null

            renderer.openPage(0).use { page ->
                val scale = minOf(
                    maxDimensionPx.toFloat() / page.width,
                    maxDimensionPx.toFloat() / page.height
                ).coerceAtLeast(0.1f)

                val bitmapWidth = (page.width * scale).toInt().coerceAtLeast(1)
                val bitmapHeight = (page.height * scale).toInt().coerceAtLeast(1)

                val bitmap = Bitmap.createBitmap(
                    bitmapWidth,
                    bitmapHeight,
                    Bitmap.Config.ARGB_8888
                )

                Canvas(bitmap).apply {
                    drawColor(Color.WHITE)
                }

                val matrix = Matrix().apply { postScale(scale, scale) }

                page.render(
                    bitmap,
                    null,
                    matrix,
                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                )

                bitmap.asImageBitmap()
            }
        }
    } catch (e: Exception) {
        null
    } finally {
        descriptor.close()
    }
}

private fun openDescriptor(context: Context, uri: Uri): ParcelFileDescriptor? {
return try {
val file = runCatching { uri.toFile() }.getOrNull()
if (file != null && file.exists()) {
ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
} else {
context.contentResolver.openFileDescriptor(uri, "r")
}
} catch (e: Exception) {
null
}
}

You can plug an LruCache<String, ImageBitmap> in there just like your current loader (key = uri + lastModified + size).

1.2. Composable PDF thumbnail

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource

@Composable
fun PdfThumbnail(
uri: android.net.Uri,
modifier: Modifier = Modifier,
maxDimensionPx: Int = 400,
@androidx.annotation.DrawableRes placeholderRes: Int,
tintPlaceholder: Boolean = true
) {
val context = LocalContext.current
var thumbnail by remember(uri) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
var isLoading by remember(uri) { mutableStateOf(true) }

    LaunchedEffect(uri) {
        isLoading = true
        thumbnail = loadPdfThumbnailBitmap(context, uri, maxDimensionPx)
        isLoading = false
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
                    modifier = Modifier.fillMaxSize()
                )
            }

            isLoading -> {
                CircularProgressIndicator()
            }

            else -> {
                Image(
                    painter = painterResource(id = placeholderRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    colorFilter = if (tintPlaceholder) ColorFilter.tint(
                        androidx.compose.ui.graphics.Color.Gray
                    ) else null
                )
            }
        }
    }
}

1.3. Using it in your MediaThumbnail + LazyColumn

@Composable
fun MediaThumbnail(
media: Media,
alpha: Float,
modifier: Modifier = Modifier
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
                    PlaceholderIcon(R.drawable.ic_image, alpha)
                },
                modifier = modifier
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
                    PlaceholderIcon(R.drawable.ic_video, alpha)
                },
                modifier = modifier
                    .fillMaxSize()
                    .alpha(alpha)
            )
        }

        media.mimeType == "application/pdf" -> {
            PdfThumbnail(
                uri = media.fileUri,
                placeholderRes = R.drawable.ic_pdf,
                modifier = modifier
                    .fillMaxSize()
                    .alpha(alpha)
            )
        }

        else -> {
            PlaceholderIcon(R.drawable.ic_unknown_file, alpha)
        }
    }
}

@Composable
fun UploadList(
items: List<Media>
) {
LazyColumn {
items(items, key = { it.id }) { media ->
Box(
modifier = Modifier
.fillMaxWidth()
.height(70.dp)
) {
MediaThumbnail(media = media, alpha = 1f)
}
}
}
}

This is “pure Compose”: no AndroidView, no ImageView, and you can still reuse your PdfRenderer logic and even your LRU cache implementation.

Your existing AndroidView + PdfThumbnailLoader is not wrong though — it’s already efficient and cancellable. The Compose version mainly wins on API niceness and avoiding view interop.

⸻

2. Opinion on pre-generating thumbnails vs dynamic

You’re already:
•	Using Coil for image/video thumbnails.
•	Generating PDF thumbnails on demand via PdfThumbnailLoader with an LruCache.

You’re considering:

generate thumbnails for all media types at import time, store them (esp. PDFs) as BLOB in the Media table.

2.1. Storing thumbnails as BLOBs in SQLite (Media table)

Pros:
•	Ultra-fast reads in UI: simple query → column → display.
•	No PdfRenderer/video decode cost at scroll time.
•	Thumbnails are “atomic” with the record (transactionally).

Cons:
•	DB bloat & performance:
•	PDFs + large images → thumbnails easily 30–100 KB each; thousands of items → tens/hundreds of MB inside DB.
•	BLOBs in SQLite slow down some operations (VACUUM, backup, WAL files, encryption, migrations).
•	Harder to evolve:
•	If you change desired thumbnail size or aspect ratio, you either regenerate everything or keep legacy sizes.
•	Write cost:
•	On import, you’ll be decoding and writing thumbnails and originals. On low-end devices, importing a big batch may stutter if not careful with WorkManager / back-pressure.

For an archival app like Save, where your DB can grow and live for years, I’m not a fan of big BLOB columns.

2.2. Pre-generating thumbnails as files + storing paths

This is usually the sweet spot.

Approach:
•	On import (or in a background WorkManager job):
•	Generate small thumbnails:
•	image → downscaled JPEG/WEBP
•	video → single frame (via MediaMetadataRetriever / Coil video decoder)
•	pdf → first-page bitmap (your existing renderer)
•	Save them as files under e.g.
/data/data/.../files/thumbnails/<mediaId>.jpg
•	Add columns to Media:
•	thumbnailPath: String?
•	maybe thumbnailType/width/height if you care.
•	In Compose:
•	If thumbnailPath != null, just load that via Coil or Image.
•	Fallback to dynamic if missing.

Pros:
•	Scroll performance: excellent. You’re decoding tiny images instead of originals / PDFs / video each time.
•	DB stays lean: only strings for paths, not blobs.
•	Easier to invalidate: you can delete/regen thumbnail file without touching DB structure.

Cons:
•	Disk usage (but usually better than keeping it in DB).
•	Need cleanup: when media is deleted, delete its thumbnail file.

If you ever implement “clear cache” or “cleanup old projects”, you can safely nuke the thumbnail folder and lazily regenerate when needed.

2.3. Staying with purely dynamic thumbnails (what you do now)

For images and short videos:
•	Coil + downsampling on demand is generally OK, especially when:
•	You use .size() / .placeholder() properly.
•	You rely on Coil’s in-memory and disk caching.
•	With a lazy list and not too many visible items, this is often enough.

For PDFs:
•	Dynamic rendering is the most expensive piece.
•	You already mitigated it with:
•	LruCache (~8 MB),
•	PdfRenderer on Dispatchers.IO,
•	cancellable jobs tied to item views.

If your PDF count is low or lists are small, this is perfectly acceptable. If users can scroll through hundreds of PDFs, you will feel the cost.

⸻

3. What I’d recommend for your app

Given your context (Save, potentially big libraries, mixed media):
1.	Short term (low effort)
•	Keep your current dynamic approach, but:
•	Optionally switch to the Compose-native PdfThumbnail as shown above (or wrap your existing loader into it).
•	Fix getFileInfoText to avoid file.exists()/length() inside recomposition (we discussed this earlier).
2.	Medium term (if you hit perf issues with large libraries)
•	Introduce file-based thumbnails:
•	Generate PDF thumbnails and maybe video thumbnails in background when media is imported or queued.
•	Store thumbnailPath in Media.
•	In both PreviewMedia and UploadManager, prefer thumbnailPath when present; fallback to dynamic.
3.	Avoid:
•	Storing thumbnail bitmaps as SQLite BLOBs in the main Media table. It works, but it ages badly as the collection grows.

So:
•	Better Compose way for PDF thumbnail? Yes – using LaunchedEffect + ImageBitmap as shown.
•	Pre-generate thumbnails?
•	Yes for PDFs (and maybe videos) if you see real-world jank, but store them as files and reference paths in Room, not as BLOB columns.
•	For now, your dynamic+cache strategy is ok; only upgrade if profiling or real usage says “this list is getting heavy.”