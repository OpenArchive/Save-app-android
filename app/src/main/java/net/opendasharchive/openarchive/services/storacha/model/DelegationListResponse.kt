package net.opendasharchive.openarchive.services.storacha.model

data class DelegationListResponse(
    val userDid: String? = null,
    val spaceDid: String? = null,
    val users: List<String>? = null,
    val spaces: List<String>? = null
)