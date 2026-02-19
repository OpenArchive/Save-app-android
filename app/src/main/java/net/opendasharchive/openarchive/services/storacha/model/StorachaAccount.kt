package net.opendasharchive.openarchive.services.storacha.model

/**
 * Represents a logged-in Storacha account
 */
data class StorachaAccount(
    val email: String,
    val sessionId: String,
    val isVerified: Boolean = false,
    val did: String? = null,
)