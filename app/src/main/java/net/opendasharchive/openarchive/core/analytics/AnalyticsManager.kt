package net.opendasharchive.openarchive.core.analytics

import android.content.Context
import net.opendasharchive.openarchive.BuildConfig
import net.opendasharchive.openarchive.core.analytics.providers.CleanInsightsProvider
import net.opendasharchive.openarchive.core.analytics.providers.FirebaseProvider
import net.opendasharchive.openarchive.core.analytics.providers.MixpanelProvider
import net.opendasharchive.openarchive.core.logger.AppLogger

/**
 * Unified Analytics Manager - Facade Pattern
 *
 * Dispatches analytics events to multiple providers:
 * - CleanInsights (privacy-focused, GDPR-compliant)
 * - Mixpanel (detailed analytics)
 * - Firebase (Google Analytics)
 *
 * GDPR-Compliant: All events are sanitized and contain NO PII
 *
 * Usage:
 * ```
 * AnalyticsManager.initialize(context)
 * AnalyticsManager.trackEvent(AnalyticsEvent.AppOpened(isFirstLaunch = true, appVersion = "1.0"))
 * ```
 */
object AnalyticsManager {

    private val providers = mutableListOf<AnalyticsProvider>()
    private var isInitialized = false

    /**
     * Initialize all analytics providers
     * Call this once in Application.onCreate()
     *
     * NOTE: Analytics is disabled in DEBUG builds to keep production data clean
     */
    fun initialize(context: Context) {
        if (isInitialized) return

        // Skip analytics in debug builds to avoid polluting production data
        if (BuildConfig.DEBUG) {
            AppLogger.d("AnalyticsManager: Analytics DISABLED in DEBUG mode")
            isInitialized = false
            return
        }

        try {
            // Add all providers
            providers.add(CleanInsightsProvider(context.applicationContext))
            providers.add(MixpanelProvider(context.applicationContext))
            providers.add(FirebaseProvider(context.applicationContext))

            // Initialize each provider
            providers.forEach { provider ->
                try {
                    provider.initialize()
                    AppLogger.d("Analytics: ${provider.getProviderName()} initialized")
                } catch (e: Exception) {
                    AppLogger.e("Failed to initialize ${provider.getProviderName()}", e)
                }
            }

            isInitialized = true
            AppLogger.d("AnalyticsManager initialized with ${providers.size} providers")
        } catch (e: Exception) {
            AppLogger.e("Failed to initialize AnalyticsManager", e)
        }
    }

    /**
     * Track an analytics event across all providers
     * @param event The event to track
     */
    fun trackEvent(event: AnalyticsEvent) {
        // Skip if not initialized (includes DEBUG builds)
        if (!isInitialized) return

        providers.forEach { provider ->
            try {
                provider.trackEvent(event)
            } catch (e: Exception) {
                AppLogger.e("Failed to track event in ${provider.getProviderName()}", e)
            }
        }
    }

    /**
     * Set user properties across all providers
     * GDPR-Compliant: Only use aggregated, non-identifying properties
     * Examples: app_version, device_type, install_date
     */
    fun setUserProperty(key: String, value: Any) {
        if (!isInitialized) return

        providers.forEach { provider ->
            try {
                provider.setUserProperty(key, value)
            } catch (e: Exception) {
                AppLogger.e("Failed to set user property in ${provider.getProviderName()}", e)
            }
        }
    }

    /**
     * Persist/flush analytics data to servers
     * Call this when app goes to background
     */
    fun persist() {
        if (!isInitialized) return

        providers.forEach { provider ->
            try {
                provider.persist()
            } catch (e: Exception) {
                AppLogger.e("Failed to persist in ${provider.getProviderName()}", e)
            }
        }
    }

    // ==================== CONVENIENCE METHODS ====================

