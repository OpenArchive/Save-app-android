package net.opendasharchive.openarchive.services.storacha.model

data class UploadEntry(
    val cid: String,
    val size: Long,
    val created: String,
    val insertedAt: String,
    val updatedAt: String,
    val gatewayUrl: String,
)
