package net.opendasharchive.openarchive.analytics

import android.app.Activity
import android.content.Context
import net.opendasharchive.openarchive.BuildConfig
import net.opendasharchive.openarchive.analytics.providers.mixpanel.EnhancedMixpanelProvider
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.ui.TesterEmailDialog
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Helper class for enhanced analytics setup in staging/dev builds.
 *
 * This class handles:
 * - User identification via TesterEmailDialog
 * - Integration with EnhancedMixpanelProvider
 *
 * Only available in GMS builds. Usage:
 * ```kotlin
 * // In SaveApp.kt or MainActivity
 * if (BuildConfig.ENHANCED_ANALYTICS_ENABLED) {
 *     EnhancedAnalyticsHelper.instance.setupUserIdentification(activity)
 * }
 * ```
 */
class EnhancedAnalyticsHelper private constructor() : KoinComponent {

    // Inject EnhancedMixpanelProvider (only available when using stagingAnalyticsModule)
    private val enhancedMixpanelProvider: EnhancedMixpanelProvider by inject()

    /**
     * Check if enhanced analytics is enabled for this build.
     */
    fun isEnhancedAnalyticsEnabled(): Boolean = BuildConfig.ENHANCED_ANALYTICS_ENABLED

    /**
     * Setup user identification for staging/dev builds.
     *
     * Shows TesterEmailDialog on first launch, then uses stored email for subsequent launches.
     *
     * @param activity Activity context for showing dialogs
     * @param onIdentified Callback when user is identified (optional)
     * @param onSkipped Callback when user skips identification (optional)
     */
    fun setupUserIdentification(
        activity: Activity,
        onIdentified: ((String) -> Unit)? = null,
        onSkipped: (() -> Unit)? = null
    ) {
        if (!isEnhancedAnalyticsEnabled()) {
            AppLogger.w(TAG, "Enhanced analytics not enabled, skipping user identification")
            return
        }

        val emailDialog = TesterEmailDialog(activity)
        val storedEmail = emailDialog.getStoredEmail()

        if (storedEmail != null) {
            // Already identified - use stored email
            identifyUser(storedEmail)
            AppLogger.i(TAG, "User already identified: $storedEmail")
            onIdentified?.invoke(storedEmail)
        } else {
            // First launch - prompt for email
            emailDialog.showEmailPrompt(
                onEmailProvided = { email ->
                    identifyUser(email)
                    AppLogger.i(TAG, "User identified: $email")
                    onIdentified?.invoke(email)
                },
                onCancelled = {
                    AppLogger.w(TAG, "User skipped identification")
                    onSkipped?.invoke()
                }
            )
        }
    }

    /**
     * Identify user directly without showing dialog.
     *
     * @param email User's email address
     * @param name Optional display name
     */
    fun identifyUser(email: String, name: String? = null) {
        enhancedMixpanelProvider.identifyUser(
            userId = email,
            email = email,
            name = name
        )
    }

    /**
     * Get the current identified user ID, if any.
     */
    fun getCurrentUserId(): String? = enhancedMixpanelProvider.getCurrentUserId()

    /**
     * Check if a user has been identified.
     */
    fun isUserIdentified(): Boolean = enhancedMixpanelProvider.isUserIdentified()

    /**
     * Reset user identification (e.g., for logout or testing).
     */
    fun resetUser(context: Context) {
        enhancedMixpanelProvider.resetUser()
        TesterEmailDialog(context).clearStoredEmail()
        AppLogger.i(TAG, "User identification reset")
    }

    companion object {
        private const val TAG = "EnhancedAnalyticsHelper"

        /**
         * Singleton instance. Only use after Koin is initialized.
         */
        val instance: EnhancedAnalyticsHelper by lazy { EnhancedAnalyticsHelper() }

        /**
         * Check if enhanced analytics is enabled without needing an instance.
         * Safe to call before Koin initialization.
         */
        fun isEnabled(): Boolean = BuildConfig.ENHANCED_ANALYTICS_ENABLED
    }
}
