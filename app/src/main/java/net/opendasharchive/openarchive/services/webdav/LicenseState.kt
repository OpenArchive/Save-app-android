package net.opendasharchive.openarchive.services.webdav

import androidx.compose.runtime.Immutable

@Immutable
data class LicenseState(
    val ccEnabled: Boolean = false,
    val allowRemix: Boolean = true,
    val requireShareAlike: Boolean = false,
    val allowCommercial: Boolean = false,
    val licenseUrl: String? = null
)

interface LicenseCallbacks {
    fun onCcEnabledChange(enabled: Boolean)
    fun onAllowRemixChange(allowed: Boolean)
    fun onRequireShareAlikeChange(required: Boolean)
    fun onAllowCommercialChange(allowed: Boolean)
}