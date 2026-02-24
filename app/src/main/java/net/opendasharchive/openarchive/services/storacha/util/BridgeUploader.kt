package net.opendasharchive.openarchive.services.storacha.util

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.services.storacha.model.BridgeUploadResult
import net.opendasharchive.openarchive.services.storacha.service.StorachaApiService
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * BridgeUploader that uploads files through the backend server.
 *
 * The Storacha bridge no longer supports the store/add capability directly.
 * Instead, uploads are routed through the backend /upload endpoint which
 * handles the complex space/blob/add workflow using the JS client.
 */
class BridgeUploader(
    private val client: OkHttpClient =
        OkHttpClient
            .Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS) // 5 minutes for large file uploads
            .writeTimeout(300, TimeUnit.SECONDS) // 5 minutes for large file uploads
            .build(),
    private val gson: Gson = Gson(),
) {
    private val storachaService =
        Retrofit
            .Builder()
            .baseUrl("http://save-storacha.staging.hypha.coop:3000/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(StorachaApiService::class.java)

    /**
     * Uploads a file to Storacha via the backend server.
     *
     * The backend handles the complex blob workflow:
     * - space/blob/add (declare blob)
     * - blob/allocate (get PUT URL)
     * - http/put (upload bytes)
     * - blob/accept (confirm upload)
     * - upload/add (register DAG root)
     *
     * @param file The original file to upload (not a CAR file)
     * @param fileName The original filename (used by backend to create UnixFS directory structure)
     * @param spaceDid The DID of the target space
     * @param userDid The user's DID (for delegated access)
     * @param sessionId The session ID (for admin access)
     * @param isAdmin Whether this is an admin upload
     * @return BridgeUploadResult with the CID and size
     */
    suspend fun uploadFile(
        file: File,
        fileName: String,
        spaceDid: String,
        userDid: String? = null,
        sessionId: String? = null,
        isAdmin: Boolean = false,
    ): BridgeUploadResult =
        withContext(Dispatchers.IO) {
            val fileSize = file.length()
            Timber.d("Starting backend upload - File: $fileName, Size: $fileSize bytes")

            try {
                // Determine content type based on file extension
                val contentType = getContentType(fileName)

                // Create multipart file part
                val filePart = MultipartBody.Part.createFormData(
                    "file",
                    fileName,
                    file.asRequestBody(contentType.toMediaType()),
                )

                // Create spaceDid part
                val spaceDidPart = spaceDid.toRequestBody("text/plain".toMediaType())

                // Create fileName part - backend uses this to wrap file in UnixFS directory
                val fileNamePart = fileName.toRequestBody("text/plain".toMediaType())

                // Upload through backend - use sessionId for admin, userDid for delegated users
                val response = if (isAdmin && sessionId != null) {
                    storachaService.uploadFile(
                        userDid = null,
                        sessionId = sessionId,
                        file = filePart,
                        spaceDid = spaceDidPart,
                        fileName = fileNamePart,
                    )
                } else {
                    storachaService.uploadFile(
                        userDid = userDid,
                        sessionId = null,
                        file = filePart,
                        spaceDid = spaceDidPart,
                        fileName = fileNamePart,
                    )
                }

                if (!response.success) {
                    throw Exception("Backend upload failed: server returned success=false")
                }

                Timber.d("Backend upload successful - CID: ${response.cid}")

                BridgeUploadResult(
                    rootCid = response.cid,
                    carCid = response.cid, // Backend returns content CID
                    size = response.size,
                )
            } catch (e: retrofit2.HttpException) {
                val errorBody = e.response()?.errorBody()?.string() ?: "No error details"
                val httpCode = e.code()
                val httpMessage = e.message()

                Timber.e("Backend upload HTTP error: $httpCode $httpMessage")
                Timber.e("Error body: $errorBody")

                throw Exception("Backend upload failed - HTTP $httpCode: $httpMessage\n$errorBody")
            } catch (e: Exception) {
                Timber.e("Backend upload failed: ${e.message}")
                throw e
            }
        }

    /**
     * Legacy method signature for backwards compatibility.
     * Now delegates to the simpler uploadFile method.
     *
     * @deprecated Use uploadFile(file, fileName, spaceDid, userDid, sessionId, isAdmin) instead
     */
    @Deprecated(
        "CAR file generation is now handled by the backend",
        ReplaceWith("uploadFile(file, fileName, spaceDid, userDid, sessionId, isAdmin)"),
    )
    suspend fun uploadFile(
        carFile: File,
        carCid: String,
        rootCid: String,
        spaceDid: String,
        userDid: String? = null,
        sessionId: String? = null,
        isAdmin: Boolean = false,
    ): BridgeUploadResult {
        // Note: carCid and rootCid are ignored as the backend generates them
        Timber.w("Using deprecated uploadFile method with CAR parameters. Consider updating to the new API.")
        return uploadFile(
            file = carFile,
            fileName = carFile.name,
            spaceDid = spaceDid,
            userDid = userDid,
            sessionId = sessionId,
            isAdmin = isAdmin,
        )
    }

    /**
     * Determines the content type based on file extension.
     */
    private fun getContentType(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return when (extension) {
            // Images
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "heic", "heif" -> "image/heic"
            "svg" -> "image/svg+xml"
            // Videos
            "mp4" -> "video/mp4"
            "mov" -> "video/quicktime"
            "avi" -> "video/x-msvideo"
            "webm" -> "video/webm"
            "mkv" -> "video/x-matroska"
            // Audio
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "ogg" -> "audio/ogg"
            "m4a" -> "audio/mp4"
            // Documents
            "pdf" -> "application/pdf"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "txt" -> "text/plain"
            "json" -> "application/json"
            // Archives
            "zip" -> "application/zip"
            "tar" -> "application/x-tar"
            "gz" -> "application/gzip"
            // Default
            else -> "application/octet-stream"
        }
    }
}
