package net.opendasharchive.openarchive.services.webdav

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.folders.Folder
import net.opendasharchive.openarchive.services.SaveClient
import net.opendasharchive.openarchive.services.SaveClientFactory
import okhttp3.Request
import java.io.IOException
import java.util.Date
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class WebDavRepository(
    private val context: Context,
    private val saveClientFactory: SaveClientFactory
) {
    suspend fun testConnection(space: Space) = withContext(Dispatchers.IO) {
        val url = space.hostUrl ?: throw IOException("400 Bad Request")

        val client = saveClientFactory.createClient(space.username, space.password)

        val request = Request.Builder()
            .url(url)
            .method("GET", null)
            .addHeader("OCS-APIRequest", "true")
            .addHeader("Accept", "application/json")
            .build()

        suspendCoroutine { continuation ->
            client.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    continuation.resumeWithException(e)
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    val code = response.code
                    val message = response.message
                    response.close()

                    if (code != 200 && code != 204) {
                        continuation.resumeWithException(IOException("$code $message"))
                    } else {
                        continuation.resume(Unit)
                    }
                }
            })
        }
    }

    @Throws(IOException::class)
    suspend fun getFolders(space: Space): List<Folder> = withContext(Dispatchers.IO) {
        val root = space.hostUrl?.encodedPath

        SaveClient.getSardine(context, space).list(space.host)?.mapNotNull {
            if (it?.isDirectory == true && it.path != root) {
                Folder(it.name, it.modified ?: Date())
            } else {
                null
            }
        } ?: emptyList()
    }
}
