package net.opendasharchive.openarchive.services.snowbird.service

import net.opendasharchive.openarchive.db.ApiError

sealed class ApiResponse<out T> {
    data class SingleResponse<T>(val data: T) : ApiResponse<T>()
    data class ListResponse<T>(val data: List<T>) : ApiResponse<T>()
    data class ErrorResponse(val error: ApiError) : ApiResponse<Nothing>()
}