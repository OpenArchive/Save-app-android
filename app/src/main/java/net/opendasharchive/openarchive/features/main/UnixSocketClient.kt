package net.opendasharchive.openarchive.features.main

import android.content.Context
import android.net.LocalSocket
import android.net.LocalSocketAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.services.snowbird.service.db.SerializableMarker
import net.opendasharchive.openarchive.services.snowbird.service.ErrorResponse
import net.opendasharchive.openarchive.services.snowbird.service.HttpLikeException
import timber.log.Timber
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.SocketTimeoutException

enum class HttpMethod(val value: String) {
    GET("GET"), POST("POST"), PUT("PUT"), DELETE("DELETE"), PATCH("PATCH"),
    HEAD("HEAD"), OPTIONS("OPTIONS"), TRACE("TRACE");

    override fun toString() = value

    companion object {
        fun fromString(method: String) = entries.find { it.value.equals(method, ignoreCase = true) }
    }
}

//sealed class ClientResponse<out T> {
//    data class SuccessResponse<T>(val data: T) : ClientResponse<T>()
//    data class ErrorResponse(val error: ApiError) : ClientResponse<Nothing>()
//}

class UnixSocketClient(context: Context) {
    private val context = context
    val socketPath: String = File(context.filesDir, "rust_server.sock").absolutePath
    val json = Json { ignoreUnknownKeys = true }

    init {
        // Log the socket path for debugging
        Timber.d("Unix socket path: $socketPath")
        logFileDirectoryStatus()
    }

    private fun logFileDirectoryStatus() {
        try {
            val filesDir = context.filesDir
            Timber.d("App files directory: ${filesDir.absolutePath}")
            Timber.d("Files directory exists: ${filesDir.exists()}")
            Timber.d("Files directory readable: ${filesDir.canRead()}")
            Timber.d("Files directory writable: ${filesDir.canWrite()}")
            
            val socketFile = File(socketPath)
            Timber.d("Socket file exists: ${socketFile.exists()}")
            if (socketFile.exists()) {
                Timber.d("Socket file readable: ${socketFile.canRead()}")
                Timber.d("Socket file writable: ${socketFile.canWrite()}")
                Timber.d("Socket file size: ${socketFile.length()} bytes")
                Timber.d("Socket file last modified: ${socketFile.lastModified()}")
            }
            
            // List all files in the directory for debugging
            val files = filesDir.listFiles()
            if (files != null) {
                Timber.d("Files in app directory (${files.size} total):")
                files.forEach { file ->
                    AppLogger.d("  - ${file.name} (${if (file.isDirectory()) "dir" else "file"}, ${file.length()} bytes)")
                }
            } else {
                AppLogger.w("Unable to list files in directory: ${filesDir.absolutePath}")
            }
        } catch (e: Exception) {
            AppLogger.e("Error checking file directory status", e)
        }
    }

    private fun logConnectionDiagnostics() {
        Timber.w("Connection failed - running diagnostics:")
        logFileDirectoryStatus()
    }

    suspend inline fun <reified REQUEST : SerializableMarker, reified RESPONSE : SerializableMarker> sendRequest(
        endpoint: String,
        method: HttpMethod,
        body: REQUEST? = null
    ): RESPONSE = withContext(Dispatchers.IO) {
        Timber.d("$method $endpoint")
        sendRequestInternal(endpoint, method, body, { json.encodeToString(it) }, { json.decodeFromString<RESPONSE>(it) })
    }

    fun <REQUEST : SerializableMarker, RESPONSE : Any> sendRequestInternal(
        endpoint: String,
        method: HttpMethod,
        body: REQUEST?,
        serialize: (REQUEST) -> String,
        deserialize: (String) -> RESPONSE
    ): RESPONSE {
        return try {
            LocalSocket().use { socket ->
                socket.connect(LocalSocketAddress(socketPath, LocalSocketAddress.Namespace.FILESYSTEM))

                val (responseCode, _, responseBody) = sendJsonRequestAndGetResponse(socket, endpoint, method, body, serialize)

                Timber.d("response body = $responseBody")

                when (responseCode) {
                    in 200..299 -> parseSuccessResponse(responseBody, deserialize)
                    else -> {
                        AppLogger.e("Error response body = $responseBody")
                        // try to decode our {"error":"…","status":"error"} payload
                        val message = try {
                            json.decodeFromString<ErrorResponse>(responseBody).error
                        } catch (_: Exception) {
                            // fallback to raw body if it wasn’t JSON
                            responseBody
                        }
                        throw HttpLikeException(responseCode, message)
                    }
                }
            }
        } catch (e: SocketTimeoutException) {
            AppLogger.e("Socket timeout when connecting to Unix socket", e)
            logConnectionDiagnostics()
            throw IOException("Connection timeout to Unix socket at $socketPath. Check if the server is running.")
        } catch (e: IOException) {
            AppLogger.e("IO error when connecting to Unix socket", e)
            logConnectionDiagnostics()
            
            val errorMessage = when {
                e.message?.contains("Connection refused") == true -> 
                    "Connection refused to Unix socket at $socketPath. Server may not be running or socket file may not exist."
                e.message?.contains("No such file") == true -> 
                    "Socket file not found at $socketPath. Server may not be started yet."
                else -> 
                    "Failed to connect to Unix socket at $socketPath: ${e.message}"
            }
            throw IOException(errorMessage)
        } catch (e: Exception) {
            AppLogger.e("Unexpected error during Unix socket communication", e)
            logConnectionDiagnostics()
            throw IOException("Unexpected error during Unix socket communication: ${e.message}")
        }
    }

    private fun <REQUEST : SerializableMarker> sendJsonRequestAndGetResponse(
        socket: LocalSocket,
        endpoint: String,
        method: HttpMethod,
        body: REQUEST?,
        serialize: (REQUEST) -> String
    ): Triple<Int, Map<String, String>, String> {
        val output = socket.outputStream
        val jsonBody = body?.let { serialize(it) } ?: ""

        val requestHeaders = buildString {
            append("$method $endpoint HTTP/1.1\r\n")
            append("Content-Type: application/json\r\n")
            append("Content-Length: ${jsonBody.length}\r\n")
            //append("Connection: close\r\n")
            append("\r\n")
        }

        output.write(requestHeaders.toByteArray())
        output.write(jsonBody.toByteArray())
        output.flush()

        return readResponse(socket.inputStream)
    }

    fun readResponse(inputStream: InputStream): Triple<Int, Map<String, String>, String> {
        val reader = BufferedReader(InputStreamReader(inputStream))
        val statusLine = reader.readLine()
        val (_, statusCode, _) = statusLine.split(" ", limit = 3)

        val headers = mutableMapOf<String, String>()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            if (line.isNullOrBlank()) break
            val (key, value) = line.split(": ", limit = 2)
            headers[key] = value
        }

        val responseBody = reader.readText()

        return Triple(statusCode.toInt(), headers, responseBody)
    }

    fun <T> parseSuccessResponse(responseBody: String, deserialize: (String) -> T): T {
        return try {
            deserialize(responseBody)
        } catch (e: Exception) {
            AppLogger.e("error", e)
            throw e
        }
    }
}