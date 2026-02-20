package net.opendasharchive.openarchive.core.domain

sealed class DomainResult<out T> {
    data class Success<out T>(val data: T) : DomainResult<T>()
    data class Error(val error: DomainError) : DomainResult<Nothing>()
}

inline fun <T> DomainResult<T>.valueOr(alternative: (DomainResult.Error) -> T): T {
    return when (this) {
        is DomainResult.Error -> alternative(this)
        is DomainResult.Success -> this.data
    }
}
