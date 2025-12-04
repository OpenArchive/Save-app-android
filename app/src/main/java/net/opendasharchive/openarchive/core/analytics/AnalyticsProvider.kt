package net.opendasharchive.openarchive.core.analytics

/**
 * Interface for analytics providers
 * Implements Strategy Pattern for multiple analytics backends
 */
interface AnalyticsProvider {

    /**
     * Initialize the analytics provider
     */
    fun initialize()

    /**
     * Track an analytics event
     * @param event The event to track
     */
    fun trackEvent(event: AnalyticsEvent)

    /**
     * Set user properties (GDPR-compliant, aggregated only)
     * Examples: app_version, device_type, install_date
     */
    fun setUserProperty(key: String, value: Any)

    /**
     * Persist/flush analytics data
     */
    fun persist()

    /**
     * Get provider name for debugging
     */
    fun getProviderName(): String
}
