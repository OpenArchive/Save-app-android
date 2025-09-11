package net.opendasharchive.openarchive.services.storacha.model

data class RevokeDelegationResponse(
    val message: String,
    val userDid: String,
    val spaceDid: String,
    val revokedCount: Int
)