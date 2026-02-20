package net.opendasharchive.openarchive.services

import android.content.Context
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import net.opendasharchive.openarchive.services.tor.TorConstants
import net.opendasharchive.openarchive.services.tor.TorServiceManager
import net.opendasharchive.openarchive.services.common.auth.BasicAuthInterceptor
import net.opendasharchive.openarchive.util.Prefs
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.net.Authenticator
import java.net.InetSocketAddress
import java.net.PasswordAuthentication
import java.net.Proxy
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

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
 *
 * SECURITY: Uses IsolateSOCKSAuth for circuit isolation - each client gets
 * a unique session ID which results in a separate Tor circuit.
 */
object SaveClient : KoinComponent {

    private val torServiceManager: TorServiceManager by inject()

    /** Thread-safe holder for current SOCKS auth session */
    private val currentSessionId = AtomicReference<String>(null)

    /**
     * SOCKS5 Authenticator for circuit isolation.
     *
     * When Tor is configured with IsolateSOCKSAuth, each unique username/password
     * combination gets a separate Tor circuit. This prevents correlation of
     * different requests through the same circuit.
     */
    private val socksAuthenticator = object : Authenticator() {
        override fun getPasswordAuthentication(): PasswordAuthentication? {
            return if (requestorType == RequestorType.PROXY) {
                val sessionId = currentSessionId.get() ?: return null
                // Use session ID as both username and password
                // Tor only cares that different values = different circuits
                PasswordAuthentication(sessionId, sessionId.toCharArray())
            } else {
                null
            }
        }
    }

    init {
        // Set the default authenticator for SOCKS proxy authentication
        Authenticator.setDefault(socksAuthenticator)
    }

    /**
     * Generates a unique session ID for circuit isolation.
     * Each session ID will result in a separate Tor circuit.
     */
    private fun generateSessionId(): String {
        return UUID.randomUUID().toString()
    }

    /**
     * Creates an OkHttpClient configured for the current settings.
     *
     * @param context Application context
     * @param user Optional username for basic auth
     * @param password Optional password for basic auth
     * @param isolateCircuit If true, generates a new session ID for circuit isolation
     * @return Configured OkHttpClient
     * @throws TorNotReadyException if Tor is enabled but not yet connected
     */
    suspend fun get(
        context: Context,
        user: String = "",
        password: String = "",
        isolateCircuit: Boolean = true
    ): OkHttpClient {
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
            if (!torServiceManager.isReady()) {
                throw TorNotReadyException("Tor is not yet connected. Please wait for Tor to connect.")
            }

            val port = torServiceManager.socksPort.value

            // Generate new session ID for circuit isolation
            if (isolateCircuit) {
                currentSessionId.set(generateSessionId())
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
    suspend fun getSardine(context: Context, user: String, pass: String): OkHttpSardine {
        val sardine = OkHttpSardine(get(context))
        sardine.setCredentials(user, pass)
        return sardine
    }
}
