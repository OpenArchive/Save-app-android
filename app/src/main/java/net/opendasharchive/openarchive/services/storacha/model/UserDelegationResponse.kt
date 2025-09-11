package net.opendasharchive.openarchive.services.storacha.model

data class UserDelegationResponse(
    val userDid: String,
    val spaces: List<String>,
    val expiresAt: String?
)