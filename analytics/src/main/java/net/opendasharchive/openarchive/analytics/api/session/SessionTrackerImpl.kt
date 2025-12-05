package net.opendasharchive.openarchive.analytics.api.session

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.opendasharchive.openarchive.analytics.api.AnalyticsEvent
import net.opendasharchive.openarchive.analytics.api.AnalyticsManager

/**
 * Implementation of SessionTracker using StateFlow for reactive session management
 */
class SessionTrackerImpl(
    private val analyticsManager: AnalyticsManager,
    context: Context
) : SessionTracker {

    private val prefs: SharedPreferences = context.getSharedPreferences("analytics_session", Context.MODE_PRIVATE)

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Idle)
    override val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private val _currentScreen = MutableStateFlow("")
    override val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    private var sessionStartTime: Long = 0
    private var sessionUploadsCompleted: Int = 0
    private var sessionUploadsFailed: Int = 0

    override suspend fun startSession() {
        val currentState = _sessionState.value
        if (currentState is SessionState.Active) return

        sessionStartTime = System.currentTimeMillis()
        sessionUploadsCompleted = 0
        sessionUploadsFailed = 0

        val isFirstLaunch = prefs.getBoolean(PREF_FIRST_LAUNCH, true)
        val sessionCount = prefs.getInt(PREF_SESSION_COUNT, 0) + 1

        _sessionState.value = SessionState.Active(
            sessionNumber = sessionCount,
            startTime = sessionStartTime,
            uploadsCompleted = 0,
            uploadsFailed = 0
        )

        // Track session start with new AnalyticsManager
        analyticsManager.trackSessionStarted(isFirstLaunch, sessionCount)

        // Track app opened
        val appVersion = prefs.getString(PREF_APP_VERSION, "unknown") ?: "unknown"
        analyticsManager.trackAppOpened(isFirstLaunch, appVersion)

        if (isFirstLaunch) {
            prefs.edit().putBoolean(PREF_FIRST_LAUNCH, false).apply()
        }

        // Increment and save session count
        prefs.edit().putInt(PREF_SESSION_COUNT, sessionCount).apply()
    }

    override suspend fun endSession() {
        val currentState = _sessionState.value
        if (currentState !is SessionState.Active) return

        val sessionDuration = (System.currentTimeMillis() - sessionStartTime) / 1000

        // Track session end with upload stats
        analyticsManager.trackSessionEnded(
            lastScreen = _currentScreen.value,
            durationSeconds = sessionDuration,
            uploadsCompleted = sessionUploadsCompleted,
            uploadsFailed = sessionUploadsFailed
        )

        // Track app closed
        analyticsManager.trackAppClosed(sessionDuration)

        // Persist analytics data
        analyticsManager.flush()

        // Store last screen and upload stats for analysis
        prefs.edit()
            .putString(PREF_LAST_SCREEN, _currentScreen.value)
            .putInt(PREF_SESSION_UPLOADS_COMPLETED, sessionUploadsCompleted)
            .putInt(PREF_SESSION_UPLOADS_FAILED, sessionUploadsFailed)
            .apply()

        _sessionState.value = SessionState.Idle
    }

    override fun setCurrentScreen(screenName: String) {
        _currentScreen.value = screenName
    }

    override fun trackUploadCompleted() {
        sessionUploadsCompleted++
        val currentState = _sessionState.value
        if (currentState is SessionState.Active) {
            _sessionState.value = currentState.copy(uploadsCompleted = sessionUploadsCompleted)
        }
    }

    override fun trackUploadFailed() {
        sessionUploadsFailed++
        val currentState = _sessionState.value
        if (currentState is SessionState.Active) {
            _sessionState.value = currentState.copy(uploadsFailed = sessionUploadsFailed)
        }
    }

    override fun getUploadSuccessRate(): Float {
        val total = sessionUploadsCompleted + sessionUploadsFailed
        return if (total > 0) sessionUploadsCompleted.toFloat() / total else 0f
    }

    override fun isFirstLaunch(): Boolean {
        return prefs.getBoolean(PREF_FIRST_LAUNCH, true)
    }

    override fun getSessionCount(): Int {
        return prefs.getInt(PREF_SESSION_COUNT, 0)
    }

    override fun getCurrentSessionDuration(): Long {
        val currentState = _sessionState.value
        return if (currentState is SessionState.Active) {
            (System.currentTimeMillis() - sessionStartTime) / 1000
        } else {
            0
        }
    }

    override suspend fun onBackground() {
        val currentState = _sessionState.value
        if (currentState is SessionState.Active) {
            analyticsManager.trackEvent(AnalyticsEvent.AppBackgrounded)
        }
    }

    override suspend fun onForeground() {
        val currentState = _sessionState.value
        if (currentState is SessionState.Active) {
            analyticsManager.trackEvent(AnalyticsEvent.AppForegrounded)
        }
    }

    /**
     * Set app version for tracking
     * Should be called during initialization
     */
    fun setAppVersion(version: String) {
        prefs.edit().putString(PREF_APP_VERSION, version).apply()
    }

    companion object {
        private const val PREF_FIRST_LAUNCH = "first_launch_completed"
        private const val PREF_LAST_SCREEN = "last_active_screen"
        private const val PREF_SESSION_COUNT = "session_count"
        private const val PREF_SESSION_UPLOADS_COMPLETED = "session_uploads_completed"
        private const val PREF_SESSION_UPLOADS_FAILED = "session_uploads_failed"
        private const val PREF_APP_VERSION = "app_version"
    }
}
