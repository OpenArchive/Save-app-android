package net.opendasharchive.openarchive.core.domain

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.Locale

/**
 * Vault - Domain representation of a server connection or account.
 * (Formerly known as Space)
 */
@Serializable
data class Vault(
    val id: Long = 0L,
    val type: VaultType,
    val name: String = "",
    val username: String = "",
    val displayName: String = "",
    val password: String = "",
    val host: String = "",
    val metaData: String = "",
    val licenseUrl: String? = null,
    val vaultKey: String? = null,
    val createdAt: LocalDateTime? = null
) {
    val friendlyName: String
        get() {
            if (name.isNotBlank()) {
                return name
            }
            return hostUrl?.host ?: name
        }

    val initial: String
        get() = (friendlyName.firstOrNull() ?: 'X').uppercase(Locale.getDefault())

    val hostUrl: HttpUrl?
        get() = host.toHttpUrlOrNull()
}

/**
 * VaultType - Types of supported backends.
 */
@Serializable
enum class VaultType(val id: Int, val friendlyName: String) {
    PRIVATE_SERVER(0, "Private Server"),
    INTERNET_ARCHIVE(1, "Internet Archive"),
    DWEB_STORAGE(5, "DWeb Storage"),
    STORACHA(7, "Storacha Service")
}
