package net.opendasharchive.openarchive.services

import android.content.Context
import android.content.Intent
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import info.guardianproject.netcipher.client.StrongBuilder
import info.guardianproject.netcipher.client.StrongBuilderBase
import info.guardianproject.netcipher.proxy.OrbotHelper
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.services.webdav.BasicAuthInterceptor
import net.opendasharchive.openarchive.util.Prefs
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.internal.platform.Platform
import java.util.concurrent.TimeUnit
import kotlin.coroutines.suspendCoroutine

class SaveClient(context: Context) : StrongBuilderBase<SaveClient, OkHttpClient>(context) {

    class OrbotException(message: String): Exception(message)

    private var okBuilder: OkHttpClient.Builder

    init {
        val cacheInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder().addHeader("Connection", "close").build()
            chain.proceed(request)
        }

        okBuilder = OkHttpClient.Builder()
            .addInterceptor(cacheInterceptor)
            .connectTimeout(40L, TimeUnit.SECONDS)
            .writeTimeout(40L, TimeUnit.SECONDS)
            .readTimeout(40L, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .protocols(arrayListOf(Protocol.HTTP_1_1))
    }

    /**
     * OkHttp3 [does not support SOCKS proxies.](https://github.com/square/okhttp/issues/2315)
     *
     * @return false
     */
    override fun supportsSocksProxy(): Boolean {
        return false
    }

    /**
     * {@inheritDoc}
     */
    override fun build(status: Intent): OkHttpClient {
        if (!status.hasExtra(OrbotHelper.EXTRA_STATUS)) {
            status.putExtra(OrbotHelper.EXTRA_STATUS, OrbotHelper.STATUS_OFF)
        }

        return applyTo(okBuilder, status).build()
    }

    /**
     * Adds NetCipher configuration to an existing OkHttpClient.Builder,
     * in case you have additional configuration that you wish to
     * perform.
     *
     * @param builder a new or partially-configured OkHttpClient.Builder
     * @return the same builder
     */
    private fun applyTo(builder: OkHttpClient.Builder, status: Intent?): OkHttpClient.Builder {
        val factory = buildSocketFactory()

        if (factory != null) {
            val trustManager = Platform.get().trustManager(factory)

            if (trustManager != null) {
                builder.sslSocketFactory(factory, trustManager)
            }
        }

        return builder
            .proxy(buildProxy(status))
    }

    @Throws(Exception::class)
    override fun get(status: Intent, connection: OkHttpClient, url: String): String? {
        val request: Request = Request.Builder().url(TOR_CHECK_URL).build()

        return connection.newCall(request).execute().body?.string()
    }

    companion object {
        suspend fun get(context: Context, user: String = "", password: String = ""): OkHttpClient {

            val strongBuilder = SaveClient(context)

            if (user.isNotEmpty() || password.isNotEmpty()) {
                strongBuilder.okBuilder.addInterceptor(BasicAuthInterceptor(user, password))
            }

            return suspendCoroutine {
                val callback = object : StrongBuilder.Callback<OkHttpClient?> {
                    override fun onConnected(connection: OkHttpClient?) {
                        val result = if (connection != null) {
                            Result.success(connection)
                        }
                        else {
                            Result.failure(OrbotException(context.getString(R.string.tor_connection_exception)))
                        }

                        it.resumeWith(result)
                    }

                    override fun onConnectionException(e: java.lang.Exception?) {
                        it.resumeWith(Result.failure(e ?: OrbotException(context.getString(R.string.tor_connection_exception))))
                    }

                    override fun onTimeout() {
                        it.resumeWith(Result.failure(OrbotException(context.getString(R.string.tor_connection_timeout))))
                    }

                    override fun onInvalid() {
                        it.resumeWith(Result.failure(OrbotException(context.getString(R.string.tor_connection_invalid))))
                    }
                }

                if (Prefs.useTor) {
                    if (!OrbotHelper.requestStartTor(context)) {
                        callback.onInvalid()
                    }
                    else {
                        strongBuilder.build(callback)
                    }
                }
                else {
                    callback.onConnected(strongBuilder.build(Intent()))
                }
            }
        }

        suspend fun getSardine(context: Context, space: Space): OkHttpSardine {
            val sardine = OkHttpSardine(get(context))
            sardine.setCredentials(space.username, space.password)

            return sardine
        }
    }
}
