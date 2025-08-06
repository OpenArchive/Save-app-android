package net.opendasharchive.openarchive.services.storacha.model

data class SessionValidationResponse(
    val valid: Boolean,
    val verified: Int,
    val expiresAt: String,
    val message: String
)
