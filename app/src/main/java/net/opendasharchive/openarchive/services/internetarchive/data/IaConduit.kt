package net.opendasharchive.openarchive.services.internetarchive.data

import android.content.Context
import android.net.Uri
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.domain.Evidence
import net.opendasharchive.openarchive.core.domain.VaultAuth
import net.opendasharchive.openarchive.core.domain.Vault
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.services.Conduit
import net.opendasharchive.openarchive.services.SaveClient
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.File
import java.io.IOException
import androidx.core.net.toUri
import net.opendasharchive.openarchive.services.common.network.RequestBodyUtil
import net.opendasharchive.openarchive.services.common.network.RequestListener
import net.opendasharchive.openarchive.services.common.network.createListener
import net.opendasharchive.openarchive.util.Utility

class IaConduit(evidence: Evidence, context: Context) : Conduit(evidence, context) {


    companion object {
        const val ARCHIVE_BASE_URL = "https://archive.org/"
        const val NAME = "Internet Archive"

        const val ARCHIVE_API_ENDPOINT = "https://s3.us.archive.org"
        private const val ARCHIVE_DETAILS_ENDPOINT = "https://archive.org/details/"

        private fun getSlug(title: String): String {
            return title.replace("[^A-Za-z\\d]".toRegex(), "-")
        }

        val textMediaType = "texts".toMediaTypeOrNull()

        private val gson = GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()
    }

    override suspend fun upload(): Boolean {
        sanitize()

        try {
            val vault = spaceRepository.getSpaceById(mEvidence.vaultId) ?: return false
            val auth = spaceRepository.getVaultAuth(mEvidence.vaultId) ?: return false
            val mimeType = mEvidence.mimeType

            val client = SaveClient.get(mContext)

            val fileName = getUploadFileName(mEvidence, true)
            val metaJson = gson.toJson(mEvidence)
            val c2paManifest = getC2paManifest()

            if (mEvidence.serverUrl.isBlank()) {
                // TODO this should make sure we aren't accidentally using one of archive.org's metadata fields by accident
                val slug = getSlug(mEvidence.title)
                val newIdentifier = "$slug-${Utility.RandomString(4).nextString()}"
                // create an identifier for the upload
                mEvidence = mEvidence.copy(serverUrl = newIdentifier)
            }

            // upload content synchronously for progress
            client.uploadContent(fileName, mimeType, vault, auth)

            // upload metadata and proofs, and report failures
            client.uploadMetaData(metaJson, fileName, auth)

            // Upload C2PA manifest, if enabled and successfully created.
            if (c2paManifest != null) {
                AppLogger.d("Uploading C2PA manifest to Internet Archive: ${c2paManifest.name}")
                client.uploadProofFiles(c2paManifest, auth)
            }

            jobSucceeded()

            return true
        } catch (e: Throwable) {
            jobFailed(e)
        }

        return false
    }

    override suspend fun createFolder(url: String) {
        // Ignored. Not used here.
    }

    private suspend fun OkHttpClient.uploadContent(
        fileName: String,
        mimeType: String,
        vault: Vault,
        auth: VaultAuth
    ) {
        val mediaUri = mEvidence.originalFilePath

        val url = "${ARCHIVE_API_ENDPOINT}/${mEvidence.serverUrl}/$fileName"

        val requestBody = RequestBodyUtil.create(
            mContext.contentResolver,
            mediaUri.toUri(),
            mEvidence.contentLength,
            mimeType.toMediaTypeOrNull(),
            createListener(
                cancellable = { !mCancelled },
                onProgress = {
                    jobProgress(it)
                }
            )
        )

        val request = Request.Builder()
            .url(url)
            .put(requestBody)
            .headers(mainHeader(vault, auth))
            .build()

        execute(request)
    }

    @Throws(IOException::class)
    private suspend fun OkHttpClient.uploadMetaData(content: String, fileName: String, auth: VaultAuth) {
        val requestBody = RequestBodyUtil.create(
            textMediaType,
            content.byteInputStream(),
            content.length.toLong(),
            createListener(cancellable = { !mCancelled })
        )

        val url = "${ARCHIVE_API_ENDPOINT}/${mEvidence.serverUrl}/$fileName.meta.json"

        val request = Request.Builder()
            .url(url)
            .put(requestBody)
            .headers(metadataHeader(auth))
            .build()

        execute(request)
    }

