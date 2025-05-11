package net.opendasharchive.openarchive.services

import android.content.Context
import android.content.Intent
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import info.guardianproject.netcipher.client.StrongOkHttpClientBuilder
import info.guardianproject.netcipher.proxy.OrbotHelper
import info.guardianproject.netcipher.proxy.OrbotHelper.SimpleStatusCallback
import net.opendasharchive.openarchive.core.infrastructure.client.enqueueResult
import net.opendasharchive.openarchive.db.Space
import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.koin.core.component.KoinComponent
import timber.log.Timber
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger

class SaveClient(private val context: Context) : SimpleStatusCallback(), KoinComponent, Call.Factory {

    private var okBuilder: OkHttpClient.Builder
    private val strongBuilder: StrongOkHttpClientBuilder

    var proxyHttpPort: Int = -1
        private set

    var proxySocksPort: Int = -1
        private set

    init {
        Logger.getLogger(OkHttpClient::class.java.name).setLevel(Level.FINE)
        okBuilder = setup()
        strongBuilder = StrongOkHttpClientBuilder.forMaxSecurity(context)
        OrbotHelper.get(context).apply {
            addStatusCallback(this@SaveClient)
            init()
        }
    }

    private fun setup(): OkHttpClient.Builder {
        val cacheInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder().addHeader("Connection", "close").build()
            chain.proceed(request)
        }

        var builder = OkHttpClient.Builder()
            .addInterceptor(cacheInterceptor)
            .connectTimeout(40L, TimeUnit.SECONDS)
            .writeTimeout(40L, TimeUnit.SECONDS)
            .readTimeout(40L, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .protocols(arrayListOf(Protocol.HTTP_1_1))

        return builder
    }

    override fun onEnabled(statusIntent: Intent?) {
        OrbotHelper.get(context).removeStatusCallback(this)

        try {
            strongBuilder.applyTo(okBuilder, statusIntent)
            proxyHttpPort = strongBuilder.getHttpPort(statusIntent)
            proxySocksPort = strongBuilder.getSocksPort(statusIntent)
        } catch (e: Exception) {
            Timber.e(e, "Error setting up OkHttp client")
        }
    }

    override fun onNotYetInstalled() {
        OrbotHelper.get(context).removeStatusCallback(this)
        okBuilder = okBuilder.proxy(null)
    }

    override fun onStatusTimeout() {
        OrbotHelper.get(context).removeStatusCallback(this)
        okBuilder = okBuilder.proxy(null)
    }

    override fun newCall(request: Request): Call {
        return okBuilder.build().newCall(request)
    }

    suspend fun enqueue(request: Request): Result<Response> {
        return okBuilder.build().enqueueResult(request)
    }

    fun execute(request: Request): Response {
        return okBuilder.build().newCall(request).execute()
    }

    fun webdav(space: Space): OkHttpSardine {
        val sardine = OkHttpSardine(okBuilder.build())
        sardine.setCredentials(space.username, space.password)
        return sardine
    }
}
