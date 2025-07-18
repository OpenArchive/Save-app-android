package net.opendasharchive.openarchive.services.webdav

import okhttp3.Credentials.basic
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.Throws

class BasicAuthInterceptor(user: String?, password: String?) : Interceptor {
    private val credentials: String
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request: Request = chain.request()
        val authenticatedRequest = request.newBuilder()
            .header("Authorization", credentials).build()
        return chain.proceed(authenticatedRequest)
    }
    init {
        credentials = basic(user!!, password!!)
    }
}