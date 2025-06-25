package net.opendasharchive.openarchive.services.storacha.service

import net.opendasharchive.openarchive.services.storacha.model.AccountUsageResponse
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
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface StorachaApiService {
    @POST("auth/login")
    fun login(
        @Body request: LoginRequest,
    ): Call<LoginResponse>

    @POST("auth/login/did")
    fun loginWithDid(
        @Body request: DidLoginRequest,
    ): Call<LoginResponse>

    @GET("auth/session")
    fun validateSession(
        @Header("x-session-id") sessionId: String,
    ): Call<SessionValidationResponse>

    @POST("auth/logout")
    fun logout(
        @Header("x-session-id") sessionId: String,
    ): Call<Void>

    @POST("auth/w3up/logout")
    fun logoutW3up(
        @Header("x-session-id") sessionId: String,
    ): Call<Void>

    @GET("auth/sessions")
    fun listSessions(
        @Header("x-session-id") sessionId: String,
    ): Call<List<SessionInfo>>

    @POST("auth/sessions/{id}/deactivate")
    fun deactivateSession(
        @Header("x-session-id") sessionId: String,
        @Path("id") id: String,
    ): Call<Void>

    @POST("auth/sessions/deactivate-all")
    fun deactivateAllSessions(
        @Header("x-session-id") sessionId: String,
    ): Call<Void>

    @GET("spaces")
    fun listSpaces(
        @Header("x-user-did") userDid: String,
    ): Call<List<SpaceInfo>>

    @GET("spaces/usage")
    fun getSpaceUsage(
        @Header("x-session-id") sessionId: String,
        @Query("spaceDid") spaceDid: String,
    ): Call<SpaceUsageResponse>

    @GET("spaces/account-usage")
    fun getAccountUsage(
        @Header("x-session-id") sessionId: String,
    ): Call<AccountUsageResponse>

    @GET("uploads")
    fun listUploads(
        @Header("x-user-did") userDid: String,
        @Query("spaceDid") spaceDid: String,
        @Query("cursor") cursor: String? = null,
        @Query("size") size: Int? = null,
    ): Call<UploadListResponse>

    @Multipart
    @POST("upload")
    fun uploadFile(
        @Header("x-user-did") userDid: String,
        @Part file: MultipartBody.Part,
        @Part("spaceDid") spaceDid: RequestBody,
    ): Call<UploadResponse>

    @GET("bridge-tokens")
    fun getBridgeTokens(
        @Header("x-user-did") userDid: String,
        @Query("spaceDid") spaceDid: String,
    ): Call<BridgeTokenResponse>

    @GET("delegations/user/spaces")
    fun getUserSpaces(
        @Header("x-user-did") userDid: String,
    ): Call<UserDelegationResponse>

    @GET("delegations/list")
    fun listDelegationsByUser(
        @Header("x-session-id") sessionId: String,
        @Query("userDid") userDid: String,
    ): Call<DelegationListResponse>

    @GET("delegations/list")
    fun listDelegationsBySpace(
        @Header("x-session-id") sessionId: String,
        @Query("spaceDid") spaceDid: String,
    ): Call<DelegationListResponse>

    @POST("delegations/create")
    fun createDelegation(
        @Header("x-session-id") sessionId: String,
        @Body request: DelegationRequest,
    ): Call<DelegationCreateResponse>

    @GET("delegations/get")
    fun getDelegationDetails(
        @Header("x-user-did") userDid: String,
        @Query("spaceDid") spaceDid: String,
    ): Call<DelegationDetailsResponse>

    @DELETE("delegations/revoke")
    fun revokeDelegation(
        @Header("x-session-id") sessionId: String,
        @Body request: DelegationRevokeRequest,
    ): Call<RevokeDelegationResponse>
}
