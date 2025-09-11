package net.opendasharchive.openarchive.services.storacha.model

data class VerifyRequest(
    val did: String,
    val challengeId: String,
    val signature: String,
    val sessionId: String,
    val email: String? = null
)