package net.opendasharchive.openarchive.features.settings

import net.opendasharchive.openarchive.databinding.ContentCcBinding
import net.opendasharchive.openarchive.util.extensions.openBrowser
import net.opendasharchive.openarchive.util.extensions.styleAsLink
import net.opendasharchive.openarchive.util.extensions.toggle

object CreativeCommonsLicenseManager {

    private const val CC_DOMAIN = "creativecommons.org"
    private const val CC_LICENSE_URL_FORMAT = "https://%s/licenses/%s/4.0/"

    /**
     * Generates a Creative Commons license URL based on the provided options
     * @param ccEnabled Whether Creative Commons licensing is enabled
     * @param allowRemix Whether derivative works are allowed
     * @param requireShareAlike Whether derivative works must be shared under the same license (only applies if allowRemix is true)
     * @param allowCommercial Whether commercial use is allowed
     * @return The generated license URL, or null if CC is not enabled
     */
    fun generateLicenseUrl(
        ccEnabled: Boolean,
        allowRemix: Boolean,
        requireShareAlike: Boolean,
        allowCommercial: Boolean
    ): String? {
        if (!ccEnabled) return null
        
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
                val license = getSelectedLicenseUrl(binding)
                update?.invoke(license)
            }

            swAllowRemix.setOnCheckedChangeListener { _, isChecked ->
                swRequireShareAlike.isEnabled = isChecked
                val license = getSelectedLicenseUrl(binding)
                update?.invoke(license)
            }

            swRequireShareAlike.setOnCheckedChangeListener { _, _ ->
                val license = getSelectedLicenseUrl(binding)
                update?.invoke(license)
            }
            swAllowCommercial.setOnCheckedChangeListener { _, _ ->
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
        val isActive = currentLicense?.contains(CC_DOMAIN, true) ?: false

        with(binding) {
            swCcEnabled.isChecked = isActive
            setShowLicenseOptions(this, isActive)

            swAllowRemix.isChecked = isActive && !(currentLicense?.contains("-nd", true) ?: false)
            swRequireShareAlike.isEnabled = binding.swAllowRemix.isChecked
            swRequireShareAlike.isChecked = isActive && binding.swAllowRemix.isChecked && currentLicense?.contains("-sa", true) ?: false
            swAllowCommercial.isChecked = isActive && !(currentLicense?.contains("-nc", true) ?: false)
            tvLicenseUrl.text = currentLicense
            tvLicenseUrl.styleAsLink()
            swCcEnabled.isEnabled = enabled
            swAllowRemix.isEnabled =  enabled
            swRequireShareAlike.isEnabled = isActive && enabled && swAllowRemix.isEnabled
            swAllowCommercial.isEnabled = enabled
        }
    }

    fun getSelectedLicenseUrl(cc: ContentCcBinding): String? {
        val license = generateLicenseUrl(
            ccEnabled = cc.swCcEnabled.isChecked,
            allowRemix = cc.swAllowRemix.isChecked,
            requireShareAlike = cc.swRequireShareAlike.isChecked,
            allowCommercial = cc.swAllowCommercial.isChecked
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
        binding.rowAllowRemix.toggle(isVisible)
        binding.rowShareAlike.toggle(isVisible)
        binding.rowCommercialUse.toggle(isVisible)
        binding.tvLicenseUrl.toggle(isVisible)
    }
}