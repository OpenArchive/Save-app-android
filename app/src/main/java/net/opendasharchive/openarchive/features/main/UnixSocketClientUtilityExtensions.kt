package net.opendasharchive.openarchive.features.main

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import kotlin.coroutines.cancellation.CancellationException

suspend fun UnixSocketClient.readBinaryResponseWithCancellation(
    inputStream: InputStream,
    onProgress: ((Long) -> Unit)? = null
): Triple<Int, Map<String, String>, ByteArray> = withContext(Dispatchers.IO) {
    val reader = BufferedReader(InputStreamReader(inputStream))

    // Read status line
    val statusLine = reader.readLine() ?: throw IOException("Empty response")
    val (_, statusCode, _) = statusLine.split(" ", limit = 3)

    // Read headers
    val headers = mutableMapOf<String, String>()
    var line: String?
    while (reader.readLine().also { line = it } != null) {
        if (line.isNullOrBlank()) break
        val (key, value) = line!!.split(": ", limit = 2)
        headers[key] = value
    }

    val outputStream = ByteArrayOutputStream()
    var totalBytesRead = 0L

    val isChunked = headers["Transfer-Encoding"]?.equals("chunked", ignoreCase = true) ?: false
    val contentLength = headers["Content-Length"]?.toLongOrNull()

    try {
        if (isChunked) {
            // Handle chunked transfer encoding
            while (isActive) {
                ensureActive()
                val chunkSizeLine = reader.readLine() ?: break
                val chunkSize = chunkSizeLine.trim().toInt(16)
                if (chunkSize == 0) break

                val buffer = ByteArray(chunkSize)
                var bytesRead = 0
                while (bytesRead < chunkSize) {
                    ensureActive()
                    val count = inputStream.read(buffer, bytesRead, chunkSize - bytesRead)
                    if (count == -1) break
                    bytesRead += count
                }
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                onProgress?.invoke(totalBytesRead)

                reader.readLine() // Read the CRLF after the chunk
            }
        } else if (contentLength != null) {
            // Handle Content-Length specified
            val buffer = ByteArray(8192) // 8KB buffer
            var bytesRead = 0
            while (totalBytesRead < contentLength && inputStream.read(buffer).also { bytesRead = it } != -1) {
                ensureActive()
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                onProgress?.invoke(totalBytesRead)
            }
        } else {
            // Handle case where neither chunked nor Content-Length is specified
            val buffer = ByteArray(8192) // 8KB buffer
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                ensureActive()
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                onProgress?.invoke(totalBytesRead)
            }
        }
    } catch (e: CancellationException) {
        throw e
    } finally {
        inputStream.close()
    }

    Triple(statusCode.toInt(), headers, outputStream.toByteArray())
}