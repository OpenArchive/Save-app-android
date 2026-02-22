package net.opendasharchive.openarchive.services.snowbird.service

import android.content.Context
import android.net.Uri
import kotlinx.serialization.json.Json
import net.opendasharchive.openarchive.extensions.getFilename
import net.opendasharchive.openarchive.services.snowbird.data.FileUploadResult
import net.opendasharchive.openarchive.services.snowbird.data.JoinGroupResponse
import net.opendasharchive.openarchive.services.snowbird.data.MembershipRequest
import net.opendasharchive.openarchive.services.snowbird.data.RefreshGroupResponse
import net.opendasharchive.openarchive.services.snowbird.data.RequestName
import net.opendasharchive.openarchive.services.snowbird.data.SnowbirdFileListDTO
import net.opendasharchive.openarchive.services.snowbird.data.SnowbirdGroupDTO
import net.opendasharchive.openarchive.services.snowbird.data.SnowbirdGroupListDTO
import net.opendasharchive.openarchive.services.snowbird.data.SnowbirdRepoDTO
import net.opendasharchive.openarchive.services.snowbird.data.SnowbirdRepoListDTO
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import retrofit2.Response
import java.io.IOException

class RetrofitAPI(private var context: Context, private val client: RetrofitClient) : ISnowbirdAPI {

    private val json = Json { ignoreUnknownKeys = true }

    private fun ensureServerReadyForNetwork() {
        if (SnowbirdService.getCurrentStatus() !is ServiceStatus.Connected) {
            throw IOException("DWeb server is not ready. Enable DWeb Server and wait for it to connect.")
        }
    }

    private suspend fun <T> safeApiCall(call: suspend () -> Response<T>): T {
        try {
            ensureServerReadyForNetwork()
            val response = call()
            if (response.isSuccessful) {
                return response.body() ?: throw IOException("Empty response body")
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = parseError(errorBody) ?: "Unknown error occurred (Code: ${response.code()})"
                throw IOException(errorMessage)
            }
        } catch (e: Exception) {
            if (e is IOException) throw e
            throw IOException("Network error: ${e.localizedMessage ?: "Unknown error"}", e)
        }
    }

    private fun parseError(errorBody: String?): String? {
        if (errorBody == null) return null
        return try {
            // Try to parse as JSON string (it's often quoted like "Error message")
            json.decodeFromString<String>(errorBody)
        } catch (e: Exception) {
            // If it's not a valid JSON string, just return it as is (stripped of whitespace)
            errorBody.trim().removeSurrounding("\"")
        }
    }

    // Groups
    // Create group
    override suspend fun createGroup(groupName: RequestName): SnowbirdGroupDTO {
        return safeApiCall { client.createGroup(groupName) }
    }

    override suspend fun fetchFiles(groupKey: String, repoKey: String): SnowbirdFileListDTO {
        return safeApiCall { client.fetchFiles(groupKey, repoKey) }
    }

    override suspend fun downloadFile(groupKey: String, repoKey: String, filename: String): ByteArray {
        try {
            ensureServerReadyForNetwork()
            val responseBody = client.downloadFile(groupKey, repoKey, filename)
            return responseBody.bytes()
        } catch (e: Exception) {
            if (e is IOException) throw e
            throw IOException("Network error: ${e.localizedMessage ?: "Unknown error"}", e)
        }
    }

    override suspend fun uploadFile(groupKey: String, repoKey: String, uri: Uri): FileUploadResult {
        val resolver = context.contentResolver
        val mediaType = resolver.getType(uri)?.toMediaTypeOrNull() ?: "application/octet-stream".toMediaType()
        val length = resolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
            val l = afd.length
            if (l > 0) l else -1L
        } ?: -1L

        val requestBody = object : RequestBody() {
            override fun contentType() = mediaType
            override fun contentLength(): Long = length
            override fun writeTo(sink: BufferedSink) {
                resolver.openInputStream(uri)?.use { input ->
                    sink.writeAll(input.source())
                }
            }
        }

        // Encode for the path segment Rust expects as {file_name}
        val encodedFilename = Uri.encode(uri.getFilename(context) ?: "upload.bin")

        return safeApiCall {
            client.uploadFile(
                groupKey = groupKey,
                repoKey = repoKey,
                filename = encodedFilename,
                imageData = requestBody
            )
        }
    }


    override suspend fun fetchGroup(key: String): SnowbirdGroupDTO {
        return safeApiCall { client.fetchGroup(key) }
    }

    override suspend fun fetchGroups(): SnowbirdGroupListDTO {
        return safeApiCall { client.fetchGroups() }
    }

    override suspend fun joinGroup(request: MembershipRequest): JoinGroupResponse {
        return safeApiCall { client.joinGroup(request) }
    }

    override suspend fun refreshGroupContent(groupKey: String): RefreshGroupResponse {
        return safeApiCall { client.refreshGroup(groupKey) }
    }

    override suspend fun createRepo(groupKey: String, repoName: RequestName): SnowbirdRepoDTO {
        return safeApiCall { client.createRepo(groupKey, repoName) }
    }

    override suspend fun fetchRepos(groupKey: String): SnowbirdRepoListDTO {
        return safeApiCall { client.fetchRepos(groupKey) }
    }

}
