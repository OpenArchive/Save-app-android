package net.opendasharchive.openarchive.services.storacha.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class BridgeUploader(
    private val client: OkHttpClient = OkHttpClient(),
) {
    suspend fun fetchBridgeTokens(
        userDid: String,
        spaceDid: String,
        sessionId: String,
    ): JSONObject =
        withContext(Dispatchers.IO) {
            val url = "http://192.168.0.55:3000/bridge-tokens?spaceDid=$spaceDid"
            val request =
                Request
                    .Builder()
                    .url(url)
                    .addHeader("x-user-did", userDid)
                    .addHeader("x-session-id", sessionId)
                    .build()
            val response = client.newCall(request).execute()
            JSONObject(response.body?.string() ?: "")
        }

    suspend fun uploadCarFile(
        carData: ByteArray,
        authHeaders: JSONObject,
    ): JSONObject =
        withContext(Dispatchers.IO) {
            val headers = authHeaders.getJSONObject("headers")
            val request =
                Request
                    .Builder()
                    .url("https://up.storacha.network/bridge")
                    .post(carData.toRequestBody("application/vnd.ipld.car".toMediaType()))
                    .addHeader("X-Auth-Secret", headers.getString("X-Auth-Secret"))
                    .addHeader("Authorization", headers.getString("Authorization"))
                    .build()
            val response = client.newCall(request).execute()
            JSONObject(response.body?.string() ?: "")
        }
}
