package net.opendasharchive.openarchive.services.storacha.model

data class SessionValidationResponse(
    val valid: Boolean,
    val expiresAt: String,
    val message: String
)