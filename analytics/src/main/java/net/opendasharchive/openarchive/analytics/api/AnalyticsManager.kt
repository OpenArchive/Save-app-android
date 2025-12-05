package net.opendasharchive.openarchive.analytics.api

import android.content.Context

/**
 * Unified Analytics Manager Interface
 *
 * Dispatches analytics events to multiple providers:
 * - CleanInsights (privacy-focused, GDPR-compliant)
 * - Mixpanel (detailed analytics)
 * - Firebase (Google Analytics)
 *
 * GDPR-Compliant: All events are sanitized and contain NO PII
 *
 * Modern implementation using coroutines for async operations
 */
interface AnalyticsManager {

    /**
     * Initialize all analytics providers
     * Call this once in Application.onCreate()
     */
    suspend fun initialize(context: Context)

    /**
     * Track an analytics event across all providers
     * @param event The event to track
     */
    suspend fun trackEvent(event: AnalyticsEvent)

    /**
     * Set user properties across all providers
     * GDPR-Compliant: Only use aggregated, non-identifying properties
     * Examples: app_version, device_type, install_date
     */
    suspend fun setUserProperty(key: String, value: Any)

    /**
     * Persist/flush analytics data to servers
     * Call this when app goes to background
     */
    suspend fun flush()

    // ==================== CONVENIENCE METHODS ====================

    /**
     * Track screen view with time spent
     */
    suspend fun trackScreenView(
        screenName: String,
        timeSpentSeconds: Long? = null,
        previousScreen: String? = null
    )

    /**
     * Track navigation between screens
     */
    suspend fun trackNavigation(
        fromScreen: String,
        toScreen: String,
        trigger: String? = null
    )

    /**
     * Track backend configuration
     */
    suspend fun trackBackendConfigured(
        backendType: String,
        isNew: Boolean = true
    )

    /**
     * Track upload started
     */
    suspend fun trackUploadStarted(
        backendType: String,
        fileType: String,
        fileSizeKB: Long
    )

    /**
     * Track upload completed
     */
    suspend fun trackUploadCompleted(
        backendType: String,
        fileType: String,
        fileSizeKB: Long,
        durationSeconds: Long,
        uploadSpeedKBps: Long? = null
    )

    /**
     * Track upload failed
     */
    suspend fun trackUploadFailed(
        backendType: String,
        fileType: String,
        errorCategory: String,
        fileSizeKB: Long? = null
    )

    /**
     * Track feature toggle
     */
    suspend fun trackFeatureToggled(
        featureName: String,
        enabled: Boolean
    )

    /**
     * Track error
     */
    suspend fun trackError(
        errorCategory: String,
        screenName: String,
        backendType: String? = null
    )

    /**
     * Track app lifecycle events
     */
    suspend fun trackAppOpened(
        isFirstLaunch: Boolean,
        appVersion: String
    )

    suspend fun trackAppClosed(
        sessionDurationSeconds: Long
    )

    /**
     * Track session events
     */
    suspend fun trackSessionStarted(
        isFirstSession: Boolean,
        sessionNumber: Int
    )

    suspend fun trackSessionEnded(
        lastScreen: String,
        durationSeconds: Long,
        uploadsCompleted: Int = 0,
        uploadsFailed: Int = 0
    )
}
