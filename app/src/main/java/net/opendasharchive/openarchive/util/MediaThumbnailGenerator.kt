package net.opendasharchive.openarchive.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.media.MediaMetadataRetriever
import android.os.ParcelFileDescriptor
import net.opendasharchive.openarchive.core.logger.AppLogger
import java.io.ByteArrayOutputStream
import java.io.File

object MediaThumbnailGenerator {
    private const val MAX_DIMENSION_PX = 128
    private const val JPEG_QUALITY = 40

    fun generateThumbnailBytes(file: File, mimeType: String): ByteArray? {
        if (!file.exists()) return null

        val bitmap = try {
            when {
                mimeType.startsWith("image/") -> createImageThumbnail(file)
                mimeType.startsWith("video/") -> createVideoThumbnail(file)
                mimeType == "application/pdf" -> createPdfThumbnail(file)
                else -> null
            }
        } catch (e: Exception) {
            AppLogger.w("Failed to generate thumbnail for ${file.name}: ${e.message}")
            null
        } ?: return null

        return bitmap.useCompressedJpeg()
    }

    private fun createImageThumbnail(file: File): Bitmap? {
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.absolutePath, bounds)

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val sampled = BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, MAX_DIMENSION_PX)
                inPreferredConfig = Bitmap.Config.RGB_565
            }
        ) ?: return null

        return sampled.scaleDown(MAX_DIMENSION_PX)
    }

    private fun createVideoThumbnail(file: File): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val frame = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            frame?.scaleDown(MAX_DIMENSION_PX)
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun createPdfThumbnail(file: File): Bitmap? {
        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        return try {
            PdfRenderer(descriptor).use { renderer ->
                if (renderer.pageCount == 0) return null

                renderer.openPage(0).use { page ->
                    val scale = minOf(
                        MAX_DIMENSION_PX.toFloat() / page.width,
                        MAX_DIMENSION_PX.toFloat() / page.height
                    ).coerceAtLeast(0.1f)

                    val bitmap = Bitmap.createBitmap(
                        (page.width * scale).toInt().coerceAtLeast(1),
                        (page.height * scale).toInt().coerceAtLeast(1),
                        Bitmap.Config.ARGB_8888
                    )

                    Canvas(bitmap).drawColor(Color.WHITE)
                    val matrix = Matrix().apply { postScale(scale, scale) }

                    page.render(
                        bitmap,
                        null,
                        matrix,
                        PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                    )

                    bitmap
                }
            }
        } finally {
            descriptor.close()
        }
    }

    private fun Bitmap.scaleDown(maxDimensionPx: Int): Bitmap {
        if (width <= maxDimensionPx && height <= maxDimensionPx) return this

        val scale = minOf(
            maxDimensionPx.toFloat() / width,
            maxDimensionPx.toFloat() / height
        )
        val scaled = Bitmap.createScaledBitmap(
            this,
            (width * scale).toInt().coerceAtLeast(1),
            (height * scale).toInt().coerceAtLeast(1),
            true
        )

        if (scaled != this) {
            recycle()
        }
        return scaled
    }

    private fun Bitmap.useCompressedJpeg(): ByteArray? {
        return try {
            ByteArrayOutputStream().use { output ->
                compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
                output.toByteArray()
            }
        } finally {
            recycle()
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDimensionPx: Int): Int {
        var inSampleSize = 1
        var currentWidth = width
        var currentHeight = height

        while (currentWidth / 2 >= maxDimensionPx || currentHeight / 2 >= maxDimensionPx) {
            inSampleSize *= 2
            currentWidth /= 2
            currentHeight /= 2
        }

        return inSampleSize.coerceAtLeast(1)
    }
}
