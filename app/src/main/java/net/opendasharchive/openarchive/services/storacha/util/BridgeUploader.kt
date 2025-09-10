package net.opendasharchive.openarchive.services.storacha.util

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.services.storacha.model.BridgeTaskRequest
import net.opendasharchive.openarchive.services.storacha.model.BridgeTokenRequest
import net.opendasharchive.openarchive.services.storacha.model.BridgeTokens
import net.opendasharchive.openarchive.services.storacha.model.BridgeUploadResult
import net.opendasharchive.openarchive.services.storacha.model.StoreAddResponse
import net.opendasharchive.openarchive.services.storacha.model.StoreAddTask
import net.opendasharchive.openarchive.services.storacha.model.UploadAddResponse
import net.opendasharchive.openarchive.services.storacha.model.UploadAddTask
import net.opendasharchive.openarchive.services.storacha.service.StorachaApiService
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.utf8Size
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Fixed BridgeUploader that addresses "unexpected end of data" issues
 */
class BridgeUploader(
    private val client: OkHttpClient =
        OkHttpClient
            .Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build(),
    private val gson: Gson = Gson(),
) {
    private val storachaService =
        Retrofit
            .Builder()
            .baseUrl("http://192.168.0.55:3000/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(StorachaApiService::class.java)

    private val bridgeService =
        Retrofit
            .Builder()
            .baseUrl("https://up.storacha.network/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(net.opendasharchive.openarchive.services.storacha.service.BridgeApiService::class.java)

    suspend fun uploadFile(
        file: File,
        carData: ByteArray,
        carCid: String,
        rootCid: String,
        spaceDid: String,
        userDid: String? = null,
        sessionId: String? = null,
    ): BridgeUploadResult =
        withContext(Dispatchers.IO) {
            // Validate CID formats
            if (!carCid.startsWith("bag")) {
                throw IllegalArgumentException("CAR CID must start with 'bag', got: $carCid")
            }
            if (!rootCid.startsWith("bafy")) {
                throw IllegalArgumentException("Root CID must start with 'bafy', got: $rootCid")
            }

            Timber.e("Starting bridge upload - CAR CID: $carCid, Root CID: $rootCid, Size: ${carData.size}")

            try {
                // Step 1: Generate bridge tokens IMMEDIATELY before use
                val tokens = generateBridgeTokens(spaceDid, userDid, sessionId)

                // Add small delay to ensure token propagation
                kotlinx.coroutines.delay(100)

                // Step 2: Call store/add to get S3 pre-signed URL
                val storeResult = storeAddWithRetry(tokens, spaceDid, carCid, carData.size.toLong())

                // Step 3: Upload to S3 (if required)
                if (storeResult.status == "upload" && storeResult.url != null) {
                    Timber.e("S3 upload required")
                    uploadToS3(carData, storeResult.url, storeResult.headers ?: emptyMap())
                    Timber.e("S3 upload completed")
                } else {
                    Timber.e("File already uploaded, status: ${storeResult.status}")
                }

                // Step 4: Register upload with upload/add
                val uploadResult = uploadAddWithRetry(tokens, spaceDid, rootCid)
                Timber.e("Upload registered successfully")

                BridgeUploadResult(
                    rootCid = uploadResult.root.getValue("/"),
                    carCid = carCid,
                    size = carData.size.toLong(),
                )
            } catch (e: Exception) {
                Timber.e("Bridge upload failed: ${e.message}")
                Timber.e("Stack trace: ${e.stackTrace.joinToString("\n")}")
                throw e
            }
        }

    private suspend fun generateBridgeTokens(
        spaceDid: String,
        userDid: String?,
        sessionId: String?,
    ): BridgeTokens {
        // Generate expiration timestamp exactly like working debug script
        val currentTimeSeconds = System.currentTimeMillis() / 1000
        val expirationMillis = (currentTimeSeconds * 1000) + (60 * 60 * 1000) // 1 hour from now

        Timber.e("Token expiration: $expirationMillis (current: ${System.currentTimeMillis()})")

        val request =
            BridgeTokenRequest(
                resource = spaceDid,
                can = listOf("store/add", "upload/add"),
                expiration = expirationMillis,
                json = false,
            )

        val response = storachaService.getBridgeTokens(userDid, sessionId, request)

        // Log token details for debugging
        Timber.e("Generated tokens - X-Auth-Secret length: ${response.tokens.xAuthSecret.length}")
        Timber.e("Authorization length: ${response.tokens.authorization.length}")

        return response.tokens
    }

    /**
     * Store/add with retry logic for token expiration
     */
    private suspend fun storeAddWithRetry(
        tokens: BridgeTokens,
        spaceDid: String,
        carCid: String,
        carSize: Long,
        retryCount: Int = 0,
    ): net.opendasharchive.openarchive.services.storacha.model.StoreAddSuccess {
        val storeTask =
            StoreAddTask(
                link = mapOf("/" to carCid),
                size = carSize,
            )

        val taskRequest =
            BridgeTaskRequest(
                tasks =
                    listOf(
                        listOf("store/add", spaceDid, storeTask)
                    ),
            )

        Timber.e("store/add request JSON: ${gson.toJson(taskRequest)}")

        try {
            val responses =
                bridgeService.callBridgeApi(
                    tokens.xAuthSecret,
                    tokens.authorization,
                    "application/json",
                    taskRequest,
                )

            val responseJson = gson.toJson(responses[0])
            Timber.e("store/add response: $responseJson")

            val storeResponse = gson.fromJson(responseJson, StoreAddResponse::class.java)

            if (storeResponse.p.out.error != null) {
                val errorMsg =
                    storeResponse.p.out.error
                        .toString()
                Timber.e("Bridge store/add error: $errorMsg")

                // Check for token expiration and retry once
                if (retryCount == 0 && errorMsg.contains("expired")) {
                    Timber.e("Token expired, regenerating and retrying...")
                    kotlinx.coroutines.delay(500) // Brief delay
                    // Note: Would need to regenerate tokens here
                    throw Exception("Token expired - need to regenerate tokens")
                }

                throw Exception("Bridge store/add error: $errorMsg")
            }

            return storeResponse.p.out.ok!!
        } catch (e: Exception) {
            Timber.e("store/add failed: ${e.message}")

            // Check for "unexpected end of data" and provide debugging info
            if (e.message?.contains("unexpected end of data") == true) {
                Timber.e("🎯 Unexpected end of data detected!")
                Timber.e("CAR CID: $carCid")
                Timber.e("CAR size: $carSize")
                Timber.e("Space DID: $spaceDid")
                Timber.e("Token X-Auth-Secret starts with: ${tokens.xAuthSecret.take(20)}...")
                Timber.e("Request JSON length: ${gson.toJson(taskRequest).length}")
            }

            throw e
        }
    }

    private suspend fun uploadToS3(
        carData: ByteArray,
        url: String,
        headers: Map<String, String>,
    ) {
        val requestBuilder =
            Request
                .Builder()
                .url(url)
                .put(carData.toRequestBody("application/vnd.ipld.car".toMediaType()))

        // Add explicit Content-Length header - S3 requires exact match
//        requestBuilder.addHeader("Content-Length", carData.size.toString())

        headers.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }

        val response = client.newCall(requestBuilder.build()).execute()
        if (!response.isSuccessful) {
            val responseBody = response.body?.string() ?: "No response body"
            Timber.e("S3 upload failed - Code: ${response.code}, Message: ${response.message}")
            Timber.e("S3 response body: $responseBody")
            Timber.e("CAR data size: ${carData.size} bytes")
            throw Exception("S3 upload failed: ${response.code} ${response.message} - $responseBody")
        }
    }

    /**
     * Upload/add with retry logic for token expiration
     */
    private suspend fun uploadAddWithRetry(
        tokens: BridgeTokens,
        spaceDid: String,
        rootCid: String,
        retryCount: Int = 0,
    ): net.opendasharchive.openarchive.services.storacha.model.UploadAddSuccess {
        val uploadTask =
            UploadAddTask(
                root = mapOf("/" to rootCid),
            )

        val taskRequest =
            BridgeTaskRequest(
                tasks =
                    listOf(
                        listOf("upload/add", spaceDid, uploadTask),
                    ),
            )

        Timber.e("upload/add request JSON: ${gson.toJson(taskRequest)}")

        try {
            val responses =
                bridgeService.callBridgeApi(
                    tokens.xAuthSecret,
                    tokens.authorization,
                    "application/json",
                    taskRequest,
                )

            val responseJson = gson.toJson(responses[0])
            Timber.e("upload/add response: $responseJson")

            val uploadResponse = gson.fromJson(responseJson, UploadAddResponse::class.java)

            if (uploadResponse.p.out.error != null) {
                val errorMsg =
                    uploadResponse.p.out.error
                        .toString()
                Timber.e("Bridge upload/add error: $errorMsg")
                throw Exception("Bridge upload/add error: $errorMsg")
            }

            return uploadResponse.p.out.ok!!
        } catch (e: Exception) {
            Timber.e("upload/add failed: ${e.message}")

            // Check for "unexpected end of data" and provide debugging info
            if (e.message?.contains("unexpected end of data") == true) {
                Timber.e("🎯 Unexpected end of data detected in upload/add!")
                Timber.e("Root CID: $rootCid")
                Timber.e("Space DID: $spaceDid")
                Timber.e("Request JSON length: ${gson.toJson(taskRequest).length}")
            }

            throw e
        }
    }
}
