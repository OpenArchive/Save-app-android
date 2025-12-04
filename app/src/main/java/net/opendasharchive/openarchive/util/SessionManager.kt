package net.opendasharchive.openarchive.util

import android.content.Context
import net.opendasharchive.openarchive.BuildConfig
import net.opendasharchive.openarchive.core.analytics.AnalyticsManager

/**
 * Manages user sessions and tracks session analytics
 * Provides GDPR-compliant session tracking for user journey analysis
 */
object SessionManager {

    private const val PREF_FIRST_LAUNCH = "first_launch_completed"
    private const val PREF_LAST_SCREEN = "last_active_screen"
    private const val PREF_SESSION_COUNT = "session_count"
    private const val PREF_SESSION_UPLOADS_COMPLETED = "session_uploads_completed"
    private const val PREF_SESSION_UPLOADS_FAILED = "session_uploads_failed"

    private var sessionStartTime: Long = 0
    private var currentScreen: String = ""
    private var isSessionActive: Boolean = false
    private var sessionUploadsCompleted: Int = 0
    private var sessionUploadsFailed: Int = 0

    /**
     * Start a new session
     * Called when app is opened or comes to foreground
     */
    fun startSession(context: Context) {
        if (isSessionActive) return

        sessionStartTime = System.currentTimeMillis()
        isSessionActive = true

        // Reset session counters
        sessionUploadsCompleted = 0
        sessionUploadsFailed = 0

        val isFirstLaunch = Prefs.getBoolean(PREF_FIRST_LAUNCH, true)
        val sessionCount = Prefs.getInt(PREF_SESSION_COUNT, 0) + 1

        // Track session start with new AnalyticsManager
        AnalyticsManager.trackSessionStarted(isFirstLaunch, sessionCount)

        // Track app opened
        AnalyticsManager.trackAppOpened(isFirstLaunch, BuildConfig.VERSION_NAME)

        if (isFirstLaunch) {
            Prefs.putBoolean(PREF_FIRST_LAUNCH, false)
        }

        // Increment and save session count
        Prefs.putInt(PREF_SESSION_COUNT, sessionCount)
    }

    /**
     * End the current session
     * Called when app is closed or goes to background
     */
    fun endSession() {
        if (!isSessionActive) return

        val sessionDuration = (System.currentTimeMillis() - sessionStartTime) / 1000

        // Track session end with upload stats
        AnalyticsManager.trackSessionEnded(
            lastScreen = currentScreen,
            durationSeconds = sessionDuration,
            uploadsCompleted = sessionUploadsCompleted,
            uploadsFailed = sessionUploadsFailed
        )

        // Track app closed
        AnalyticsManager.trackAppClosed(sessionDuration)

        // Persist analytics data
        AnalyticsManager.persist()

        // Store last screen and upload stats for analysis
        Prefs.putString(PREF_LAST_SCREEN, currentScreen)
        Prefs.putInt(PREF_SESSION_UPLOADS_COMPLETED, sessionUploadsCompleted)
        Prefs.putInt(PREF_SESSION_UPLOADS_FAILED, sessionUploadsFailed)

        isSessionActive = false
    }

    /**
     * Update the current screen
     * @param screenName Name of the current screen
     */
    fun setCurrentScreen(screenName: String) {
        currentScreen = screenName
    }

    /**
     * Get the last active screen (useful for crash/uninstall analysis)
     */
    fun getLastScreen(): String {
        return Prefs.getString(PREF_LAST_SCREEN, "Unknown") ?: "Unknown"
    }

    /**
     * Get total session count
     */
    fun getSessionCount(): Int {
        return Prefs.getInt(PREF_SESSION_COUNT, 0)
    }

    /**
     * Check if this is the first launch
     */
    fun isFirstLaunch(): Boolean {
        return Prefs.getBoolean(PREF_FIRST_LAUNCH, true)
    }

    /**
     * Get current session duration in seconds
     */
    fun getCurrentSessionDuration(): Long {
        if (!isSessionActive) return 0
        return (System.currentTimeMillis() - sessionStartTime) / 1000
    }

    /**
     * Track app going to background
     */
    fun onBackground() {
        if (isSessionActive) {
            AnalyticsManager.trackEvent(net.opendasharchive.openarchive.core.analytics.AnalyticsEvent.AppBackgrounded())
        }
    }

    /**
     * Track app coming to foreground
     */
    fun onForeground() {
        if (isSessionActive) {
            AnalyticsManager.trackEvent(net.opendasharchive.openarchive.core.analytics.AnalyticsEvent.AppForegrounded())
        }
    }

    /**
     * Track successful upload
     */
    fun trackUploadCompleted() {
        sessionUploadsCompleted++
    }

    /**
     * Track failed upload
     */
    fun trackUploadFailed() {
        sessionUploadsFailed++
    }

    /**
     * Get upload success rate for current session
     */
    fun getUploadSuccessRate(): Float {
        val total = sessionUploadsCompleted + sessionUploadsFailed
        return if (total > 0) sessionUploadsCompleted.toFloat() / total else 0f
    }
}
