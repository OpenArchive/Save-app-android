package net.opendasharchive.openarchive.services.storacha.model

data class UploadResponse(
    val success: Boolean,
    val cid: String,
    val size: Long,
)
