package net.opendasharchive.openarchive.core.domain

import kotlinx.serialization.Serializable

@Serializable
sealed class Credentials {
    @Serializable
    data class WebDav(val url: String, val user: String, val pass: String) : Credentials()

    @Serializable
    data class InternetArchive(val email: String, val pass: String) : Credentials()
}
