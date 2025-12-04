package net.opendasharchive.openarchive.core.analytics.providers

import android.content.Context
import net.opendasharchive.openarchive.CleanInsightsManager
import net.opendasharchive.openarchive.core.analytics.AnalyticsEvent
import net.opendasharchive.openarchive.core.analytics.AnalyticsProvider

/**
 * CleanInsights implementation of AnalyticsProvider
 * Privacy-focused, GDPR-compliant by design
 */
class CleanInsightsProvider(
    private val context: Context
) : AnalyticsProvider {

    override fun initialize() {
        CleanInsightsManager.init(context)
    }

    override fun trackEvent(event: AnalyticsEvent) {
        // Only track if user has consented
        if (!CleanInsightsManager.hasConsent()) return

        CleanInsightsManager.measureEvent(
            category = event.category,
            action = event.action,
            name = event.label,
            value = event.value
        )

        // Track screen views separately for visit tracking
        if (event is AnalyticsEvent.ScreenViewed) {
            CleanInsightsManager.measureView(event.screenName)
        }
    }

    override fun setUserProperty(key: String, value: Any) {
        // CleanInsights doesn't support user properties (privacy-focused)
        // Aggregate data only
    }

    override fun persist() {
        CleanInsightsManager.persist()
    }

    override fun getProviderName(): String = "CleanInsights"
}
