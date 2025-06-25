package net.opendasharchive.openarchive.services.storacha.model

data class LoginResponse(
    val message: String,
    val sessionId: String,
    val did: String
)