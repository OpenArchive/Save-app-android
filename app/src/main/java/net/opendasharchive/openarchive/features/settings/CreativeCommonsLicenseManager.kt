package net.opendasharchive.openarchive.features.settings

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
}