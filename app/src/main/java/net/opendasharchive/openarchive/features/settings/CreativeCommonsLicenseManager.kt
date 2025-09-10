package net.opendasharchive.openarchive.features.settings

import net.opendasharchive.openarchive.databinding.ContentCcBinding
import net.opendasharchive.openarchive.util.extensions.openBrowser
import net.opendasharchive.openarchive.util.extensions.styleAsLink
import net.opendasharchive.openarchive.util.extensions.toggle

object CreativeCommonsLicenseManager {

    private const val CC_DOMAIN = "creativecommons.org"
    private const val CC_LICENSE_URL_FORMAT = "https://%s/licenses/%s/4.0/"
    private const val CC0_LICENSE_URL_FORMAT = "https://%s/publicdomain/zero/1.0/"

    /**
     * Generates a Creative Commons license URL based on the provided options
     * @param ccEnabled Whether Creative Commons licensing is enabled
     * @param allowRemix Whether derivative works are allowed
     * @param requireShareAlike Whether derivative works must be shared under the same license (only applies if allowRemix is true)
     * @param allowCommercial Whether commercial use is allowed
     * @param cc0Enabled Whether CC0 (no restrictions) is enabled
     * @return The generated license URL, or null if neither CC nor CC0 is enabled
     */
    fun generateLicenseUrl(
        ccEnabled: Boolean = false,
        allowRemix: Boolean = false,
        requireShareAlike: Boolean = false,
        allowCommercial: Boolean = false,
        cc0Enabled: Boolean = false
    ): String? {
        // First check if CC is enabled at all
        if (!ccEnabled) return null
        
        // If CC is enabled and CC0 is selected, return CC0 license
        if (cc0Enabled) {
            return String.format(CC0_LICENSE_URL_FORMAT, CC_DOMAIN)
        }
        
        // Generate regular CC license
        var license = "by"
        
        if (allowRemix) {
            if (!allowCommercial) {
                license += "-nc"
            }
            if (requireShareAlike) {
                license += "-sa"
            }
        } else {
            // When remix is not allowed, ShareAlike should be automatically disabled
            if (!allowCommercial) {
                license += "-nc"
            }
            license += "-nd"
        }
        
        return String.format(CC_LICENSE_URL_FORMAT, CC_DOMAIN, license)
    }

    fun initialize(
        binding: ContentCcBinding,
        currentLicense: String? = null,
        enabled: Boolean = true,
        update: ((license: String?) -> Unit)? = null
    ) {
        configureInitialState(binding, currentLicense, enabled)

        with(binding) {
            swCcEnabled.setOnCheckedChangeListener { _, isChecked ->
                setShowLicenseOptions(binding, isChecked)
                if (!isChecked) {
                    // When main CC is disabled, reset ALL license options
                    swCc0Enabled.isChecked = false
                    swAllowRemix.isChecked = false
                    swRequireShareAlike.isChecked = false
                    swAllowCommercial.isChecked = false
                }
                val license = getSelectedLicenseUrl(binding)
                update?.invoke(license)
            }

            swCc0Enabled.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    // When CC0 is enabled, disable other options
                    swAllowRemix.isChecked = false
                    swRequireShareAlike.isChecked = false
                    swAllowCommercial.isChecked = false
                } else {
                    // When CC0 is disabled, re-enable other switches
                    swAllowRemix.isEnabled = enabled
                    swAllowCommercial.isEnabled = enabled
                }
                val license = getSelectedLicenseUrl(binding)
                update?.invoke(license)
            }

            swAllowRemix.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    swCc0Enabled.isChecked = false  // Disable CC0 when other options are enabled
                }
                swRequireShareAlike.isEnabled = isChecked
                val license = getSelectedLicenseUrl(binding)
                update?.invoke(license)
            }

            swRequireShareAlike.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    swCc0Enabled.isChecked = false  // Disable CC0 when other options are enabled
                }
                val license = getSelectedLicenseUrl(binding)
                update?.invoke(license)
            }
            
            swAllowCommercial.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    swCc0Enabled.isChecked = false  // Disable CC0 when other options are enabled
                }
                val license = getSelectedLicenseUrl(binding)
                update?.invoke(license)
            }

            tvLicenseUrl.setOnClickListener {
                it?.context?.openBrowser(tvLicenseUrl.text.toString())
            }

            btLearnMore.styleAsLink()
            btLearnMore.setOnClickListener {
                it?.context?.openBrowser("https://creativecommons.org/about/cclicenses/")
            }
        }
    }

    private fun configureInitialState(
        binding: ContentCcBinding,
        currentLicense: String?,
        enabled: Boolean = true
    ) {
        val isCc0 = currentLicense?.contains("publicdomain/zero", true) ?: false
        val isCC = currentLicense?.contains("creativecommons.org/licenses", true) ?: false
        val isActive = isCc0 || isCC

        with(binding) {
            swCcEnabled.isChecked = isActive
            setShowLicenseOptions(this, isActive)

            if (isCc0) {
                // CC0 license detected
                swCc0Enabled.isChecked = true
                swAllowRemix.isChecked = false
                swRequireShareAlike.isChecked = false
                swAllowCommercial.isChecked = false
            } else if (isCC && currentLicense != null) {
                // Regular CC license detected
                swCc0Enabled.isChecked = false
                swAllowRemix.isChecked = !(currentLicense.contains("-nd", true))
                swRequireShareAlike.isChecked = !(currentLicense.contains("-nd", true)) && currentLicense.contains("-sa", true)
                swAllowCommercial.isChecked = !(currentLicense.contains("-nc", true))
            } else {
                // No license
                swCc0Enabled.isChecked = false
                swAllowRemix.isChecked = false  // Changed from true to fix auto-enable bug
                swRequireShareAlike.isChecked = false
                swAllowCommercial.isChecked = false
            }

            swRequireShareAlike.isEnabled = swAllowRemix.isChecked
            tvLicenseUrl.text = currentLicense
            tvLicenseUrl.styleAsLink()
            
            // Set enabled states
            swCcEnabled.isEnabled = enabled
            swCc0Enabled.isEnabled = enabled
            swAllowRemix.isEnabled = enabled
            swRequireShareAlike.isEnabled = isActive && enabled && swAllowRemix.isChecked
            swAllowCommercial.isEnabled = enabled
        }
    }

    fun getSelectedLicenseUrl(cc: ContentCcBinding): String? {
        val license = generateLicenseUrl(
            ccEnabled = cc.swCcEnabled.isChecked,
            allowRemix = cc.swAllowRemix.isChecked,
            requireShareAlike = cc.swRequireShareAlike.isChecked,
            allowCommercial = cc.swAllowCommercial.isChecked,
            cc0Enabled = cc.swCc0Enabled.isChecked
        )

        // Auto-disable ShareAlike when Remix is disabled (preserve existing behavior)
        if (!cc.swAllowRemix.isChecked) {
            cc.swRequireShareAlike.isChecked = false
        }

        cc.tvLicenseUrl.text = license
        cc.tvLicenseUrl.styleAsLink()

        return license
    }

    private fun setShowLicenseOptions(binding: ContentCcBinding, isVisible: Boolean) {
        binding.rowCc0.toggle(isVisible)
        binding.rowAllowRemix.toggle(isVisible)
        binding.rowShareAlike.toggle(isVisible)
        binding.rowCommercialUse.toggle(isVisible)
        binding.tvLicenseUrl.toggle(isVisible)
    }
}