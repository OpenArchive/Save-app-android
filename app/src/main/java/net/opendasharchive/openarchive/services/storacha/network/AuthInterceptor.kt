package net.opendasharchive.openarchive.services.storacha.network

import kotlinx.coroutines.runBlocking
import net.opendasharchive.openarchive.services.storacha.util.SessionManager
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import timber.log.Timber

/**
 * OkHttp interceptor that handles 401 Unauthorized and 403 "Session not verified" responses.
 * Simply returns 401 to trigger login flow - no auto-refresh.
 *
 * Uses lazy initialization to break circular dependency with SessionManager.
 */
class AuthInterceptor(
    private val sessionManagerProvider: () -> SessionManager,
) : Interceptor {
    private val sessionManager by lazy { sessionManagerProvider() }

    companion object {
        private const val TAG = "AuthInterceptor"
    }

    /**
     * Checks if a response contains "Session not verified" message
     */
    private fun isSessionNotVerifiedError(response: Response): Boolean =
        try {
            val errorBody = response.peekBody(Long.MAX_VALUE).string()
            errorBody.contains("Session not verified", ignoreCase = true)
        } catch (_: Exception) {
            false
        }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Skip intercepting auth endpoints to prevent infinite loops
        val path = originalRequest.url.encodedPath
        if (path.contains("/auth/")) {
            return chain.proceed(originalRequest)
        }

        val originalResponse = chain.proceed(originalRequest)

        // Handle 401 or 403 "Session not verified" - just return 401 to trigger login
        if (originalResponse.code == 401 || (
                originalResponse.code == 403 &&
                    isSessionNotVerifiedError(originalResponse)
            )
        ) {
            Timber.tag(TAG).d("Received ${originalResponse.code} - sending to login")
            // Just return the 401, let the UI handle it
            return originalResponse
        }

        // Return the original response for all other cases
        return originalResponse
    }
}
