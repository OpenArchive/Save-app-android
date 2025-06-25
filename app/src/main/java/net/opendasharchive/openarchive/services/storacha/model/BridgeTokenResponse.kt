package net.opendasharchive.openarchive.services.storacha.model

data class BridgeTokenResponse(
    val headers: Map<String, String>,
    val curlCommand: String,
    val note: String
)