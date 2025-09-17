package net.opendasharchive.openarchive.services.storacha.model

data class DelegationDetailsResponse(
    val userDid: String,
    val spaceDid: String,
    val delegationCar: String,
    val expiresAt: String,
)
