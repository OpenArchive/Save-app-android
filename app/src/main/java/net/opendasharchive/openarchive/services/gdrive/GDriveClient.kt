package net.opendasharchive.openarchive.services.gdrive

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.InputStreamContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.services.SaveClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

private const val BASE_API = "https://www.googleapis.com/drive/v3/files"

class GDriveClient(private val client: SaveClient, private val credential: GoogleAccountCredential) {
    suspend fun getAccessToken(credential: GoogleAccountCredential): String {
        return withContext(Dispatchers.IO) {
            credential.token
        }
    }

    suspend fun newFolder(name: String, parent: String? = null): Result<String> {
        val jsonBody = """
{
    "name": "$name",
    "mimeType": "application/vnd.google-apps.folder"
    ${if (parent != null) ", \"parents\": [\"$parent\"]" else ""}
}
""".trimIndent()

        val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

        val token = getAccessToken(credential)

        val request = Request.Builder()
            .url(BASE_API)
            .addHeader("Authorization", "Bearer $token")
            .post(requestBody)
            .build()

        return client.enqueue(request).mapCatching { response ->
            response.body?.string() ?: throw IOException("No response body")
        }
    }

    suspend fun listFolders(parents: String? = null, pageSize: Int = 1000, pageToken: String? = null): Result<String> {
        val token = getAccessToken(credential)
        val request = Request.Builder()
            .url("$BASE_API?q=mimeType='application/vnd.google-apps.folder'${if (parents != null) " and '$parents' in parents" else ""} and trashed=false&pageSize=$pageSize&fields=files(id,name,modifiedTime),nextPageToken${if (pageToken != null) "&pageToken=$pageToken" else ""}")
            .addHeader("Authorization", "Bearer $token")
            .build()

        return client.enqueue(request).mapCatching { response ->
            response.body?.string() ?: throw IOException("No response body")
        }
    }

    suspend fun upload(file: GDriveFile, content: InputStreamContent, onProgress: ProgressListener): Result<String> {
        val requestBody = ProgressRequestBody(content) { bytesWritten, contentLength ->
            val progress = (bytesWritten.toFloat() / contentLength.toFloat()) * 100
            onProgress.onProgressUpdate(bytesWritten, progress.toInt())
        }

        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, requestBody)
            .build()

        val token = getAccessToken(credential)

        val request = Request.Builder()
            .url("$BASE_API?uploadType=multipart")
            .header("Authorization", "Bearer $token")
            .post(multipartBody)
            .build()

        return client.enqueue(request).mapCatching { response ->
            response.body?.string() ?: throw IOException("No response body")
        }
    }

}