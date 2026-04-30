package net.opendasharchive.openarchive.services.snowbird.service

import net.opendasharchive.openarchive.services.snowbird.data.*
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
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
    ): Response<SnowbirdFileListDTO>

    @GET("groups/{groupKey}/repos/{repoKey}/media/{filename}")
    suspend fun downloadFile(
        @Path("groupKey") groupKey: String,
        @Path("repoKey") repoKey: String,
        @Path("filename") filename: String
    ): Response<ResponseBody>

    @POST("groups/{groupKey}/repos/{repoKey}/media/{filename}")
    @Headers("Content-Type: application/octet-stream")
    suspend fun uploadFile(
        @Path("groupKey") groupKey: String,
        @Path("repoKey") repoKey: String,
        @Path(value = "filename", encoded = true) filename: String,
        @Body imageData: RequestBody
    ): Response<FileUploadResult>

    // Groups

    @POST("groups")
    suspend fun createGroup(
        @Body groupName: RequestName
    ): Response<SnowbirdGroupDTO>

    @GET("groups/{groupKey}")
    suspend fun fetchGroup(
        @Path("groupKey") groupKey: String
    ): Response<SnowbirdGroupDTO>

    @GET("groups")
    suspend fun fetchGroups(): Response<SnowbirdGroupListDTO>

    @POST("memberships")
    suspend fun joinGroup(
        @Body request: MembershipRequest
    ): Response<JoinGroupResponse>

    @POST("groups/{group_id}/refresh")
    suspend fun refreshGroup(
        @Path("group_id") groupKey: String
    ): Response<RefreshGroupResponse>

    // Repos

    @POST("groups/{groupKey}/repos")
    suspend fun createRepo(
        @Path("groupKey") groupKey: String,
        @Body repoName: RequestName
    ): Response<SnowbirdRepoDTO>

    @GET("groups/{groupKey}/repos")
    suspend fun fetchRepos(
        @Path("groupKey") groupKey: String
    ): Response<SnowbirdRepoListDTO>
}
