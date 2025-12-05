package net.opendasharchive.openarchive.analytics.core

import net.opendasharchive.openarchive.analytics.api.AnalyticsEvent

/**
 * Interface for analytics providers
 * Implements Strategy Pattern for multiple analytics backends
 *
 * Modern implementation using coroutines for async operations
 */
interface AnalyticsProvider {

    /**
     * Initialize the analytics provider
     * Suspends to allow async initialization
     */
    suspend fun initialize()

    /**
     * Track an analytics event
     * @param event The event to track
     */
    suspend fun trackEvent(event: AnalyticsEvent)

    /**
     * Set user properties (GDPR-compliant, aggregated only)
     * Examples: app_version, device_type, install_date
     */
    suspend fun setUserProperty(key: String, value: Any)

    /**
     * Persist/flush analytics data
     */
    suspend fun flush()

    /**
     * Get provider name for debugging
     * Not a suspend function as it's synchronous
     */
    fun getProviderName(): String
}
