package net.opendasharchive.openarchive.services

import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import net.opendasharchive.openarchive.core.infrastructure.client.enqueueResult
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.services.tor.ITorRepository
import net.opendasharchive.openarchive.services.tor.TorStatus
import net.opendasharchive.openarchive.util.Prefs
import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

class SaveClientProxyBuilder() : KoinComponent {
    private val torRepository: ITorRepository by inject(named("tor"))
    private val builder: OkHttpClient.Builder

    init {
        val cacheInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder().addHeader("Connection", "close").build()
            chain.proceed(request)
        }

        builder = OkHttpClient.Builder()
            .addInterceptor(cacheInterceptor)
            .connectTimeout(40L, TimeUnit.SECONDS)
            .writeTimeout(40L, TimeUnit.SECONDS)
            .readTimeout(40L, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .protocols(arrayListOf(Protocol.HTTP_1_1))
    }

    private fun withTor(): SaveClientProxyBuilder {
        builder.proxy(Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", 9150)))
        return this
    }

    fun build(): OkHttpClient {
        if (Prefs.useTor) {
            withTor()
        }
        return builder.build()
    }

}

class SaveClient(
    private val builder: SaveClientProxyBuilder =  SaveClientProxyBuilder()
) : Call.Factory {

    override fun newCall(request: Request): Call {
        return builder.build().newCall(request)
    }

    suspend fun enqueue(request: Request): Result<Response> {
        return builder.build().enqueueResult(request)
    }

    fun execute(request: Request): Response {
        return builder.build().newCall(request).execute()
    }

    fun webdav(space: Space): OkHttpSardine {
        val sardine = OkHttpSardine(builder.build())
        sardine.setCredentials(space.username, space.password)
        return sardine
    }
}
