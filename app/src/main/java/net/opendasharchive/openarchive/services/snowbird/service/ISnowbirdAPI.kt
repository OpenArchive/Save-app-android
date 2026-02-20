package net.opendasharchive.openarchive.services.snowbird.service

import android.net.Uri
import net.opendasharchive.openarchive.services.snowbird.data.*

interface ISnowbirdAPI {
    // Media
    suspend fun fetchFiles(groupKey: String, repoKey: String): SnowbirdFileListDTO
    suspend fun downloadFile(groupKey: String, repoKey: String, filename: String): ByteArray
    suspend fun uploadFile(groupKey: String, repoKey: String, uri: Uri): FileUploadResult

    // Groups
    suspend fun createGroup(groupName: RequestName): SnowbirdGroupDTO
    suspend fun fetchGroup(key: String): SnowbirdGroupDTO
    suspend fun fetchGroups(): SnowbirdGroupListDTO
    suspend fun joinGroup(request: MembershipRequest): JoinGroupResponse
    suspend fun refreshGroupContent(groupKey: String): RefreshGroupResponse

    // Repos
    suspend fun createRepo(groupKey: String, repoName: RequestName): CreateRepoResponse
    suspend fun fetchRepos(groupKey: String): SnowbirdRepoListDTO
}