package net.opendasharchive.openarchive.services.storacha.model

data class VerifyResponse(
    val sessionId: String,
    val did: String,
    val message: String
)