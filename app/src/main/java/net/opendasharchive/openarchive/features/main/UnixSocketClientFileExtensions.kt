package net.opendasharchive.openarchive.features.main

import android.net.LocalSocket
import android.net.LocalSocketAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.services.snowbird.service.db.SerializableMarker
import net.opendasharchive.openarchive.services.snowbird.service.HttpLikeException
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.SocketTimeoutException


suspend fun UnixSocketClient.downloadFile(endpoint: String): ByteArray = withContext(Dispatchers.IO) {
    LocalSocket().use { socket ->
        socket.connect(LocalSocketAddress(socketPath, LocalSocketAddress.Namespace.FILESYSTEM))

        // 1) build & send a perfectly raw HTTP/1.1 GET
        val req = buildString {
            append("GET $endpoint HTTP/1.1\r\n")
            append("Accept: image/*\r\n")
            append("Connection: close\r\n")
            append("\r\n")
        }.toByteArray()
        socket.outputStream.write(req)
        socket.outputStream.flush()

        // 2) now read response entirely over the InputStream
        val input = socket.inputStream
        val (statusCode, headers) = input.readHttpResponseHeaders()
        if (statusCode !in 200..299) throw HttpLikeException(statusCode, "Failed to download file: $statusCode")

        // 3) decide how to read the body
        return@withContext when {
            headers["Transfer-Encoding"]?.equals("chunked", true) == true ->
                input.readChunkedBody()
            headers["Content-Length"] != null ->
                input.readFixedLengthBody(headers["Content-Length"]!!.toInt())
            else ->
                input.readAllCompat()   // maybe not ideal for huge files, but safe
        }
    }
}

private fun InputStream.readAllCompat(): ByteArray {
    val buffer = ByteArrayOutputStream()
    this.use { input ->
        input.copyTo(buffer)
    }
    return buffer.toByteArray()
}

suspend fun UnixSocketClient.downloadFileOld(endpoint: String): ByteArray = withContext(Dispatchers.IO) {
    LocalSocket().use { socket ->
        socket.connect(LocalSocketAddress(socketPath, LocalSocketAddress.Namespace.FILESYSTEM))

        val requestHeaders = buildString {
            append("${HttpMethod.GET} $endpoint HTTP/1.1\r\n")
            append("Accept: image/*\r\n")
            append("\r\n")
        }

        socket.outputStream.apply {
            write(requestHeaders.toByteArray())
            flush()
        }

        try {
            val (responseCode, _, bytes) = readBinaryResponseWithCancellation(socket.inputStream)

            Timber.d("File download response code: $responseCode")

            when (responseCode) {
                in 200..299 -> bytes
                else -> throw HttpLikeException(responseCode, "Failed to download file: $responseCode")
            }
        } catch (e: SocketTimeoutException) {
            throw e
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw IOException("Unexpected error during Unix socket communication: ${e.message}")
        }
    }
}

suspend inline fun <reified RESPONSE : SerializableMarker> UnixSocketClient.uploadFile(
    endpoint: String,
    imageData: ByteArray
): RESPONSE = withContext(Dispatchers.IO) {
    try {
        LocalSocket().use { socket ->
            socket.connect(LocalSocketAddress(socketPath, LocalSocketAddress.Namespace.FILESYSTEM))

            val requestHeaders = buildString {
                append("${HttpMethod.POST} $endpoint HTTP/1.1\r\n")
                append("Content-Type: application/octet-stream\r\n")
                append("Content-Length: ${imageData.size}\r\n")
                append("\r\n")
            }

            socket.outputStream.apply {
                write(requestHeaders.toByteArray())
                write(imageData)
                flush()
            }

            val (responseCode, _, responseBody) = readResponse(socket.inputStream)

            Timber.d("Image upload response code: $responseCode")

            when (responseCode) {
                in 200..299 -> parseSuccessResponse(responseBody) { json.decodeFromString<RESPONSE>(it) }
                else -> throw HttpLikeException(responseCode, "Failed to upload file: $responseCode")
            }
        }
    } catch (e: SocketTimeoutException) {
        throw e
    } catch (e: IOException) {
        throw e
    } catch (e: Exception) {
        throw IOException("Unexpected error during Unix socket communication: ${e.message}")
    }
}

suspend inline fun <reified RESPONSE : SerializableMarker> UnixSocketClient.uploadFile(
    endpoint: String,
    inputStream: InputStream,
): RESPONSE {
    inputStream.use { stream ->
        return uploadFile<RESPONSE>(endpoint, stream.readBytes())
    }
}