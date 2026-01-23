package net.opendasharchive.openarchive.services.snowbird.service

import android.content.Context
import android.net.Uri
import net.opendasharchive.openarchive.services.snowbird.service.db.CreateRepoResponse
import net.opendasharchive.openarchive.services.snowbird.service.db.FileUploadResult
import net.opendasharchive.openarchive.services.snowbird.service.db.JoinGroupResponse
import net.opendasharchive.openarchive.services.snowbird.service.db.MembershipRequest
import net.opendasharchive.openarchive.services.snowbird.service.db.RefreshGroupResponse
import net.opendasharchive.openarchive.services.snowbird.service.db.RequestName
import net.opendasharchive.openarchive.services.snowbird.service.db.SnowbirdFileList
import net.opendasharchive.openarchive.services.snowbird.service.db.SnowbirdGroup
import net.opendasharchive.openarchive.services.snowbird.service.db.SnowbirdGroupList
import net.opendasharchive.openarchive.services.snowbird.service.db.SnowbirdRepoList
import net.opendasharchive.openarchive.extensions.getFilename
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source

class RetrofitAPI(private var context: Context, private val client: RetrofitClient) : ISnowbirdAPI {


    // Groups
    // Create group
    override suspend fun createGroup(groupName: RequestName): SnowbirdGroup {
        return client.createGroup(groupName)
    }

    override suspend fun fetchFiles(groupKey: String, repoKey: String): SnowbirdFileList {
        return client.fetchFiles(groupKey, repoKey)
    }

    override suspend fun downloadFile(groupKey: String, repoKey: String, filename: String): ByteArray {
        val responseBody = client.downloadFile(groupKey, repoKey, filename)
        return responseBody.bytes()
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

        return client.uploadFile(
            groupKey = groupKey,
            repoKey = repoKey,
            filename = encodedFilename,
            imageData = requestBody
        )
    }


    override suspend fun fetchGroup(key: String): SnowbirdGroup {
        return client.fetchGroup(key)
    }

    override suspend fun fetchGroups(): SnowbirdGroupList {
        return client.fetchGroups()
    }

    override suspend fun joinGroup(request: MembershipRequest): JoinGroupResponse {
        return client.joinGroup(request)
    }

    override suspend fun refreshGroupContent(groupKey: String): RefreshGroupResponse {
        return client.refreshGroup(groupKey)
    }

    override suspend fun createRepo(groupKey: String, repoName: RequestName): CreateRepoResponse {
        return client.createRepo(groupKey, repoName)
    }

    override suspend fun fetchRepos(groupKey: String): SnowbirdRepoList {
        return client.fetchRepos(groupKey)
    }

}