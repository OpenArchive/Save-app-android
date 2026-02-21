package net.opendasharchive.openarchive.core.domain

data class VaultAuth(
    val vaultId: Long,
    val type: VaultType,
    val username: String,
    val secret: String
)

