package net.opendasharchive.openarchive.services.snowbird.service

import net.opendasharchive.openarchive.db.FileUploadResult
import net.opendasharchive.openarchive.db.JoinGroupResponse
import net.opendasharchive.openarchive.db.MembershipRequest
import net.opendasharchive.openarchive.db.RequestName
import net.opendasharchive.openarchive.db.SnowbirdFileList
import net.opendasharchive.openarchive.db.SnowbirdGroup
import net.opendasharchive.openarchive.db.SnowbirdGroupList
import net.opendasharchive.openarchive.db.SnowbirdRepo
import net.opendasharchive.openarchive.db.SnowbirdRepoList
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

interface RetrofitClient {

    // Files

    @GET("groups/{groupKey}/repos/{repoKey}/media")
    suspend fun fetchFiles(
        @Path("groupKey") groupKey: String,
        @Path("repoKey") repoKey: String
    ): SnowbirdFileList

    @GET("groups/{groupKey}/repos/{repoKey}/media/{filename}")
    suspend fun downloadFile(
        @Path("groupKey") groupKey: String,
        @Path("repoKey") repoKey: String,
        @Path("filename") filename: String
    ): ByteArray

    @POST("groups/{groupKey}/repos/{repoKey}/media/{filename}")
    @Headers("Content-Type: application/octet-stream")
    suspend fun uploadFile(
        @Path("groupKey") groupKey: String,
        @Path("repoKey") repoKey: String,
        @Path("filename") filename: String,
        // @Body imageData: RequestBody
    ): FileUploadResult

    // Groups

    @POST("groups")
    suspend fun createGroup(
        @Body groupName: RequestName
    ): SnowbirdGroup

    @GET("groups/{groupKey}")
    suspend fun fetchGroup(
        @Path("groupKey") groupKey: String
    ): SnowbirdGroup

    @GET("groups")
    suspend fun fetchGroups(): SnowbirdGroupList

    @POST("memberships")
    suspend fun joinGroup(
        @Body request: MembershipRequest
    ): JoinGroupResponse

    // Repos

    @POST("groups/{groupKey}/repos")
    suspend fun createRepo(
        @Path("groupKey") groupKey: String,
        @Body repoName: RequestName
    ): SnowbirdRepo

    @GET("groups/{groupKey}/repos")
    suspend fun fetchRepos(
        @Path("groupKey") groupKey: String
    ): SnowbirdRepoList
}