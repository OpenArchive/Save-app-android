package net.opendasharchive.openarchive.services.storacha.model

data class BridgeUploadResult(
    val rootCid: String,
    val carCid: String,
    val size: Long,
)
