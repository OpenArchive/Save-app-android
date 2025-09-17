package net.opendasharchive.openarchive.services.storacha.model

data class DelegationRevokeRequest(
    val userDid: String,
    val spaceDid: String,
)
