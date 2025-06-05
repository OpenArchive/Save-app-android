package net.opendasharchive.openarchive.db

sealed class SnowbirdError: SerializableMarker {
    data class NetworkError(val code: Int, val message: String) : SnowbirdError()
    data class GeneralError(val message: String) : SnowbirdError()
    data object TimedOut : SnowbirdError()

    val friendlyMessage: String
        get() = when (this) {
            is GeneralError -> message
            is NetworkError -> message
            is TimedOut -> "The current operation took too long."
        }
}