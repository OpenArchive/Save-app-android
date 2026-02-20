package net.opendasharchive.openarchive.core.domain

/**
 * Standard interface for authenticating and validating server connections.
 */
interface VaultAuthenticator {
    /**
     * Authenticates with the service and returns a Vault object populated with 
     * necessary keys, display names, and metadata.
     */
    suspend fun authenticate(credentials: Credentials): Result<Vault>

    /**
     * Validates an existing Vault connection (e.g., checking if keys are still valid).
     */
    suspend fun testConnection(vault: Vault): Result<Unit>
}
