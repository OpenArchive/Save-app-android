package net.opendasharchive.openarchive.services.storacha.model

data class DelegationCreateResponse(
    val message: String,
    val principalDid: String,
    val delegationCid: String,
    val expiresAt: String,
    val createdBy: String,
)
