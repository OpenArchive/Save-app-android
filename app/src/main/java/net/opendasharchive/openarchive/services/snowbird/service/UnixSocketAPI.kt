package net.opendasharchive.openarchive.services.snowbird.service

import android.content.Context
import android.net.Uri
import kotlinx.serialization.Serializable
import net.opendasharchive.openarchive.services.snowbird.service.db.CreateRepoResponse
import net.opendasharchive.openarchive.services.snowbird.service.db.EmptyRequest
import net.opendasharchive.openarchive.services.snowbird.service.db.FileUploadResult
import net.opendasharchive.openarchive.services.snowbird.service.db.JoinGroupResponse
import net.opendasharchive.openarchive.services.snowbird.service.db.MembershipRequest
import net.opendasharchive.openarchive.services.snowbird.service.db.RefreshGroupResponse
import net.opendasharchive.openarchive.services.snowbird.service.db.RequestName
import net.opendasharchive.openarchive.services.snowbird.service.db.SnowbirdFileList
import net.opendasharchive.openarchive.services.snowbird.service.db.SnowbirdGroup
import net.opendasharchive.openarchive.services.snowbird.service.db.SnowbirdGroupList
import net.opendasharchive.openarchive.services.snowbird.service.db.SnowbirdRepoList
import net.opendasharchive.openarchive.extensions.createInputStream
import net.opendasharchive.openarchive.extensions.getFilename
import net.opendasharchive.openarchive.features.main.HttpMethod
import net.opendasharchive.openarchive.features.main.UnixSocketClient
import net.opendasharchive.openarchive.features.main.downloadFile
import net.opendasharchive.openarchive.features.main.uploadFile
import java.io.FileNotFoundException
import java.io.IOException

class UnixSocketAPI(private var context: Context, private var client: UnixSocketClient) :
    ISnowbirdAPI {

    companion object {
        private const val BASE_PATH = "/api"
        const val MEMBERSHIPS_PATH = "$BASE_PATH/memberships"
        const val GROUPS_PATH = "$BASE_PATH/groups"
        const val REPOS_PATH = "$BASE_PATH/groups/%s/repos"
        const val MEDIA_PATH = "$BASE_PATH/groups/%s/repos/%s/media"
        const val MEDIA_PATH_UPLOAD = "$BASE_PATH/groups/%s/repos/%s/media/%s"
        const val FORCE_REFRESH = "$BASE_PATH/groups/%s/refresh"
    }

    // Media

    override suspend fun fetchFiles(groupKey: String, repoKey: String): SnowbirdFileList {
        return client.sendRequest<EmptyRequest, net.opendasharchive.openarchive.services.snowbird.service.db.SnowbirdFileList>(
            endpoint = MEDIA_PATH.format(groupKey, repoKey),
            method = HttpMethod.GET
        )
    }

    override suspend fun downloadFile(
        groupKey: String,
        repoKey: String,
        filename: String
    ): ByteArray {
        return client.downloadFile(
            endpoint = MEDIA_PATH_UPLOAD.format(groupKey, repoKey, filename)
        )
    }

    override suspend fun uploadFile(groupKey: String, repoKey: String, uri: Uri): FileUploadResult {
        val inputStream =
            uri.createInputStream(context) ?: throw IOException("Unable to create input stream")
        val filename = uri.getFilename(context)
            ?: throw FileNotFoundException("Unable to get filename from Uri")

        return client.uploadFile<FileUploadResult>(
            endpoint = MEDIA_PATH_UPLOAD.format(groupKey, repoKey, filename),
            inputStream = inputStream
        )
    }

    // Groups

    override suspend fun createGroup(groupName: RequestName): SnowbirdGroup {
        return client.sendRequest<net.opendasharchive.openarchive.services.snowbird.service.db.RequestName, net.opendasharchive.openarchive.services.snowbird.service.db.SnowbirdGroup>(
            endpoint = GROUPS_PATH,
            method = HttpMethod.POST,
            body = groupName
        )
    }

    override suspend fun fetchGroup(key: String): SnowbirdGroup {
        return client.sendRequest<EmptyRequest, net.opendasharchive.openarchive.services.snowbird.service.db.SnowbirdGroup>(
            endpoint = "$GROUPS_PATH/$key",
            method = HttpMethod.GET
        )
    }

    override suspend fun fetchGroups(): SnowbirdGroupList {
        return client.sendRequest<EmptyRequest, net.opendasharchive.openarchive.services.snowbird.service.db.SnowbirdGroupList>(
            endpoint = GROUPS_PATH,
            method = HttpMethod.GET
        )
    }

    override suspend fun joinGroup(request: MembershipRequest): JoinGroupResponse {
        return client.sendRequest<net.opendasharchive.openarchive.services.snowbird.service.db.MembershipRequest, net.opendasharchive.openarchive.services.snowbird.service.db.JoinGroupResponse>(
            endpoint = MEMBERSHIPS_PATH,
            method = HttpMethod.POST,
            body = request
        )
    }

    override suspend fun refreshGroupContent(groupKey: String): RefreshGroupResponse {
        return client.sendRequest<EmptyRequest, net.opendasharchive.openarchive.services.snowbird.service.db.RefreshGroupResponse>(
            endpoint = FORCE_REFRESH.format(groupKey),
            method = HttpMethod.POST,
        )
    }

    override suspend fun createRepo(groupKey: String, repoName: RequestName): CreateRepoResponse {
        return client.sendRequest<net.opendasharchive.openarchive.services.snowbird.service.db.RequestName, net.opendasharchive.openarchive.services.snowbird.service.db.CreateRepoResponse>(
            endpoint = REPOS_PATH.format(groupKey),
            HttpMethod.POST,
            body = repoName
        )
    }

    override suspend fun fetchRepos(groupKey: String): SnowbirdRepoList {
        return client.sendRequest<EmptyRequest, net.opendasharchive.openarchive.services.snowbird.service.db.SnowbirdRepoList>(
            endpoint = REPOS_PATH.format(groupKey),
            method = HttpMethod.GET
        )
    }
}

@Serializable
data class ErrorResponse(
    val error: String,
    val status: String
)