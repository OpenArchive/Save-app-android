package net.opendasharchive.openarchive.services.storacha.service

import net.opendasharchive.openarchive.services.storacha.model.BridgeTaskRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface BridgeApiService {
    @POST("bridge")
    suspend fun callBridgeApi(
        @Header("X-Auth-Secret") xAuthSecret: String,
        @Header("Authorization") authorization: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Body request: BridgeTaskRequest,
    ): List<Map<String, Any>>
    
    @POST("bridge")
    suspend fun callBridgeApiRaw(
        @Header("X-Auth-Secret") xAuthSecret: String,
        @Header("Authorization") authorization: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Body request: BridgeTaskRequest,
    ): Response<ResponseBody>
}