package net.opendasharchive.openarchive.services.snowbird

import net.opendasharchive.openarchive.services.snowbird.service.db.SnowbirdError

sealed class SnowbirdResult<out T> {
    data class Success<out T>(val value: T) : SnowbirdResult<T>()
    data class Error(val error: SnowbirdError) : SnowbirdResult<Nothing>()
}