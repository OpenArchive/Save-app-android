package net.opendasharchive.openarchive.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import android.widget.ImageView
import android.net.Uri
import androidx.core.net.toFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.core.logger.AppLogger

object PdfThumbnailLoader {

    private const val DEFAULT_MAX_DIMENSION = 600
    private const val CACHE_SIZE_KB = 8 * 1024 // ~8MB

    private val cache = object : LruCache<String, Bitmap>(CACHE_SIZE_KB) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount / 1024
        }
    }

    fun loadThumbnail(
        imageView: ImageView,
        uri: Uri,
        placeholderRes: Int,
        scope: CoroutineScope,
        maxDimensionPx: Int = DEFAULT_MAX_DIMENSION,
        context: Context,
        requestKey: Any? = null,
        onPlaceholder: (() -> Unit)? = null,
        onResult: (Boolean) -> Unit = {}
    ): Job {
        // Use a stable target size to avoid cache misses when the same item is rebound
        val targetWidth = maxDimensionPx
        val targetHeight = maxDimensionPx

        val file = runCatching { uri.toFile() }.getOrNull()
        val cacheKey = buildString {
            append(uri.toString())
            append(":")
            append(file?.lastModified() ?: 0)
            append(":")
            append(targetWidth)
            append("x")
            append(targetHeight)
        }

        val cached = cache[cacheKey]
        if (cached != null) {
            return scope.launch(Dispatchers.Main.immediate) {
                if (!isTargetValid(imageView, requestKey)) return@launch
                imageView.setImageBitmap(cached)
                imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                imageView.setPadding(0, 0, 0, 0)
                imageView.imageTintList = null
                onResult(true)
            }
        } else {
            if (isTargetValid(imageView, requestKey)) {
                onPlaceholder?.invoke() ?: imageView.setImageResource(placeholderRes)
            }
        }

        return scope.launch(Dispatchers.Main.immediate) {
            val bitmap = withContext(Dispatchers.IO) {
                cache[cacheKey] ?: openDescriptor(context, uri)?.use { descriptor ->
                    renderPdfFirstPage(
                        descriptor = descriptor,
                        targetWidth = targetWidth,
                        targetHeight = targetHeight
                    )
                }?.also { rendered ->
                    cache.put(cacheKey, rendered)
                }
            }

            if (!isActive) return@launch
            if (!isTargetValid(imageView, requestKey)) return@launch

            if (bitmap != null) {
                imageView.setImageBitmap(bitmap)
                imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                imageView.setPadding(0, 0, 0, 0)
                imageView.imageTintList = null
                onResult(true)
            } else {
                imageView.setImageResource(placeholderRes)
                onResult(false)
            }
        }
    }

    private fun renderPdfFirstPage(
        descriptor: ParcelFileDescriptor,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap? {
        if (targetWidth <= 0 || targetHeight <= 0) return null

        return try {
            PdfRenderer(descriptor).use { renderer ->
                if (renderer.pageCount == 0) return null

                renderer.openPage(0).use { page ->
                    val scale = minOf(
                        targetWidth.toFloat() / page.width,
                        targetHeight.toFloat() / page.height
                    ).coerceAtLeast(0.1f)

                    val bitmapWidth = (page.width * scale).toInt().coerceAtLeast(1)
                    val bitmapHeight = (page.height * scale).toInt().coerceAtLeast(1)

                    val bitmap =
                        Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)

                    // Fill with white background explicitly to ensure consistent rendering
                    // PDFs are typically designed for white backgrounds
                    Canvas(bitmap).drawColor(Color.WHITE)

                    val matrix = Matrix().apply {
                        postScale(scale, scale)
                    }

                    page.render(
                        bitmap,
                        null,
                        matrix,
                        PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                    )

                    bitmap
                }
            }
        } catch (e: Exception) {
            AppLogger.w("Failed to render PDF thumbnail: ${e.message}")
            null
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
            AppLogger.w("Failed to open PDF descriptor: ${e.message}")
            null
        }
    }

    private fun isTargetValid(imageView: ImageView, requestKey: Any?): Boolean {
        return requestKey == null || imageView.tag == requestKey
    }
}
