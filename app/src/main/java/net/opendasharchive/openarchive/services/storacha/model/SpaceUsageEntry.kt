package net.opendasharchive.openarchive.services.storacha.model

data class SpaceUsageEntry(
    val spaceDid: String,
    val name: String,
    val usage: Usage,
)
