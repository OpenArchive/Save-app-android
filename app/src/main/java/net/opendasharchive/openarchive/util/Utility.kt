package net.opendasharchive.openarchive.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.util.Base64
import com.github.derlio.waveform.soundfile.SoundFile
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object Utility {

    fun getMimeType(context: Context, uri: Uri?): String? {
        val cR = context.contentResolver
        return cR.getType(uri!!)
    }

    fun getUriDisplayName(context: Context, uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, null, null, null, null) ?: return null

        var result: String? = null

        // Get the column indexes of the data in the Cursor,
        // move to the first row in the Cursor, get the data, and display it.
        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (idx >= 0 && cursor.moveToFirst()) {
            result = cursor.getString(idx)
        }

        cursor.close()

        return result
    }

    fun getOutputMediaFileByCache(context: Context, fileName: String): File? {
        val dir = context.filesDir
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                return null
            }
        }

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

        return File(dir, "$timeStamp.$fileName")
    }

    private const val PREFS_NAME = "EncryptedFiles"
    private const val KEY_GLOBAL = "global-aes-key"

    fun writeStreamToFile(context: Context, input: InputStream?, file: File?): Boolean {
        @Suppress("NAME_SHADOWING")
        val input = input ?: return false

        @Suppress("NAME_SHADOWING")
        val file = file ?: return false

        var success = false
        var output: FileOutputStream? = null

        try {
            val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            // Retrieve or generate global AES-256 key
            val key = getOrGenerateGlobalKey(sharedPreferences)

            // Retrieve or generate IV for this file
            val iv = getOrGenerateFileIV(sharedPreferences, file.absolutePath)

            // Initialize Cipher for AES encryption
            val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))

            // Open FileOutputStream to write encrypted data
            output = FileOutputStream(file)
            val cipherOutputStream = CipherOutputStream(output, cipher)

            // Read from input stream and encrypt
            val buffer = ByteArray(4 * 1024) // Buffer size
            var read: Int

            while (input.read(buffer).also { read = it } != -1) {
                cipherOutputStream.write(buffer, 0, read)
            }
            cipherOutputStream.flush()
            cipherOutputStream.close()

            success = true
        } catch (e: FileNotFoundException) {
            Timber.e(e)
        } catch (e: IOException) {
            Timber.e(e)
        } catch (e: Exception) {
            Timber.e(e, "Encryption error")
        } finally {
            try {
                output?.close()
            } catch (e: IOException) {
                Timber.e(e)
            }

            try {
                input.close()
            } catch (e: IOException) {
                Timber.e(e)
            }
        }

        return success
    }

    // Function to retrieve existing global AES-256 key or generate a new one
    private fun getOrGenerateGlobalKey(sharedPreferences: android.content.SharedPreferences): ByteArray {
        val keyBase64 = sharedPreferences.getString(KEY_GLOBAL, null)

        return if (keyBase64 != null) {
            Base64.decode(keyBase64, Base64.DEFAULT)
        } else {
            // Generate a new AES-256 key
            val key = generateAESKey()

            // Store it in SharedPreferences
            val editor = sharedPreferences.edit()
            editor.putString(KEY_GLOBAL, Base64.encodeToString(key, Base64.DEFAULT))
            editor.apply()

            key
        }
    }

    // Function to retrieve existing IV for a file or generate a new one
    private fun getOrGenerateFileIV(sharedPreferences: android.content.SharedPreferences, filePath: String): ByteArray {
        val ivBase64 = sharedPreferences.getString("$filePath-iv", null)

        return if (ivBase64 != null) {
            Base64.decode(ivBase64, Base64.DEFAULT)
        } else {
            // Generate a new IV
            val iv = generateIV()

            // Store IV in SharedPreferences
            val editor = sharedPreferences.edit()
            editor.putString("$filePath-iv", Base64.encodeToString(iv, Base64.DEFAULT))
            editor.apply()

            iv
        }
    }

    // Function to generate AES-256 key
    private fun generateAESKey(): ByteArray {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256) // 256-bit key
        return keyGen.generateKey().encoded
    }

    // Function to generate a 16-byte IV
    private fun generateIV(): ByteArray {
        val iv = ByteArray(16) // AES block size is 16 bytes
        SecureRandom().nextBytes(iv)
        return iv
    }

    fun openStore(context: Context, appId: String) {
        var i = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${appId}"))

        val capableApps = context.packageManager.queryIntentActivities(i, 0)

        // If there are no app stores installed, send to the web.
        if (capableApps.size < 1) {
            i = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${appId}"))
        }

        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        context.startActivity(i)
    }

    fun showMaterialWarning(context: Context, message: String? = null, positiveButtonText: String = "Ok", completion: (() -> Unit)? = null) {
        showMaterialMessage(context, "Oops", message, positiveButtonText, completion)
    }

    fun showMaterialMessage(context: Context, title: String = "Oops", message: String? = null, positiveButtonText: String = "Ok", completion: (() -> Unit)? = null) {
        Handler(Looper.getMainLooper()).post {
            MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveButtonText) { _, _ ->
                    completion?.invoke()
                }
                .show()
        }
    }

    fun showMaterialPrompt(
        context: Context,
        title: String,
        message: String? = null,
        positiveButtonText: String,
        negativeButtonText: String,
        completion: (Boolean) -> Unit
    ) {
        Handler(Looper.getMainLooper()).post {
            MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveButtonText) { dialog, _ ->
                    dialog.dismiss()
                    completion.invoke(true)
                }
                .setNegativeButton(negativeButtonText) { dialog, _ ->
                    dialog.dismiss()
                    completion.invoke(false)
                }
                .show()
        }
    }
}

fun createSoundFileFromStream(context: Context, inputStream: InputStream?): SoundFile? {
    if (inputStream == null) return null

    val tempFile = File(context.filesDir, "temp_audio_${System.currentTimeMillis()}.wav") // Adjust extension as needed

    return try {
        tempFile.outputStream().use { output -> inputStream.copyTo(output) }
        SoundFile.create(tempFile.absolutePath) { true } // ✅ Use file path
    } catch (e: Exception) {
        Timber.e(e, "Failed to create SoundFile from InputStream")
        null
    } finally {
        inputStream.close()
    }
}