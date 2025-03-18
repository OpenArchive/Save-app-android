package net.opendasharchive.openarchive.services.webdav

import android.content.Context
import com.thegrizzlylabs.sardineandroid.SardineListener
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import kotlinx.coroutines.*
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.services.Conduit
import net.opendasharchive.openarchive.services.SaveClient
import okhttp3.HttpUrl
import java.io.IOException

class WebDavConduit(media: Media, context: Context) : Conduit(media, context) {

    // ✅ Correct lazy initialization (Deferred)
    private val mClient: Deferred<OkHttpSardine> by lazy {
        CoroutineScope(Dispatchers.IO).async(start = CoroutineStart.LAZY) {
            SaveClient.getSardine(mContext, mMedia.space!!)
        }
    }

    override suspend fun upload(): Boolean {
        val space = mMedia.space ?: return false
        val base = space.hostUrl ?: return false
        val path = getPath() ?: return false
        val client = mClient.await() // ✅ Correctly await the client

        sanitize()

        val fileName = getUploadFileName(mMedia)

        try {
            createFolders(base, path)
            uploadMetadata(base, path, fileName) // ✅ Now it's a suspend function
        } catch (e: Throwable) {
            jobFailed(e)
            return false
        }

        AppLogger.i("Begin media file upload...")
        if (mMedia.contentLength > CHUNK_FILESIZE_THRESHOLD) {
            return uploadChunked(base, path, fileName)
        }

        val fullPath = construct(base, path, fileName)
        AppLogger.i("Uploading started for single file upload...", "filePath: $fullPath")

        try {
            client.put(
                mContext.contentResolver,
                fullPath,
                mMedia.getFileUri(mContext),
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
                        AppLogger.i("Bytes transferred for ${mMedia.id}: $bytes")
                    }

                    override fun continueUpload(): Boolean {
                        AppLogger.i("Should continue upload for ${mMedia.id}?", "$mCancelled")
                        return !mCancelled
                    }
                }
            )
        } catch (e: Throwable) {
            jobFailed(e)
            return false
        }

        mMedia.serverUrl = fullPath
        jobSucceeded()

        return true
    }

    override suspend fun createFolder(url: String) {
        val client = mClient.await() // ✅ Correctly await the client
        if (!client.exists(url)) {
            client.createDirectory(url)
        } else {
            AppLogger.i("Folder already exists: $url")
        }
    }

    @Throws(IOException::class)
    private suspend fun uploadChunked(base: HttpUrl, path: List<String>, fileName: String): Boolean {
        AppLogger.i("Uploading started as chunked upload...")
        val space = mMedia.space ?: return false
        val url = space.hostUrl ?: return false
        val client = mClient.await() // ✅ Correctly await the client

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

            var offset = 0
            mMedia.fileInputStream(mContext)?.use { inputStream -> // ✅ Null-safe handling
                while (!mCancelled && offset < mMedia.contentLength) {
                    val buffer = ByteArray(CHUNK_SIZE.toInt())
                    val length = inputStream.read(buffer)

                    if (length < 1) break
                    val total = offset + length

                    val chunkPath = construct(tmpBase, tmpPath, "$offset-$total")
                    val chunkExists = client.exists(chunkPath)
                    val chunkLengthMatches = chunkExists && client.list(chunkPath)
                        .firstOrNull()?.contentLength == length.toLong()

                    if (!chunkExists || !chunkLengthMatches) {
                        client.put(
                            chunkPath,
                            buffer.copyOf(length),
                            mMedia.mimeType,
                            object : SardineListener {
                                override fun transferred(bytes: Long) {
                                    jobProgress(offset.toLong() + bytes)
                                }

                                override fun continueUpload(): Boolean {
                                    return !mCancelled
                                }
                            }
                        )
                    }

                    jobProgress(total.toLong())
                    offset = total + 1
                }
            } ?: throw IOException("Failed to open input stream for upload") // ✅ Handle null case

            if (mCancelled) throw Exception("Cancelled")

            val dest = mutableListOf("files", space.username)
            dest.addAll(path)
            client.move(construct(tmpBase, tmpPath, ".file"), construct(tmpBase, dest, fileName))

            mMedia.serverUrl = construct(base, path, fileName)
            jobSucceeded()
            true
        } catch (e: Throwable) {
            jobFailed(e)
            false
        }
    }

    // ✅ Now a suspend function to avoid `runBlocking`
    private suspend fun uploadMetadata(base: HttpUrl, path: List<String>, fileName: String) {
        AppLogger.i("Uploading metadata....")
        val metadata = getMetadata()

        if (mCancelled) throw Exception("Cancelled")

        val client = mClient.await() // ✅ Correctly await the client
        client.put(
            construct(base, path, "$fileName.meta.json"),
            metadata.toByteArray(),
            "text/plain",
            null
        )

        // ✅ Upload ProofMode metadata safely
        for (file in getProof()) {
            if (mCancelled) throw Exception("Cancelled")
            client.put(
                construct(base, path, file.name), file, "text/plain",
                false, null
            )
        }
    }
}