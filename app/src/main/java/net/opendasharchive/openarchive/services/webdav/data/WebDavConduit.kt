package net.opendasharchive.openarchive.services.webdav.data

import android.content.Context
import com.thegrizzlylabs.sardineandroid.SardineListener
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.core.domain.Evidence
import net.opendasharchive.openarchive.services.Conduit
import net.opendasharchive.openarchive.services.SaveClient
import okhttp3.HttpUrl
import java.io.IOException


class WebDavConduit(evidence: Evidence, context: Context) : Conduit(evidence, context) {

    private lateinit var mClient: OkHttpSardine

    override suspend fun upload(): Boolean {
        val vault = spaceRepository.getSpaceById(mEvidence.vaultId) ?: return false
        val base = vault.hostUrl ?: return false
        val path = getPath() ?: return false

        mClient = SaveClient.getSardine(mContext, vault.username, vault.password)

        sanitize()

        val fileName = getUploadFileName(mEvidence)

        try {
            val archive = projectRepository.getProject(mEvidence.archiveId)
            createFolders(base, path, archive?.isRemote ?: false)

            uploadMetadata(base, path, fileName)
        } catch (e: Throwable) {
            jobFailed(e)

            return false
        }

//        if (space.useChunking && mMedia.contentLength > CHUNK_FILESIZE_THRESHOLD) {
//            return uploadChunked(base, path, fileName)
//        }

        AppLogger.i("Begin media file upload...")
        if (mEvidence.contentLength > CHUNK_FILESIZE_THRESHOLD) {
            return uploadChunked(base, path, fileName)
        }

        val fullPath = construct(base, path, fileName)
        AppLogger.i("Uploading started for single file upload...", "filePath: $fullPath")

        try {
            mClient.put(
                mContext.contentResolver,
                fullPath,
                mEvidence.fileUri,
                mEvidence.contentLength,
                mEvidence.mimeType,
                false,
                object : SardineListener {
                    var lastBytes: Long = 0

                    override fun transferred(bytes: Long) {
                        if (bytes > lastBytes) {
                            jobProgress(bytes)
                            lastBytes = bytes
                        }
                        AppLogger.i("Bytes transferred for for ${mEvidence.id}: ", "$bytes")
                    }

                    override fun continueUpload(): Boolean {
                        AppLogger.i("Should continue upload for ${mEvidence.id}?", "$mCancelled")
                        return !mCancelled
                    }
                })
        } catch (e: Throwable) {
            jobFailed(e)

            return false
        }

        mEvidence = mEvidence.copy(serverUrl = fullPath)
        jobSucceeded()

        return true
    }

    override suspend fun createFolder(url: String) {
        if (!mClient.exists(url)) {
            mClient.createDirectory(url)
        } else {
            AppLogger.i("folder already exists: ", url)
        }
    }

    @Throws(IOException::class)
    private suspend fun uploadChunked(base: HttpUrl, path: List<String>, fileName: String): Boolean {
        AppLogger.i("Uploading started as chunked upload...")
        val vault = spaceRepository.getSpaceById(mEvidence.vaultId) ?: return false
        val url = vault.hostUrl ?: return false

        val tmpBase = HttpUrl.Builder()
            .scheme(url.scheme)
            .username(url.username)
            .password(url.password)
            .host(url.host)
            .port(url.port)
            .query(url.query)
            .fragment(url.fragment)
            .addPathSegment("remote.php")
            .addPathSegment("dav")
            .build()

        val tmpPath = listOf("uploads", vault.username, fileName)

        return try {
            createFolders(tmpBase, tmpPath)

            // Create chunks and start uploads. Look for existing chunks, and skip if done.
            // Start with the last chunk and re-upload.

            var offset = 0

            mEvidence.file.inputStream().use { inputStream ->
                while (!mCancelled && offset < mEvidence.contentLength) {
                    var buffer = ByteArray(CHUNK_SIZE.toInt())

                    val length = inputStream.read(buffer)

                    if (length < 1) break

                    if (length < CHUNK_SIZE) buffer = buffer.copyOfRange(0, length)

                    val total = offset + length

                    val chunkPath = construct(tmpBase, tmpPath, "$offset-$total")
                    val chunkExists = mClient.exists(chunkPath)
                    var chunkLengthMatches = false

                    if (chunkExists) {
                        val dirList = mClient.list(chunkPath)
                        chunkLengthMatches =
                            !dirList.isNullOrEmpty() && dirList.first().contentLength == length.toLong()
                    }

                    if (!chunkExists || !chunkLengthMatches) {
                        mClient.put(
                            chunkPath,
                            buffer,
                            mEvidence.mimeType,
                            object : SardineListener {
                                override fun transferred(bytes: Long) {
                                    jobProgress(offset.toLong() + bytes)
                                }

                                override fun continueUpload(): Boolean {
                                    return !mCancelled
                                }
                            })
                    }

                    jobProgress(total.toLong())
                    offset = total + 1
                }
            }

            if (mCancelled) throw Exception("Cancelled")

            val dest = mutableListOf("files", vault.username)
            dest.addAll(path)

            mClient.move(construct(tmpBase, tmpPath, ".file"), construct(tmpBase, dest, fileName))

            mEvidence = mEvidence.copy(serverUrl = construct(base, path, fileName))

            jobSucceeded()

            true
        } catch (e: Throwable) {
            jobFailed(e)

            false
        }
    }

    private fun uploadMetadata(base: HttpUrl, path: List<String>, fileName: String) {
        AppLogger.i("Uploading metadata....")
        val metadata = getMetadata()

        if (mCancelled) throw Exception("Cancelled")

        mClient.put(
            construct(base, path, "$fileName.meta.json"),
            metadata.toByteArray(),
            "text/plain",
            null
        )

        /// Upload ProofMode metadata, if enabled and successfully created.
        for (file in getProof()) {
            if (mCancelled) throw Exception("Cancelled")

            mClient.put(
                construct(base, path, file.name), file, "text/plain",
                false, null
            )
        }
    }
}