package net.opendasharchive.openarchive.services.storacha.model

data class BridgeTokenResponse(
    val success: Boolean,
    val tokens: BridgeTokens,
)

data class BridgeTokens(
    val xAuthSecret: String,
    val authorization: String,
)
