package net.opendasharchive.openarchive.core.security

interface VaultCredentialStore {
    suspend fun putSecret(vaultId: Long, secret: String)
    suspend fun getSecret(vaultId: Long): String?
    suspend fun hasSecret(vaultId: Long): Boolean
    suspend fun deleteSecret(vaultId: Long)
}

