package net.opendasharchive.openarchive.services.snowbird.service

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

    // Repos
    suspend fun createRepo(groupKey: String, repoName: RequestName): SnowbirdRepo
    suspend fun fetchRepos(groupKey: String): SnowbirdRepoList
}