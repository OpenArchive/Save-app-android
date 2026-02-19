package net.opendasharchive.openarchive.services.storacha.util

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
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
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import java.io.File
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
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
    // Separate client for S3 uploads without logging to avoid OOM on large files
    private val s3Client = OkHttpClient
        .Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS) // 5 minutes for large file uploads
        .writeTimeout(300, TimeUnit.SECONDS) // 5 minutes for large file uploads
        .build()
    private val storachaService =
        Retrofit
            .Builder()
            .baseUrl("http://save-storacha.staging.hypha.coop:3000/")
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
        carFile: File,
        carCid: String,
        rootCid: String,
        spaceDid: String,
        userDid: String? = null,
        sessionId: String? = null,
        isAdmin: Boolean = false,
    ): BridgeUploadResult =
        withContext(Dispatchers.IO) {
            // Validate CID formats
            if (!carCid.startsWith("bag")) {
                throw IllegalArgumentException("CAR CID must start with 'bag', got: $carCid")
            }
            if (!rootCid.startsWith("bafy")) {
                throw IllegalArgumentException("Root CID must start with 'bafy', got: $rootCid")
            }

            val carSize = carFile.length()
            Timber.e("Starting bridge upload - CAR CID: $carCid, Root CID: $rootCid, Size: $carSize")

            try {
                // Step 1: Generate bridge tokens IMMEDIATELY before use
                val tokens = generateBridgeTokens(spaceDid, userDid, sessionId, isAdmin)

                // Add small delay to ensure token propagation
                kotlinx.coroutines.delay(100)

                // Step 2: Call store/add to get S3 pre-signed URL
                val storeResult =
                    storeAddWithRetry(
                        tokens,
                        spaceDid,
                        carCid,
                        carSize,
                        0,
                        userDid,
                        sessionId,
                        isAdmin,
                    )

                // Step 3: Upload to S3 (if required)
                if (storeResult.status == "upload" && storeResult.url != null) {
                    Timber.e("S3 upload required")
                    try {
                        uploadToS3(carFile, storeResult.url, storeResult.headers ?: emptyMap())
                        Timber.e("S3 upload completed")
                    } catch (s3Error: Exception) {
                        // Check if S3 error is due to expired token/URL - regenerate and retry once
                        if (s3Error.message?.contains("InvalidToken", ignoreCase = true) == true ||
                            s3Error.message?.contains("403") == true
                        ) {
                            Timber.e("S3 upload failed with token/permission error, regenerating tokens and retrying...")

                            // Regenerate tokens and get fresh S3 URL
                            val newTokens = generateBridgeTokens(spaceDid, userDid, sessionId, isAdmin)
                            kotlinx.coroutines.delay(100) // Brief delay for token propagation
                            val newStoreResult =
                                storeAddWithRetry(
                                    newTokens,
                                    spaceDid,
                                    carCid,
                                    carSize,
                                    0,
                                    userDid,
                                    sessionId,
                                    isAdmin,
                                )

                            if (newStoreResult.status == "upload" && newStoreResult.url != null) {
                                uploadToS3(
                                    carFile,
                                    newStoreResult.url,
                                    newStoreResult.headers ?: emptyMap(),
                                )
                                Timber.e("S3 upload completed after retry")
                            }
                        } else {
                            throw s3Error // Re-throw if not a token issue
                        }
                    }
                } else {
                    Timber.e("File already uploaded, status: ${storeResult.status}")
                }

                // Step 4: Register upload with upload/add
                val uploadResult =
                    uploadAddWithRetry(tokens, spaceDid, rootCid, 0, userDid, sessionId, isAdmin)
                Timber.e("Upload registered successfully")

                BridgeUploadResult(
                    rootCid = uploadResult.root.getValue("/"),
                    carCid = carCid,
                    size = carSize,
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
        isAdmin: Boolean = false,
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

        try {
            // Send sessionId only when isAdmin = true, otherwise send userDid
            val response = if (isAdmin) {
                storachaService.getBridgeTokens(null, sessionId, request)
            } else {
                storachaService.getBridgeTokens(userDid, null, request)
            }

            // Log token details for debugging
            Timber.e("Generated tokens - X-Auth-Secret length: ${response.tokens.xAuthSecret.length}")
            Timber.e("Authorization length: ${response.tokens.authorization.length}")

            return response.tokens
        } catch (e: retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string() ?: "No error details"
            val httpCode = e.code()
            val httpMessage = e.message()

            Timber.e("Token generation HTTP error: $httpCode $httpMessage")
            Timber.e("Token generation error body: $errorBody")

            // Re-throw HttpException so AuthInterceptor can handle 401/403 properly
            throw e
        }
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
        userDid: String? = null,
        sessionId: String? = null,
        isAdmin: Boolean = false,
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
                        listOf("store/add", spaceDid, storeTask),
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

                // Check for token expiration and retry once with new tokens
                if (retryCount == 0 && (errorMsg.contains("expired") || errorMsg.contains("delegation"))) {
                    Timber.e("Token expired or delegation issue, regenerating tokens and retrying...")
                    kotlinx.coroutines.delay(500) // Brief delay
                    val newTokens = generateBridgeTokens(spaceDid, userDid, sessionId, isAdmin)
                    return storeAddWithRetry(
                        newTokens,
                        spaceDid,
                        carCid,
                        carSize,
                        retryCount + 1,
                        userDid,
                        sessionId,
                        isAdmin,
                    )
                }

                throw Exception("Bridge store/add error: $errorMsg")
            }

            return storeResponse.p.out.ok!!
        } catch (e: retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string() ?: "No error details"
            val httpCode = e.code()
            val httpMessage = e.message()

            Timber.e("store/add HTTP error: $httpCode $httpMessage")
            Timber.e("store/add error body: $errorBody")

            // Check for token-related HTTP errors and retry with fresh tokens
            if (retryCount == 0 && (
                    httpCode == 401 || httpCode == 403 ||
                        errorBody.contains("InvalidToken", ignoreCase = true) ||
                        errorBody.contains("expired", ignoreCase = true) ||
                        errorBody.contains("delegation", ignoreCase = true)
                )
            ) {
                Timber.e("HTTP $httpCode suggests token issue, regenerating tokens and retrying...")
                kotlinx.coroutines.delay(500)
                val newTokens = generateBridgeTokens(spaceDid, userDid, sessionId)
                return storeAddWithRetry(
                    newTokens,
                    spaceDid,
                    carCid,
                    carSize,
                    retryCount + 1,
                    userDid,
                    sessionId,
                    isAdmin,
                )
            }

            val detailedError =
                "HTTP $httpCode: $httpMessage${if (errorBody != "No error details") "\nServer response: $errorBody" else ""}"
            throw Exception("Bridge store/add failed - $detailedError")
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
        carFile: File,
        url: String,
        headers: Map<String, String>,
    ) {
        val requestBuilder =
            Request
                .Builder()
                .url(url)
                .put(carFile.asRequestBody("application/vnd.ipld.car".toMediaType()))

        // Add explicit Content-Length header - S3 requires exact match
        requestBuilder.addHeader("Content-Length", carFile.length().toString())

        headers.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }

        // Use withTimeout for S3 upload with 5 minute timeout for large files
        val response =
            withTimeout(300_000L) {
                // 5 minutes
                suspendCancellableCoroutine { continuation ->
                    // Use s3Client without logging to avoid OOM on large files
                    val call = s3Client.newCall(requestBuilder.build())

                    call.enqueue(
                        object : okhttp3.Callback {
                            override fun onFailure(
                                call: okhttp3.Call,
                                e: java.io.IOException,
                            ) {
                                continuation.resumeWith(Result.failure(e))
                            }

                            override fun onResponse(
                                call: okhttp3.Call,
                                response: okhttp3.Response,
                            ) {
                                continuation.resumeWith(Result.success(response))
                            }
                        },
                    )

                    continuation.invokeOnCancellation {
                        call.cancel()
                    }
                }
            }

        if (!response.isSuccessful) {
            val responseBody = response.body?.string() ?: "No response body"
            val responseHeaders = response.headers.toMultimap().toString()

            Timber.e("S3 upload failed - Code: ${response.code}, Message: ${response.message}")
            Timber.e("S3 response body: $responseBody")
            Timber.e("S3 response headers: $responseHeaders")
            Timber.e("CAR file size: ${carFile.length()} bytes")

            response.close()

            val detailedError =
                "S3 upload failed - HTTP ${response.code}: ${response.message}" +
                    if (responseBody != "No response body") "\nS3 response: $responseBody" else ""
            throw Exception(detailedError)
        }
        response.close()
    }

    /**
     * Upload/add with retry logic for token expiration
     */
    private suspend fun uploadAddWithRetry(
        tokens: BridgeTokens,
        spaceDid: String,
        rootCid: String,
        retryCount: Int = 0,
        userDid: String? = null,
        sessionId: String? = null,
        isAdmin: Boolean = false,
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

                // Check for token expiration and retry once with new tokens
                if (retryCount == 0 && (errorMsg.contains("expired") || errorMsg.contains("delegation"))) {
                    Timber.e("Token expired or delegation issue in upload/add, regenerating tokens and retrying...")
                    kotlinx.coroutines.delay(500) // Brief delay
                    val newTokens = generateBridgeTokens(spaceDid, userDid, sessionId, isAdmin)
                    return uploadAddWithRetry(
                        newTokens,
                        spaceDid,
                        rootCid,
                        retryCount + 1,
                        userDid,
                        sessionId,
                        isAdmin,
                    )
                }

                throw Exception("Bridge upload/add error: $errorMsg")
            }

            return uploadResponse.p.out.ok!!
        } catch (e: retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string() ?: "No error details"
            val httpCode = e.code()
            val httpMessage = e.message()

            Timber.e("upload/add HTTP error: $httpCode $httpMessage")
            Timber.e("upload/add error body: $errorBody")

            // Check for token-related HTTP errors and retry with fresh tokens
            if (retryCount == 0 && (
                    httpCode == 401 || httpCode == 403 ||
                        errorBody.contains("InvalidToken", ignoreCase = true) ||
                        errorBody.contains("expired", ignoreCase = true) ||
                        errorBody.contains("delegation", ignoreCase = true)
                )
            ) {
                Timber.e("HTTP $httpCode suggests token issue in upload/add, regenerating tokens and retrying...")
                kotlinx.coroutines.delay(500)
                val newTokens = generateBridgeTokens(spaceDid, userDid, sessionId)
                return uploadAddWithRetry(
                    newTokens,
                    spaceDid,
                    rootCid,
                    retryCount + 1,
                    userDid,
                    sessionId,
                    isAdmin,
                )
            }

            val detailedError =
                "HTTP $httpCode: $httpMessage${if (errorBody != "No error details") "\nServer response: $errorBody" else ""}"
            throw Exception("Bridge upload/add failed - $detailedError")
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
