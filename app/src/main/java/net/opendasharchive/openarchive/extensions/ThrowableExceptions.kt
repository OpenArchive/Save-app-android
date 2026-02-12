package net.opendasharchive.openarchive.extensions

import net.opendasharchive.openarchive.core.domain.DomainError
import net.opendasharchive.openarchive.services.snowbird.service.HttpLikeException
import retrofit2.HttpException
import java.net.SocketTimeoutException

fun Throwable.toDomainError(): DomainError {
    return when (this) {
        is HttpLikeException -> DomainError.Network(
            code = code,
            message = message
        )
        is HttpException -> DomainError.Server(
            code = response()?.code() ?: 0,
            message = message() ?: "HTTP Error"
        )
        is SocketTimeoutException -> DomainError.Timeout()
        else -> DomainError.Unknown(message ?: "Unknown error occurred")
    }
}