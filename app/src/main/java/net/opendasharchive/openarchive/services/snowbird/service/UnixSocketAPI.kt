package net.opendasharchive.openarchive.services.snowbird.service

import android.content.Context
import android.net.Uri
import net.opendasharchive.openarchive.db.EmptyRequest
import net.opendasharchive.openarchive.db.FileUploadResult
import net.opendasharchive.openarchive.db.JoinGroupResponse
import net.opendasharchive.openarchive.db.MembershipRequest
import net.opendasharchive.openarchive.db.RequestName
import net.opendasharchive.openarchive.db.SnowbirdFileList
import net.opendasharchive.openarchive.db.SnowbirdGroup
import net.opendasharchive.openarchive.db.SnowbirdGroupList
import net.opendasharchive.openarchive.db.SnowbirdRepo
import net.opendasharchive.openarchive.db.SnowbirdRepoList
import net.opendasharchive.openarchive.extensions.createInputStream
import net.opendasharchive.openarchive.extensions.getFilename
import net.opendasharchive.openarchive.features.main.HttpMethod
import net.opendasharchive.openarchive.features.main.UnixSocketClient
import net.opendasharchive.openarchive.features.main.downloadFile
import net.opendasharchive.openarchive.features.main.uploadFile
import java.io.FileNotFoundException
import java.io.IOException

class UnixSocketAPI(private var context: Context, private var client: UnixSocketClient): ISnowbirdAPI {

    companion object {
        private const val BASE_PATH = "/api"
        const val MEMBERSHIPS_PATH = "$BASE_PATH/memberships"
        const val GROUPS_PATH = "$BASE_PATH/groups"
        const val REPOS_PATH = "$BASE_PATH/groups/%s/repos"
        const val MEDIA_PATH = "$BASE_PATH/groups/%s/repos/%s/media"
        const val MEDIA_PATH_UPLOAD = "$BASE_PATH/groups/%s/repos/%s/media/%s"
    }

    override suspend fun downloadFile(groupKey: String, repoKey: String, filename: String): ByteArray {
       return client.downloadFile(
           endpoint = MEDIA_PATH_UPLOAD.format(groupKey, repoKey, filename))
    }

    override suspend fun fetchFiles(groupKey: String, repoKey: String): SnowbirdFileList {
        return client.sendRequest<EmptyRequest, SnowbirdFileList>(
            endpoint = MEDIA_PATH.format(groupKey, repoKey),
            method = HttpMethod.GET)
    }

    override suspend fun uploadFile(groupKey: String, repoKey: String, uri: Uri): FileUploadResult {
        val inputStream = uri.createInputStream(context) ?: throw IOException("Unable to create input stream")
        val filename = uri.getFilename(context) ?: throw FileNotFoundException("Unable to get filename from Uri")

        return client.uploadFile<FileUploadResult>(
            endpoint = MEDIA_PATH_UPLOAD.format(groupKey, repoKey, filename),
            inputStream = inputStream)
    }

    override suspend fun createGroup(groupName: RequestName): SnowbirdGroup {
        return client.sendRequest<RequestName, SnowbirdGroup>(
            endpoint = GROUPS_PATH,
            method = HttpMethod.POST,
            body = groupName)
    }

    override suspend fun fetchGroup(key: String): SnowbirdGroup {
        return client.sendRequest<EmptyRequest, SnowbirdGroup>(
            endpoint = "$GROUPS_PATH/$key",
            method = HttpMethod.GET)
    }

    override suspend fun fetchGroups(): SnowbirdGroupList {
        return client.sendRequest<EmptyRequest, SnowbirdGroupList>(
            endpoint = GROUPS_PATH,
            method = HttpMethod.GET)
    }

    override suspend fun joinGroup(request: MembershipRequest): JoinGroupResponse {
        return client.sendRequest<MembershipRequest, JoinGroupResponse>(
            endpoint = MEMBERSHIPS_PATH,
            method = HttpMethod.POST,
            body = request)
    }

    override suspend fun createRepo(groupKey: String, repoName: RequestName): SnowbirdRepo {
        return client.sendRequest<RequestName, SnowbirdRepo>(
            endpoint = REPOS_PATH.format(groupKey),
            HttpMethod.POST,
            body = repoName)
    }

    override suspend fun fetchRepos(groupKey: String): SnowbirdRepoList {
        return client.sendRequest<EmptyRequest, SnowbirdRepoList>(
            endpoint = REPOS_PATH.format(groupKey),
            method = HttpMethod.GET)
    }
}