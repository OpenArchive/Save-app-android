package net.opendasharchive.openarchive.services.storacha.model

data class UploadListResponse(
    val success: Boolean,
    val userDid: String,
    val spaceDid: String,
    val uploads: List<UploadEntry>,
    val count: Int,
    val cursor: String?,
    val hasMore: Boolean
)