package net.opendasharchive.openarchive.services.storacha.model

data class AccountUsageResponse(
    val totalUsage: Usage,
    val spaces: List<SpaceUsageEntry>
)