package net.opendasharchive.openarchive.services.storacha.model

data class LoginResponse(
    val message: String,
    val sessionId: String,
    val did: String,
    val verified: Boolean,
    val challenge: String? = null,
    val challengeId: String? = null,
    val spaces: List<SpaceInfo>? = null,
)
