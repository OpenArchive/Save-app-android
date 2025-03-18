package net.opendasharchive.openarchive.db

import kotlinx.serialization.Serializable

@Serializable
sealed class ApiError: SerializableMarker {
    @Serializable
    data class HttpError(val code: Int, val message: String) : ApiError()

    @Serializable
    data class NetworkError(val message: String) : ApiError()

    @Serializable
    data class ServerError(val message: String) : ApiError()

    @Serializable
    data class ClientError(val message: String) : ApiError()

    @Serializable
    data class UnexpectedError(val message: String) : ApiError()

    @Serializable
    data object Unauthorized : ApiError()

    @Serializable
    data object ResourceNotFound : ApiError()

    @Serializable
    data object TimedOut : ApiError()

    @Serializable
    data object None : ApiError()

    val friendlyMessage: String
        get() = when (this) {
            is HttpError -> "HTTP Error $code: $message"
            is NetworkError -> message
            is ServerError -> message
            is ClientError -> message
            is UnexpectedError -> message
            Unauthorized -> "Unauthorized: Please log in and try again"
            ResourceNotFound -> "The requested resource was not found"
            TimedOut -> "The request timed out"
            None -> "No error"
        }
}