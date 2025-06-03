package net.opendasharchive.openarchive.extensions

import net.opendasharchive.openarchive.db.SnowbirdError
import net.opendasharchive.openarchive.services.snowbird.service.HttpLikeException
import retrofit2.HttpException
import java.net.SocketTimeoutException

fun Throwable.toSnowbirdError(): SnowbirdError {
    return when (this) {
        is HttpLikeException -> SnowbirdError.NetworkError(
            code = code,
            message = message
        )
        is HttpException -> SnowbirdError.NetworkError(
            code = response()?.code() ?: 0,
            message = message() ?: "HTTP Error"
        )
        is SocketTimeoutException -> SnowbirdError.TimedOut
        else -> SnowbirdError.GeneralError(message ?: "Unknown error occurred")
    }
}