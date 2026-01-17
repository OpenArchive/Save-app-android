package net.opendasharchive.openarchive.services

import android.content.Context
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.services.tor.TorConstants
import net.opendasharchive.openarchive.services.tor.TorServiceManager
import net.opendasharchive.openarchive.services.webdav.BasicAuthInterceptor
import net.opendasharchive.openarchive.util.Prefs
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

/**
 * Exception thrown when Tor is enabled but not yet ready.
 */
class TorNotReadyException(message: String) : Exception(message)

/**
 * Factory for creating OkHttpClient instances with optional Tor proxy support.
 *
 * When Tor is enabled in preferences, the client will route all traffic through
 * the embedded Tor SOCKS5 proxy. The SOCKS port is dynamically allocated for
 * security reasons.
 */
object SaveClient : KoinComponent {

    private val torServiceManager: TorServiceManager by inject()

    /**
     * Creates an OkHttpClient configured for the current settings.
     *
     * @param context Application context
     * @param user Optional username for basic auth
     * @param password Optional password for basic auth
     * @return Configured OkHttpClient
     * @throws TorNotReadyException if Tor is enabled but not yet connected
     */
    suspend fun get(context: Context, user: String = "", password: String = ""): OkHttpClient {
        val cacheInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Connection", "close")
                .build()
            chain.proceed(request)
        }

        val builder = OkHttpClient.Builder()
            .addInterceptor(cacheInterceptor)
            .connectTimeout(60L, TimeUnit.SECONDS)
            .writeTimeout(60L, TimeUnit.SECONDS)
            .readTimeout(60L, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .protocols(arrayListOf(Protocol.HTTP_1_1))

        // Add basic auth interceptor if credentials provided
        if (user.isNotEmpty() || password.isNotEmpty()) {
            builder.addInterceptor(BasicAuthInterceptor(user, password))
        }

        // Apply SOCKS5 proxy when Tor is enabled
        if (Prefs.useTor) {
            val port = torServiceManager.socksPort.value

            if (port <= 0) {
                throw TorNotReadyException("Tor SOCKS port not yet available. Please wait for Tor to connect.")
            }

            builder.proxy(
                Proxy(
                    Proxy.Type.SOCKS,
                    InetSocketAddress(TorConstants.SOCKS5_PROXY_ADDRESS, port)
                )
            )
        }

        return builder.build()
    }

    /**
     * Creates a Sardine WebDAV client configured for the current settings.
     *
     * @param context Application context
     * @param space The space containing WebDAV credentials
     * @return Configured OkHttpSardine instance
     * @throws TorNotReadyException if Tor is enabled but not yet connected
     */
    suspend fun getSardine(context: Context, space: Space): OkHttpSardine {
        val sardine = OkHttpSardine(get(context))
        sardine.setCredentials(space.username, space.password)
        return sardine
    }
}
