package net.opendasharchive.openarchive.services.storacha.model

data class DelegationRequest(
    val userDid: String,
    val spaceDid: String,
    val expiresIn: Int = 24,
)
