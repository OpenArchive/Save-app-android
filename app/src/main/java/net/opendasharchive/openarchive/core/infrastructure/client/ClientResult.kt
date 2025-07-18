package net.opendasharchive.openarchive.core.infrastructure.client

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun <T> OkHttpClient.enqueueResult(
    request: Request,
    onResume: (Response) -> T
) = suspendCancellableCoroutine { continuation ->
    newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            continuation.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            continuation.resume(onResume(response))
        }
    })

    continuation.invokeOnCancellation {
        dispatcher.cancelAll()
    }
}
