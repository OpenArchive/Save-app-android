package net.opendasharchive.openarchive.analytics.providers.cleaninsights

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.analytics.api.AnalyticsEvent
import net.opendasharchive.openarchive.analytics.core.AnalyticsProvider
import org.cleaninsights.sdk.CleanInsights

/**
 * CleanInsights implementation of AnalyticsProvider
 * Privacy-focused, GDPR-compliant by design
 */
class CleanInsightsProvider(
    private val context: Context,
    private val campaignId: String = "main",
    private val consentChecker: () -> Boolean
) : AnalyticsProvider {

    private var cleanInsights: CleanInsights? = null

    override suspend fun initialize() {
        withContext(Dispatchers.IO) {
            val config = context.assets.open("cleaninsights.json").reader().use { it.readText() }
            cleanInsights = CleanInsights(config, context.filesDir)
        }
    }

    override suspend fun trackEvent(event: AnalyticsEvent) {
        withContext(Dispatchers.IO) {
            // Only track if user has consented
            if (!consentChecker()) return@withContext

            cleanInsights?.measureEvent(
                category = event.category,
                action = event.action,
                campaignId = campaignId,
                name = event.label,
                value = event.value
            )

            // Track screen views separately for visit tracking
            if (event is AnalyticsEvent.ScreenViewed) {
                cleanInsights?.measureVisit(listOf(event.screenName), campaignId)
            }
        }
    }

    override suspend fun setUserProperty(key: String, value: Any) {
        // CleanInsights doesn't support user properties (privacy-focused)
        // Aggregate data only
    }

    override suspend fun flush() {
        withContext(Dispatchers.IO) {
            cleanInsights?.persist()
        }
    }

    override fun getProviderName(): String = "CleanInsights"
}
