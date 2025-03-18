package net.opendasharchive.openarchive.services.snowbird

import net.opendasharchive.openarchive.db.SnowbirdError

sealed class SnowbirdResult<out T> {
    data class Success<out T>(val value: T) : SnowbirdResult<T>()
    data class Error(val error: SnowbirdError) : SnowbirdResult<Nothing>()
}