package net.opendasharchive.openarchive.services.storacha.model

data class AccountUsageResponse(
    val totalUsage: Usage,
    val spaces: List<SpaceUsageEntry>,
    val planProduct: String? = null, // e.g., "did:web:starter.web3.storage"
)
