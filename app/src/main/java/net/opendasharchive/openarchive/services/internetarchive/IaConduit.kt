package net.opendasharchive.openarchive.services.internetarchive

import android.content.Context
import android.net.Uri
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.services.Conduit
import net.opendasharchive.openarchive.services.SaveClient
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.io.InputStream

class IaConduit(media: Media, context: Context) : Conduit(media, context) {

    companion object {
        const val ARCHIVE_BASE_URL = "https://archive.org/"
        const val NAME = "Internet Archive"
        const val ARCHIVE_API_ENDPOINT = "https://s3.us.archive.org"

        private fun getSlug(title: String): String {
            return title.replace("[^A-Za-z\\d]".toRegex(), "-")
        }

        val textMediaType = "texts".toMediaTypeOrNull()
        private val gson = GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()
    }

    override suspend fun upload(): Boolean {
        sanitize()

        return try {
            val client = SaveClient.get(mContext)
            val fileName = getUploadFileName(mMedia, true)
            val metaJson = gson.toJson(mMedia)

            if (mMedia.serverUrl.isBlank()) {
                val slug = getSlug(mMedia.title)
                val newIdentifier = "$slug-${Util.RandomString(4).nextString()}"
                mMedia.serverUrl = newIdentifier
            }

            // **Decrypt the file before uploading**
            val decryptedFile = decryptFileForUpload(mMedia)

            if (decryptedFile == null) {
                Timber.e("Decryption failed, cannot upload ${mMedia.title}")
                return false
            }

            // Upload content synchronously using the decrypted file
            client.uploadContent(decryptedFile, fileName, mMedia.mimeType)

            // Upload metadata
            client.uploadMetaData(metaJson, fileName)

            // Delete decrypted file after upload
            decryptedFile.delete()

            jobSucceeded()
            true
        } catch (e: Throwable) {
            jobFailed(e)
            false
        }
    }

    private suspend fun OkHttpClient.uploadContent(decryptedFile: File, fileName: String, mimeType: String) {
        val url = "${ARCHIVE_API_ENDPOINT}/${mMedia.serverUrl}/$fileName"

        // Ensure the file exists before attempting to upload
        if (!decryptedFile.exists() || decryptedFile.length() == 0L) {
            AppLogger.e("Upload failed: Decrypted file is missing or empty for media ${mMedia.id}")
            return
        }

        val decryptedUri = mMedia.getFileUri(mContext) // ✅ Use content:// URI instead of file://

        val requestBody = RequestBodyUtil.create(
            mContext.contentResolver,
            decryptedUri, // ✅ Correctly passing a content URI instead of file://
            decryptedFile.length(),
            mimeType.toMediaTypeOrNull() ?: "application/octet-stream".toMediaType(),
            createListener(cancellable = { !mCancelled }, onProgress = { jobProgress(it) })
        )

        val request = Request.Builder()
            .url(url)
            .put(requestBody)
            .headers(mainHeader())
            .build()

        try {
            val response = newCall(request).execute() // ✅ Store the response

            if (response.isSuccessful) { // ✅ Now isSuccessful works
                AppLogger.i("Upload successful for media ${mMedia.id}")

                // ✅ Delete decrypted file only after successful upload
                decryptedFile.delete()
            } else {
                AppLogger.e("Upload failed with status ${response.code} for media ${mMedia.id}")
            }

            response.close() // ✅ Always close response to avoid memory leaks
        } catch (e: Exception) {
            AppLogger.e("Upload failed due to an exception: ${e.message}")
        }
    }

    @Throws(IOException::class)
    private fun OkHttpClient.uploadMetaData(content: String, fileName: String) {
        val requestBody = RequestBodyUtil.create(
            textMediaType,
            content.byteInputStream(),
            content.length.toLong(),
            createListener(cancellable = { !mCancelled })
        )

        val url = "${ARCHIVE_API_ENDPOINT}/${mMedia.serverUrl}/$fileName.meta.json"

        val request = Request.Builder()
            .url(url)
            .put(requestBody)
            .headers(metadataHeader())
            .build()

        enqueue(request)
    }

