package net.opendasharchive.openarchive.services.storacha.model

data class BridgeTokenRequest(
    val resource: String,
    val can: List<String>,
    val expiration: Long,
    val json: Boolean = false
)