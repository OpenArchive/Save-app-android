package net.opendasharchive.openarchive.services.storacha.model

data class Usage(
    val bytes: Long,
    val mb: Double,
    val human: String,
)