    override suspend fun createFolder(url: String) {
        // Not used for Internet Archive
    }

    /**
     * Returns a **decrypted file** that can be uploaded.
     * The file is stored temporarily and deleted after use.
     */
    private suspend fun decryptFileForUpload(media: Media): File? {
        return withContext(Dispatchers.IO) {
            try {
                val tempFile = File.createTempFile("decrypted_", ".tmp", mContext.cacheDir)
                val inputStream: InputStream? = media.fileInputStream(mContext)

                if (inputStream == null) {
                    Timber.e("Decryption failed for ${media.title}, input stream is null.")
                    return@withContext null
                }

                tempFile.outputStream().use { output ->
                    inputStream.copyTo(output)
                }

                inputStream.close()
                tempFile
            } catch (e: Exception) {
                Timber.e(e, "Failed to decrypt file for upload")
                null
            }
        }
    }

    private fun mainHeader(): Headers {
        val builder = Headers.Builder()
            .add("Accept", "*/*")
            .add("x-archive-auto-make-bucket", "1")
            .add("x-amz-auto-make-bucket", "1")
            .add("x-archive-interactive-priority", "1")
            .add("x-archive-meta-language", "eng") // TODO: Set dynamically
            .add("Authorization", "LOW " + mMedia.space?.username + ":" + mMedia.space?.password)

        mMedia.author.takeIf { it.isNotEmpty() }?.let {
            builder.add("x-archive-meta-author", it)
        }

        if (mMedia.contentLength > 0) {
            builder.add("x-archive-size-hint", mMedia.contentLength.toString())
        }

        val collection = when {
            mMedia.mimeType.startsWith("video") -> "opensource_movies"
            mMedia.mimeType.startsWith("audio") -> "opensource_audio"
            else -> "opensource_media"
        }
        builder.add("x-archive-meta-collection", collection)

        val mediaType = when {
            mMedia.mimeType.startsWith("image") -> "image"
            mMedia.mimeType.startsWith("video") -> "movies"
            mMedia.mimeType.startsWith("audio") -> "audio"
            else -> "data"
        }
        builder.add("x-archive-meta-mediatype", mediaType)

        mMedia.location.takeIf { it.isNotEmpty() }?.let {
            builder.add("x-archive-meta-location", it)
        }

        mMedia.tags.takeIf { it.isNotEmpty() }?.let {
            builder.add("x-archive-meta-subject", it)
        }

        mMedia.description.takeIf { it.isNotEmpty() }?.let {
            builder.add("x-archive-meta-description", it)
        }

        mMedia.title.takeIf { it.isNotEmpty() }?.let {
            builder.add("x-archive-meta-title", it)
        }

        builder.add("x-archive-meta-licenseurl", mMedia.licenseUrl ?: "https://creativecommons.org/licenses/by/4.0/")

        return builder.build()
    }

    private fun metadataHeader(): Headers {
        return Headers.Builder()
            .add("x-amz-auto-make-bucket", "1")
            .add("x-archive-meta-language", "eng")
            .add("Authorization", "LOW " + mMedia.space?.username + ":" + mMedia.space?.password)
            .add("x-archive-meta-mediatype", "texts")
            .add("x-archive-meta-collection", "opensource")
            .build()
    }

    @Throws(Exception::class)
    private suspend fun OkHttpClient.execute(request: Request) = withContext(Dispatchers.IO) {
        val response = newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("Upload failed: ${response.code} - ${response.message}")
        }
    }

    @Throws(Exception::class)
    private fun OkHttpClient.enqueue(request: Request) {
        newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                jobFailed(e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    jobFailed(Exception("Upload failed: ${response.code} - ${response.message}"))
                }
            }
        })
    }
}