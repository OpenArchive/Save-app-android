package net.opendasharchive.openarchive.features.settings

import net.opendasharchive.openarchive.databinding.ContentCcBinding
import net.opendasharchive.openarchive.util.extensions.openBrowser
import net.opendasharchive.openarchive.util.extensions.styleAsLink
import net.opendasharchive.openarchive.util.extensions.toggle

object CreativeCommonsLicenseManager {

    private const val CC_DOMAIN = "creativecommons.org"
    private const val CC_LICENSE_URL_FORMAT = "https://%s/licenses/%s/4.0/"

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
            swAllowRemix.isEnabled = enabled
            swRequireShareAlike.isEnabled = enabled
            swAllowCommercial.isEnabled = enabled
        }
    }

    fun getSelectedLicenseUrl(cc: ContentCcBinding): String? {
        var license: String? = null

        if (cc.swCcEnabled.isChecked) {
            license = "by"

            if (cc.swAllowRemix.isChecked) {
                if (!cc.swAllowCommercial.isChecked) {
                    license += "-nc"
                }

                if (cc.swRequireShareAlike.isChecked) {
                    license += "-sa"
                }
            } else {
                cc.swRequireShareAlike.isChecked = false

                if (!cc.swAllowCommercial.isChecked) {
                    license += "-nc"
                }

                license += "-nd"
            }
        }

        if (license != null) {
            license = String.format(CC_LICENSE_URL_FORMAT, CC_DOMAIN, license)
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