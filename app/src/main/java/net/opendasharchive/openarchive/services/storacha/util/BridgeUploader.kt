package net.opendasharchive.openarchive.services.storacha.util

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class BridgeUploader(
    private val client: OkHttpClient = OkHttpClient(),
) {
    fun fetchBridgeTokens(
        userDid: String,
        spaceDid: String,
    ): JSONObject {
        val url = "https://your-token-service.com/bridge-tokens?spaceDid=$spaceDid"
        val request =
            Request
                .Builder()
                .url(url)
                .addHeader("x-user-did", userDid)
                .build()
        val response = client.newCall(request).execute()
        return JSONObject(response.body?.string() ?: "")
    }

    fun uploadCarFile(
        carData: ByteArray,
        authHeaders: JSONObject,
    ): JSONObject {
        val request =
            Request
                .Builder()
                .url("https://up.storacha.network/bridge")
                .post(carData.toRequestBody("application/vnd.ipld.car".toMediaType()))
                .addHeader("X-Auth-Secret", authHeaders.getString("X-Auth-Secret"))
                .addHeader("Authorization", authHeaders.getString("Authorization"))
                .build()
        val response = client.newCall(request).execute()
        return JSONObject(response.body?.string() ?: "")
    }
}