package net.opendasharchive.openarchive.analytics.api

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.analytics.BuildConfig
import net.opendasharchive.openarchive.analytics.core.AnalyticsProvider

/**
 * Implementation of AnalyticsManager
 * Dispatches events to all providers with proper error isolation
 */
class AnalyticsManagerImpl(
    private val providers: List<AnalyticsProvider>,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : AnalyticsManager {

    private var isInitialized = false

    override suspend fun initialize(context: Context) {
        // Log build configuration
        Log.d("AnalyticsManager", "BuildConfig.DEBUG = ${BuildConfig.DEBUG}")
        Log.d("AnalyticsManager", "BuildConfig.ENABLE_ANALYTICS_IN_DEBUG = ${BuildConfig.ENABLE_ANALYTICS_IN_DEBUG}")

        // Skip analytics if: DEBUG build AND debug analytics not enabled
        if (BuildConfig.DEBUG && !BuildConfig.ENABLE_ANALYTICS_IN_DEBUG) {
            Log.w("AnalyticsManager", "Analytics DISABLED in DEBUG mode")
            isInitialized = false
            return
        }

        Log.i("AnalyticsManager", "Starting analytics initialization with ${providers.size} providers")

        try {
            // Initialize each provider
            providers.forEach { provider ->
                try {
                    Log.d("Analytics", "Initializing ${provider.getProviderName()}...")
                    provider.initialize()
                    Log.i("Analytics", "✓ ${provider.getProviderName()} initialized successfully")
                } catch (e: Exception) {
                    Log.e("Analytics", "✗ Failed to initialize ${provider.getProviderName()}", e)
                }
            }

            isInitialized = true
            Log.i("AnalyticsManager", "✓ Analytics ENABLED - Initialized with ${providers.size} providers")
        } catch (e: Exception) {
            Log.e("AnalyticsManager", "Failed to initialize AnalyticsManager", e)
        }
    }

    override suspend fun trackEvent(event: AnalyticsEvent) {
        // Skip if not initialized (includes DEBUG builds)
        if (!isInitialized) {
            Log.w("AnalyticsManager", "Event NOT tracked (not initialized): ${event.category}_${event.action}")
            return
        }

        Log.d("AnalyticsManager", "Tracking event: ${event.category}_${event.action}")

        providers.forEach { provider ->
            scope.launch {
                try {
                    provider.trackEvent(event)
                    Log.d("Analytics", "Event sent to ${provider.getProviderName()}: ${event.category}_${event.action}")
                } catch (e: Exception) {
                    Log.e("Analytics", "Failed to track event in ${provider.getProviderName()}", e)
                }
            }
        }
    }

    override suspend fun setUserProperty(key: String, value: Any) {
        if (!isInitialized) return

        providers.forEach { provider ->
            scope.launch {
                try {
                    provider.setUserProperty(key, value)
                } catch (e: Exception) {
                    Log.e("Analytics", "Failed to set user property in ${provider.getProviderName()}", e)
                }
            }
        }
    }

    override suspend fun flush() {
        if (!isInitialized) return

        providers.forEach { provider ->
            try {
                provider.flush()
            } catch (e: Exception) {
                Log.e("Analytics", "Failed to flush in ${provider.getProviderName()}", e)
            }
        }
    }

    // ==================== CONVENIENCE METHODS ====================

    override suspend fun trackScreenView(
        screenName: String,
        timeSpentSeconds: Long?,
        previousScreen: String?
    ) {
        trackEvent(
            AnalyticsEvent.ScreenViewed(
                screenName = screenName,
                timeSpentSeconds = timeSpentSeconds,
                previousScreen = previousScreen
            )
        )
    }

    override suspend fun trackNavigation(
        fromScreen: String,
        toScreen: String,
        trigger: String?
    ) {
        trackEvent(
            AnalyticsEvent.NavigationAction(
                fromScreen = fromScreen,
                toScreen = toScreen,
                trigger = trigger
            )
        )
    }

    override suspend fun trackBackendConfigured(
        backendType: String,
        isNew: Boolean
    ) {
        trackEvent(
            AnalyticsEvent.BackendConfigured(
                backendType = backendType,
                isNew = isNew
            )
        )
    }

    override suspend fun trackUploadStarted(
        backendType: String,
        fileType: String,
        fileSizeKB: Long
    ) {
        trackEvent(
            AnalyticsEvent.UploadStarted(
                backendType = backendType,
                fileType = fileType,
                fileSizeKB = fileSizeKB
            )
        )
    }

    override suspend fun trackUploadCompleted(
        backendType: String,
        fileType: String,
        fileSizeKB: Long,
        durationSeconds: Long,
        uploadSpeedKBps: Long?
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

    override suspend fun trackUploadFailed(
        backendType: String,
        fileType: String,
        errorCategory: String,
        fileSizeKB: Long?
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

    override suspend fun trackFeatureToggled(
        featureName: String,
        enabled: Boolean
    ) {
        trackEvent(
            AnalyticsEvent.FeatureToggled(
                featureName = featureName,
                enabled = enabled
            )
        )
    }

    override suspend fun trackError(
        errorCategory: String,
        screenName: String,
        backendType: String?
    ) {
        trackEvent(
            AnalyticsEvent.ErrorOccurred(
                errorCategory = errorCategory,
                screenName = screenName,
                backendType = backendType
            )
        )
    }

    override suspend fun trackAppOpened(
        isFirstLaunch: Boolean,
        appVersion: String
    ) {
        trackEvent(
            AnalyticsEvent.AppOpened(
                isFirstLaunch = isFirstLaunch,
                appVersion = appVersion
            )
        )
    }

    override suspend fun trackAppClosed(
        sessionDurationSeconds: Long
    ) {
        trackEvent(
            AnalyticsEvent.AppClosed(
                sessionDurationSeconds = sessionDurationSeconds
            )
        )
    }

    override suspend fun trackSessionStarted(
        isFirstSession: Boolean,
        sessionNumber: Int
    ) {
        trackEvent(
            AnalyticsEvent.SessionStarted(
                isFirstSession = isFirstSession,
                sessionNumber = sessionNumber
            )
        )
    }

    override suspend fun trackSessionEnded(
        lastScreen: String,
        durationSeconds: Long,
        uploadsCompleted: Int,
        uploadsFailed: Int
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
