package net.opendasharchive.openarchive.services.gdrive

import com.google.api.client.http.InputStreamContent
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.*
import java.io.File
import java.io.InputStream

fun interface ProgressListener {
    fun onProgressUpdate(uploadedBytes: Long, percent: Int)
}

class ProgressRequestBody(
    private val content: InputStreamContent,
    private val contentType: String,
    private val chunkSize: Int = 262144,
    private val listener: (bytesWritten: Long, contentLength: Long) -> Unit
) : RequestBody() {

    override fun contentType(): MediaType? = content.type.toMediaTypeOrNull()

    override fun contentLength(): Long = content.length

    override fun writeTo(sink: BufferedSink) {
        val buffer = ByteArray(8192)
        var bytesRead: Int
        var totalBytesWritten = 0L

        while (content.inputStream.read(buffer).also { bytesRead = it } != -1) {
            sink.write(buffer, 0, bytesRead)
            totalBytesWritten += bytesRead
            listener(totalBytesWritten, contentLength())
        }
    }
}