    /// upload proof mode
    @Throws(IOException::class)
    private suspend fun OkHttpClient.uploadProofFiles(uploadFile: File, auth: VaultAuth) {
        val requestBody = RequestBodyUtil.create(
            mContext.contentResolver,
            Uri.fromFile(uploadFile),
            uploadFile.length(),
            textMediaType, createListener(cancellable = { !mCancelled })
        )

        val url = "$ARCHIVE_API_ENDPOINT/${mEvidence.serverUrl}/${uploadFile.name}"

        val request = Request.Builder()
            .url(url)
            .put(requestBody)
            .headers(metadataHeader(auth))
            .build()

        execute(request)
    }

    private fun mainHeader(vault: Vault, auth: VaultAuth): Headers {
        val builder = Headers.Builder()
            .add("Accept", "*/*")
            .add("x-archive-auto-make-bucket", "1")
            .add("x-amz-auto-make-bucket", "1")
            .add("x-archive-interactive-priority", "1")
            .add("x-archive-meta-language", "eng") // FIXME set based on locale or selected.
            .add("Authorization", "LOW " + auth.username + ":" + auth.secret)

        val author = mEvidence.author
        if (author.isNotEmpty()) {
            builder.add("x-archive-meta-author", author)
        }

        if (mEvidence.contentLength > 0) {
            builder.add("x-archive-size-hint", mEvidence.contentLength.toString())
        }

        val collection = when {
            mEvidence.mimeType.startsWith("video") -> "opensource_movies"
            mEvidence.mimeType.startsWith("audio") -> "opensource_audio"
            else -> "opensource_media"
        }
        builder.add("x-archive-meta-collection", collection)

        if (mEvidence.mimeType.isNotEmpty()) {
            val mediaType = when {
                mEvidence.mimeType.startsWith("image") -> "image"
                mEvidence.mimeType.startsWith("video") -> "movies"
                mEvidence.mimeType.startsWith("audio") -> "audio"
                else -> "data"
            }
            builder.add("x-archive-meta-mediatype", mediaType)
        }

        if (mEvidence.location.isNotEmpty()) {
            builder.add("x-archive-meta-location", sanitizeHeaderValue(mEvidence.location))
        }

        if (mEvidence.tags.isNotEmpty()) {
            val tags = mEvidence.tags.toMutableList()
            tags.add(mContext.getString(R.string.default_tags))
            mEvidence = mEvidence.copy(tags = tags)

            builder.add("x-archive-meta-subject", tags.joinToString(","))
        }

        if (mEvidence.description.isNotEmpty()) {
            builder.add("x-archive-meta-description", sanitizeHeaderValue(mEvidence.description))
        }

        if (mEvidence.title.isNotEmpty()) {
            builder.add("x-archive-meta-title", mEvidence.title)
        }

        var licenseUrl = mEvidence.licenseUrl

        if (licenseUrl.isNullOrEmpty()) {
            licenseUrl = "https://creativecommons.org/licenses/by/4.0/"
        }

        builder.add("x-archive-meta-licenseurl", licenseUrl)

        return builder.build()
    }

    /// headers for meta-data and proof mode
    private fun metadataHeader(auth: VaultAuth): Headers {
        return Headers.Builder()
            .add("x-amz-auto-make-bucket", "1")
            .add("x-archive-meta-language", "eng") // TODO: FIXME set based on locale or selected
            .add("Authorization", "LOW " + auth.username + ":" + auth.secret)
            .add("x-archive-meta-mediatype", "texts")
            .add("x-archive-meta-collection", "opensource")
            .build()
    }

    @Throws(Exception::class)
    private suspend fun OkHttpClient.execute(request: Request) = withContext(Dispatchers.IO) {
        val result = newCall(request)
            .execute()

        if (result.isSuccessful.not()) {
            throw RuntimeException("${result.code}: ${result.message}")
        }
    }

    @Throws(Exception::class)
    private fun OkHttpClient.enqueue(request: Request) {
        newCall(request)
            .enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    jobFailedAsync(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        jobFailedAsync(Exception("${response.code}: ${response.message}"))
                    }
                }

            })
    }

    private fun sanitizeHeaderValue(value: String): String {
        return value.replace("[^\\x20-\\x7E]".toRegex(), "") // Removes non-ASCII characters
    }
}
