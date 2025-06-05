package net.opendasharchive.openarchive.extensions

import net.opendasharchive.openarchive.db.SnowbirdError
import retrofit2.HttpException
import java.net.SocketTimeoutException

fun Throwable.toSnowbirdError(): SnowbirdError {
    return when (this) {
        is HttpException -> SnowbirdError.NetworkError(
            code = response()?.code() ?: 0,
            message = message() ?: "HTTP Error"
        )
        is SocketTimeoutException -> SnowbirdError.TimedOut
        else -> SnowbirdError.GeneralError(message ?: "Unknown error occurred")
    }
}