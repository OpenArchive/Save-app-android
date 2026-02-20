package net.opendasharchive.openarchive.services.snowbird.util

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class SnowbirdFileStorage(private val context: Context) {

    suspend fun saveByteArrayToFile(byteArray: ByteArray, filename: String): Result<Uri> =
        withContext(Dispatchers.IO) {
            runCatching {
                val directory = File(context.filesDir, "files").apply { mkdirs() }
                val file = File(directory, filename)

                file.outputStream().use { it.write(byteArray) }

                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    file
                )
            }
        }

    suspend fun saveImageToGallery(
        imageBytes: ByteArray,
        displayName: String
    ): Uri? = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Save")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return@withContext null

            resolver.openOutputStream(uri)?.use { it.write(imageBytes) }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)

            return@withContext uri
        }

        val imagesDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "Save"
        ).apply { if (!exists()) mkdirs() }

        val file = File(imagesDir, displayName)
        FileOutputStream(file).use { it.write(imageBytes) }

        MediaScannerConnection.scanFile(
            context,
            arrayOf(file.absolutePath),
            arrayOf("image/jpeg"),
            null
        )
        return@withContext Uri.fromFile(file)
    }
}
