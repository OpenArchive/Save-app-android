package net.opendasharchive.openarchive.services.snowbird.service

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

interface ISnowbirdAPI {
    // Media
    suspend fun fetchFiles(groupKey: String, repoKey: String): SnowbirdFileList
    suspend fun downloadFile(groupKey: String, repoKey: String, filename: String): ByteArray
    suspend fun uploadFile(groupKey: String, repoKey: String, uri: Uri): FileUploadResult

    // Groups
    suspend fun createGroup(groupName: RequestName): SnowbirdGroup
    suspend fun fetchGroup(key: String): SnowbirdGroup
    suspend fun fetchGroups(): SnowbirdGroupList
    suspend fun joinGroup(request: MembershipRequest): JoinGroupResponse
    suspend fun refreshGroupContent(groupKey: String): RefreshGroupResponse

    // Repos
    suspend fun createRepo(groupKey: String, repoName: RequestName): CreateRepoResponse
    suspend fun fetchRepos(groupKey: String): SnowbirdRepoList
}