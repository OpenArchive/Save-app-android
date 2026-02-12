package net.opendasharchive.openarchive.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import net.opendasharchive.openarchive.core.logger.AppLogger
import java.io.File
import java.io.FileOutputStream

object ShareUtils {

    /**
     * Saves a bitmap to the application's cache directory and returns a content URI.
     */
    fun saveBitmapToCache(context: Context, bitmap: Bitmap, fileName: String): Uri? {
        return try {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, fileName)
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
        } catch (e: Exception) {
            AppLogger.e("Failed to save bitmap to cache", e)
            null
        }
    }
}
