package net.opendasharchive.openarchive.core.domain

sealed class DomainError {
    abstract val message: String

    data class Network(override val message: String, val code: Int? = null) : DomainError()
    data class Server(override val message: String, val code: Int? = null) : DomainError()
    data class Timeout(override val message: String = "The operation timed out.") : DomainError()
    data class Unknown(override val message: String) : DomainError()

    val friendlyMessage: String
        get() = message
}
