package net.opendasharchive.openarchive.services.storacha

/**
 * Represents a logged-in Storacha account
 */
data class StorachaAccount(
    val email: String,
    val sessionId: String
)