    /**
     * Track screen view with time spent
     */
    fun trackScreenView(screenName: String, timeSpentSeconds: Long? = null, previousScreen: String? = null) {
        trackEvent(
            AnalyticsEvent.ScreenViewed(
                screenName = screenName,
                timeSpentSeconds = timeSpentSeconds,
                previousScreen = previousScreen
            )
        )
    }

    /**
     * Track navigation between screens
     */
    fun trackNavigation(fromScreen: String, toScreen: String, trigger: String? = null) {
        trackEvent(
            AnalyticsEvent.NavigationAction(
                fromScreen = fromScreen,
                toScreen = toScreen,
                trigger = trigger
            )
        )
    }

    /**
     * Track backend configuration
     */
    fun trackBackendConfigured(backendType: String, isNew: Boolean = true) {
        trackEvent(
            AnalyticsEvent.BackendConfigured(
                backendType = backendType,
                isNew = isNew
            )
        )
    }

    /**
     * Track upload started
     */
    fun trackUploadStarted(backendType: String, fileType: String, fileSizeKB: Long) {
        trackEvent(
            AnalyticsEvent.UploadStarted(
                backendType = backendType,
                fileType = fileType,
                fileSizeKB = fileSizeKB
            )
        )
    }

    /**
     * Track upload completed
     */
    fun trackUploadCompleted(
        backendType: String,
        fileType: String,
        fileSizeKB: Long,
        durationSeconds: Long,
        uploadSpeedKBps: Long? = null
    ) {
        trackEvent(
            AnalyticsEvent.UploadCompleted(
                backendType = backendType,
                fileType = fileType,
                fileSizeKB = fileSizeKB,
                durationSeconds = durationSeconds,
                uploadSpeedKBps = uploadSpeedKBps
            )
        )
    }

    /**
     * Track upload failed
     */
    fun trackUploadFailed(
        backendType: String,
        fileType: String,
        errorCategory: String,
        fileSizeKB: Long? = null
    ) {
        trackEvent(
            AnalyticsEvent.UploadFailed(
                backendType = backendType,
                fileType = fileType,
                errorCategory = errorCategory,
                fileSizeKB = fileSizeKB
            )
        )
    }

    /**
     * Track feature toggle
     */
    fun trackFeatureToggled(featureName: String, enabled: Boolean) {
        trackEvent(
            AnalyticsEvent.FeatureToggled(
                featureName = featureName,
                enabled = enabled
            )
        )
    }

    /**
     * Track error
     */
    fun trackError(errorCategory: String, screenName: String, backendType: String? = null) {
        trackEvent(
            AnalyticsEvent.ErrorOccurred(
                errorCategory = errorCategory,
                screenName = screenName,
                backendType = backendType
            )
        )
    }

    /**
     * Track app lifecycle events
     */
    fun trackAppOpened(isFirstLaunch: Boolean, appVersion: String) {
        trackEvent(
            AnalyticsEvent.AppOpened(
                isFirstLaunch = isFirstLaunch,
                appVersion = appVersion
            )
        )
    }

    fun trackAppClosed(sessionDurationSeconds: Long) {
        trackEvent(
            AnalyticsEvent.AppClosed(
                sessionDurationSeconds = sessionDurationSeconds
            )
        )
    }

    /**
     * Track session events
     */
    fun trackSessionStarted(isFirstSession: Boolean, sessionNumber: Int) {
        trackEvent(
            AnalyticsEvent.SessionStarted(
                isFirstSession = isFirstSession,
                sessionNumber = sessionNumber
            )
        )
    }

    fun trackSessionEnded(
        lastScreen: String,
        durationSeconds: Long,
        uploadsCompleted: Int = 0,
        uploadsFailed: Int = 0
    ) {
        trackEvent(
            AnalyticsEvent.SessionEnded(
                lastScreen = lastScreen,
                durationSeconds = durationSeconds,
                uploadsCompleted = uploadsCompleted,
                uploadsFailed = uploadsFailed
            )
        )
    }
}
