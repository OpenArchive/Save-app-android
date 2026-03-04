package net.opendasharchive.openarchive.analytics

import android.app.Activity
import android.content.Context

/**
 * FOSS stub for EnhancedAnalyticsHelper.
 *
 * Enhanced analytics with user identification is only available in GMS builds.
 * This stub provides a no-op implementation for FOSS builds.
 */
class EnhancedAnalyticsHelper private constructor() {

    /**
     * Always returns false for FOSS builds.
     */
    fun isEnhancedAnalyticsEnabled(): Boolean = false

    /**
     * No-op for FOSS builds.
     */
    fun setupUserIdentification(
        activity: Activity,
        onIdentified: ((String) -> Unit)? = null,
        onSkipped: (() -> Unit)? = null
    ) {
        // No-op - enhanced analytics not available in FOSS builds
        onSkipped?.invoke()
    }

    /**
     * No-op for FOSS builds.
     */
    fun identifyUser(email: String, name: String? = null) {
        // No-op - enhanced analytics not available in FOSS builds
    }

    /**
     * Always returns null for FOSS builds.
     */
    fun getCurrentUserId(): String? = null

    /**
     * Always returns false for FOSS builds.
     */
    fun isUserIdentified(): Boolean = false

    /**
     * No-op for FOSS builds.
     */
    fun resetUser(context: Context) {
        // No-op - enhanced analytics not available in FOSS builds
    }

    companion object {
        /**
         * Singleton instance.
         */
        val instance: EnhancedAnalyticsHelper by lazy { EnhancedAnalyticsHelper() }

        /**
         * Always returns false for FOSS builds.
         */
        fun isEnabled(): Boolean = false
    }
}
