package net.opendasharchive.openarchive.util.extensions

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Base64
import timber.log.Timber
import java.io.*
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class DecryptedFileProvider : ContentProvider() {

    companion object {
        private const val AUTHORITY = "net.opendasharchive.openarchive.decryptedfileprovider"
        private const val FILES = 1

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "file/*", FILES)
        }

        fun getUriForFile(context: Context, file: File): Uri {
            return Uri.parse("content://$AUTHORITY/file/${file.name}")
        }
    }

    override fun onCreate(): Boolean = true

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null

    override fun getType(uri: Uri): String? {
        val fileName = uri.lastPathSegment ?: return null
        return getMimeType(fileName)
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val fileName = uri.lastPathSegment ?: return null
        val context = context ?: return null

        val encryptedFile = File(context.filesDir, fileName) // Ensure correct directory

        if (!encryptedFile.exists()) {
            Timber.e("Decryption failed: File not found -> ${encryptedFile.absolutePath}")
            return null
        }

        val decryptedInputStream = decryptFileToInputStream(context, encryptedFile)
        if (decryptedInputStream == null) {
            Timber.e("Failed to decrypt file for URI: $uri")
            return null
        }

        // Use a pipe to stream the decrypted file content
        val pipe = ParcelFileDescriptor.createPipe()

        Thread {
            try {
                decryptedInputStream.use { input ->
                    ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: IOException) {
                Timber.e(e, "Error writing decrypted file to pipe for URI: $uri")
            }
        }.start()

        return pipe[0]
    }

    /**
     * Decrypts an encrypted file and returns an InputStream.
     * **The decrypted file is NOT saved; it's streamed directly.**
     */
    private fun decryptFileToInputStream(context: Context, encryptedFile: File): InputStream? {
        return try {
            if (!encryptedFile.exists()) {
                Timber.e("Decryption failed: File does not exist -> ${encryptedFile.absolutePath}")
                return null
            }

            val sharedPreferences = context.getSharedPreferences("EncryptedFiles", Context.MODE_PRIVATE)

            val keyBase64 = sharedPreferences.getString("global-aes-key", null)
            val ivBase64 = sharedPreferences.getString("${encryptedFile.absolutePath}-iv", null)

            if (keyBase64.isNullOrEmpty() || ivBase64.isNullOrEmpty()) {
                Timber.e("Decryption failed: Missing AES key or IV for ${encryptedFile.absolutePath}")
                return null
            }

            val key = Base64.decode(keyBase64, Base64.DEFAULT)
            val iv = Base64.decode(ivBase64, Base64.DEFAULT)

            val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding") // Ensure correct mode & padding
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))

            Timber.d("Successfully initialized AES decryption for: ${encryptedFile.absolutePath}")

            CipherInputStream(FileInputStream(encryptedFile), cipher)

        } catch (e: Exception) {
            Timber.e(e, "Decryption error for file: ${encryptedFile.absolutePath}")
            null
        }
    }

    /**
     * Determines the MIME type based on the file extension.
     */
    private fun getMimeType(fileName: String): String? {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }
}
