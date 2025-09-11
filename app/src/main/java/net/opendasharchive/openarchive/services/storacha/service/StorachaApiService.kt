package net.opendasharchive.openarchive.services.storacha.service

import net.opendasharchive.openarchive.services.storacha.model.AccountUsageResponse
import net.opendasharchive.openarchive.services.storacha.model.BridgeTokenRequest
import net.opendasharchive.openarchive.services.storacha.model.BridgeTokenResponse
import net.opendasharchive.openarchive.services.storacha.model.DelegationCreateResponse
import net.opendasharchive.openarchive.services.storacha.model.DelegationDetailsResponse
import net.opendasharchive.openarchive.services.storacha.model.DelegationListResponse
import net.opendasharchive.openarchive.services.storacha.model.DelegationRequest
import net.opendasharchive.openarchive.services.storacha.model.DelegationRevokeRequest
import net.opendasharchive.openarchive.services.storacha.model.DidLoginRequest
import net.opendasharchive.openarchive.services.storacha.model.LoginRequest
import net.opendasharchive.openarchive.services.storacha.model.LoginResponse
import net.opendasharchive.openarchive.services.storacha.model.RevokeDelegationResponse
import net.opendasharchive.openarchive.services.storacha.model.SessionInfo
import net.opendasharchive.openarchive.services.storacha.model.SessionValidationResponse
import net.opendasharchive.openarchive.services.storacha.model.SpaceInfo
import net.opendasharchive.openarchive.services.storacha.model.SpaceUsageResponse
import net.opendasharchive.openarchive.services.storacha.model.UploadListResponse
import net.opendasharchive.openarchive.services.storacha.model.UploadResponse
import net.opendasharchive.openarchive.services.storacha.model.UserDelegationResponse
import net.opendasharchive.openarchive.services.storacha.model.VerifyRequest
import net.opendasharchive.openarchive.services.storacha.model.VerifyResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface StorachaApiService {
    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest,
    ): LoginResponse

    @POST("auth/login/did")
    suspend fun loginWithDid(
        @Body request: DidLoginRequest,
    ): LoginResponse

    @POST("auth/verify")
    suspend fun verify(
        @Body request: VerifyRequest,
    ): VerifyResponse

    @GET("auth/session")
    suspend fun validateSession(
        @Header("x-session-id") sessionId: String,
    ): SessionValidationResponse

    @POST("auth/logout")
    suspend fun logout(
        @Header("x-session-id") sessionId: String,
    ): Void

    @POST("auth/w3up/logout")
    suspend fun logoutW3up(
        @Header("x-session-id") sessionId: String,
    ): Void

    @GET("auth/sessions")
    suspend fun listSessions(
        @Header("x-session-id") sessionId: String,
    ): List<SessionInfo>

    @POST("auth/sessions/{id}/deactivate")
    suspend fun deactivateSession(
        @Header("x-session-id") sessionId: String,
        @Path("id") id: String,
    ): Void

    @POST("auth/sessions/deactivate-all")
    suspend fun deactivateAllSessions(
        @Header("x-session-id") sessionId: String,
    ): Void

    @GET("spaces")
    suspend fun listSpaces(
        @Header("x-user-did") userDid: String,
        @Header("x-session-id") sessionId: String,
    ): List<SpaceInfo>

    @GET("spaces/usage")
    suspend fun getSpaceUsage(
        @Header("x-session-id") sessionId: String,
        @Header("x-user-did") userDid: String,
        @Query("spaceDid") spaceDid: String,
    ): SpaceUsageResponse

    @GET("spaces/account-usage")
    suspend fun getAccountUsage(
        @Header("x-session-id") sessionId: String,
    ): AccountUsageResponse

    @GET("uploads")
    suspend fun listUploads(
        @Header("x-user-did") userDid: String,
        @Header("x-session-id") sessionId: String,
        @Query("spaceDid") spaceDid: String,
        @Query("cursor") cursor: String? = null,
        @Query("size") size: Int? = null,
    ): UploadListResponse

    @Multipart
    @POST("upload")
    suspend fun uploadFile(
        @Header("x-user-did") userDid: String,
        @Part file: MultipartBody.Part,
        @Part("spaceDid") spaceDid: RequestBody,
    ): UploadResponse

    @POST("bridge-tokens")
    suspend fun getBridgeTokens(
        @Header("x-user-did") userDid: String?,
        @Header("x-session-id") sessionId: String?,
        @Body request: BridgeTokenRequest,
    ): BridgeTokenResponse

    @GET("delegations/user/spaces")
    suspend fun getUserSpaces(
        @Header("x-user-did") userDid: String,
    ): UserDelegationResponse

    @GET("delegations/list")
    suspend fun listDelegationsByUser(
        @Header("x-session-id") sessionId: String,
        @Query("userDid") userDid: String,
    ): DelegationListResponse

    @GET("delegations/list")
    suspend fun listDelegationsBySpace(
        @Header("x-session-id") sessionId: String,
        @Query("spaceDid") spaceDid: String,
    ): DelegationListResponse

    @POST("delegations/create")
    suspend fun createDelegation(
        @Header("x-session-id") sessionId: String,
        @Body request: DelegationRequest,
    ): DelegationCreateResponse

    @GET("delegations/get")
    suspend fun getDelegationDetails(
        @Header("x-user-did") userDid: String,
        @Query("spaceDid") spaceDid: String,
    ): DelegationDetailsResponse

    @HTTP(method = "DELETE", path = "delegations/revoke", hasBody = true)
    suspend fun revokeDelegation(
        @Header("x-session-id") sessionId: String,
        @Body request: DelegationRevokeRequest,
    ): RevokeDelegationResponse
}
