package net.opendasharchive.openarchive.services.webdav.data

import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.core.domain.Credentials
import net.opendasharchive.openarchive.core.domain.Vault
import net.opendasharchive.openarchive.core.domain.VaultAuthenticator
import net.opendasharchive.openarchive.core.domain.VaultType
import net.opendasharchive.openarchive.services.SaveClientFactory
import okhttp3.Request
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class WebDavAuthenticator(
    private val saveClientFactory: SaveClientFactory
) : VaultAuthenticator {

    companion object {
        private const val REMOTE_PHP_ADDRESS = "/remote.php/webdav/"
    }

    override suspend fun authenticate(credentials: Credentials): Result<Vault> {
        if (credentials !is Credentials.WebDav) {
            return Result.failure(IllegalArgumentException("Invalid credentials type"))
        }

        val fixedUrl = fixSpaceUrl(credentials.url) ?: return Result.failure(IllegalArgumentException("Invalid URL"))
        
        val tempVault = Vault(
            type = VaultType.PRIVATE_SERVER,
            host = fixedUrl.toString(),
            username = credentials.user,
            password = credentials.pass
        )

        return testConnection(tempVault).map { tempVault }
    }

    override suspend fun testConnection(vault: Vault): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = vault.host
            val client = saveClientFactory.createClient(vault.username, vault.password)

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
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun fixSpaceUrl(url: String?): android.net.Uri? {
        if (url.isNullOrBlank()) return null

        val uri = url.toUri()
        val builder = uri.buildUpon()

        if (uri.scheme != "https") {
            builder.scheme("https")
        }

        if (uri.authority.isNullOrBlank()) {
            builder.authority(uri.path)
            builder.path(REMOTE_PHP_ADDRESS)
        } else if (uri.path.isNullOrBlank() || uri.path == "/") {
            builder.path(REMOTE_PHP_ADDRESS)
        }

        return builder.build()
    }
}
