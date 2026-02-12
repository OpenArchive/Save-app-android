package net.opendasharchive.openarchive.services.snowbird


sealed class SnowbirdResult<out T> {
    data class Success<out T>(val value: T) : SnowbirdResult<T>()
    data class Error(val error: SnowbirdError) : SnowbirdResult<Nothing>()
}