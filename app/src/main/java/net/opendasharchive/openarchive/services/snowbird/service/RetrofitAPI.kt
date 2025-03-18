package net.opendasharchive.openarchive.services.snowbird.service

import android.content.Context
import android.net.Uri
import net.opendasharchive.openarchive.db.FileUploadResult
import net.opendasharchive.openarchive.db.JoinGroupResponse
import net.opendasharchive.openarchive.db.MembershipRequest
import net.opendasharchive.openarchive.db.RequestName
import net.opendasharchive.openarchive.db.SnowbirdFileList
import net.opendasharchive.openarchive.db.SnowbirdGroup
import net.opendasharchive.openarchive.db.SnowbirdGroupList
import net.opendasharchive.openarchive.db.SnowbirdRepo
import net.opendasharchive.openarchive.db.SnowbirdRepoList
import net.opendasharchive.openarchive.extensions.getFilename

class RetrofitAPI(private var context: Context, private val client: RetrofitClient) : ISnowbirdAPI {
    override suspend fun fetchFiles(groupKey: String, repoKey: String): SnowbirdFileList {
        return client.fetchFiles(groupKey, repoKey)
    }

    override suspend fun downloadFile(groupKey: String, repoKey: String, filename: String): ByteArray {
        return client.downloadFile(groupKey, repoKey, filename)
    }

    override suspend fun uploadFile(groupKey: String, repoKey: String, uri: Uri): FileUploadResult {
        return client.uploadFile(groupKey, repoKey, uri.getFilename(context)!!)
    }

    override suspend fun createGroup(groupName: RequestName): SnowbirdGroup {
        return client.createGroup(groupName)
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

    override suspend fun createRepo(groupKey: String, repoName: RequestName): SnowbirdRepo {
        return client.createRepo(groupKey, repoName)
    }

    override suspend fun fetchRepos(groupKey: String): SnowbirdRepoList {
        return client.fetchRepos(groupKey)
    }

}