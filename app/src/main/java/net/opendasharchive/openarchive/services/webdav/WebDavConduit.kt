package net.opendasharchive.openarchive.services.webdav

import android.content.Context
import com.thegrizzlylabs.sardineandroid.SardineListener
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.services.Conduit
import net.opendasharchive.openarchive.services.SaveClient
import okhttp3.HttpUrl
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.IOException


class WebDavConduit(media: Media, context: Context) : Conduit(media, context), KoinComponent {

    private val client: SaveClient by inject()

    private val space = mMedia.space!!

    private var webdav = client.webdav(space)

    override suspend fun upload(): Boolean {
        val base = space.hostUrl ?: return false
        val path = getPath() ?: return false

        sanitize()

        val fileName = getUploadFileName(mMedia)

        try {
            createFolders(base, path)

            webdav.uploadMetadata(base, path, fileName)
        }
        catch (e: Throwable) {
            jobFailed(e)

            return false
        }

//        if (space.useChunking && mMedia.contentLength > CHUNK_FILESIZE_THRESHOLD) {
//            return uploadChunked(base, path, fileName)
//        }

        AppLogger.i("Begin media file upload...")
        if (mMedia.contentLength > CHUNK_FILESIZE_THRESHOLD) {
            return webdav.uploadChunked(base, path, fileName)
        }

        val fullPath = construct(base, path, fileName)
        AppLogger.i("Uploading started for single file upload...", "filePath: $fullPath")

        try {
            webdav.put(mContext.contentResolver,
                fullPath,
                mMedia.fileUri,
                mMedia.contentLength,
                mMedia.mimeType,
                false,
                object : SardineListener {
                    var lastBytes: Long = 0

                    override fun transferred(bytes: Long) {
                        if (bytes > lastBytes) {
                            jobProgress(bytes)
                            lastBytes = bytes
                        }
                        AppLogger.i("Bytes transferred for for ${mMedia.id}: ", "$bytes")
                    }

                    override fun continueUpload(): Boolean {
                        AppLogger.i("Should continue upload for ${mMedia.id}?", "$mCancelled")
                        return !mCancelled
                    }
                })
        }
        catch (e: Throwable) {
            jobFailed(e)

            return false
        }

        mMedia.serverUrl = fullPath
        jobSucceeded()

        return true
    }

    override suspend fun createFolder(url: String) {
        if (!webdav.exists(url)) {
            webdav.createDirectory(url)
        } else {
            AppLogger.i("folder already exists: ", url)
        }
    }

    @Throws(IOException::class)
    private suspend fun OkHttpSardine.uploadChunked(base: HttpUrl, path: List<String>, fileName: String): Boolean {
        AppLogger.i("Uploading started as chunked upload...")

        val space = mMedia.space ?: return false
        val url = space.hostUrl ?: return false

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

        val tmpPath = listOf("uploads", space.username, fileName)

        return try {
            createFolders(tmpBase, tmpPath)

            // Create chunks and start uploads. Look for existing chunks, and skip if done.
            // Start with the last chunk and re-upload.

            var offset = 0

            mMedia.file.inputStream().use { inputStream ->
                while (!mCancelled && offset < mMedia.contentLength) {
                    var buffer = ByteArray(CHUNK_SIZE.toInt())

                    val length = inputStream.read(buffer)

                    if (length < 1) break

                    if (length < CHUNK_SIZE) buffer = buffer.copyOfRange(0, length)

                    val total = offset + length

                    val chunkPath = construct(tmpBase, tmpPath, "$offset-$total")
                    val chunkExists = exists(chunkPath)
                    var chunkLengthMatches = false

                    if (chunkExists) {
                        val dirList = list(chunkPath)
                        chunkLengthMatches =
                            !dirList.isNullOrEmpty() && dirList.first().contentLength == length.toLong()
                    }

                    if (!chunkExists || !chunkLengthMatches) {
                        put(
                            chunkPath,
                            buffer,
                            mMedia.mimeType,
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

            val dest = mutableListOf("files", space.username)
            dest.addAll(path)

            move(construct(tmpBase, tmpPath, ".file"), construct(tmpBase, dest, fileName))

            mMedia.serverUrl = construct(base, path, fileName)

            jobSucceeded()

            true
        }
        catch (e: Throwable) {
            jobFailed(e)

            false
        }
    }

    private fun OkHttpSardine.uploadMetadata(base: HttpUrl, path: List<String>, fileName: String) {
        AppLogger.i("Uploading metadata....")
        val metadata = getMetadata()

        if (mCancelled) throw Exception("Cancelled")

        put(
            construct(base, path, "$fileName.meta.json"),
            metadata.toByteArray(),
            "text/plain",
            null
        )

        /// Upload ProofMode metadata, if enabled and successfully created.
        for (file in getProof()) {
            if (mCancelled) throw Exception("Cancelled")

            put(
                construct(base, path, file.name), file, "text/plain",
                false, null)
        }
    }
}