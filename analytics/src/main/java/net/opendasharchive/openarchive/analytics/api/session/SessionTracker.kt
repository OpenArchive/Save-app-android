package net.opendasharchive.openarchive.analytics.api.session

import kotlinx.coroutines.flow.StateFlow

/**
 * Session tracking interface with reactive StateFlow support
 * Modern implementation using StateFlow for lifecycle-aware session management
 */
interface SessionTracker {

    /**
     * Reactive session state
     */
    val sessionState: StateFlow<SessionState>

    /**
     * Reactive current screen tracking
     */
    val currentScreen: StateFlow<String>

    /**
     * Start a new session
     * Called when app is opened or comes to foreground
     */
    suspend fun startSession()

    /**
     * End the current session
     * Called when app is closed or goes to background
     */
    suspend fun endSession()

    /**
     * Update the current screen
     */
    fun setCurrentScreen(screenName: String)

    /**
     * Track successful upload
     */
    fun trackUploadCompleted()

    /**
     * Track failed upload
     */
    fun trackUploadFailed()

    /**
     * Get upload success rate for current session
     */
    fun getUploadSuccessRate(): Float

    /**
     * Check if this is the first launch
     */
    fun isFirstLaunch(): Boolean

    /**
     * Get total session count
     */
    fun getSessionCount(): Int

    /**
     * Get current session duration in seconds
     */
    fun getCurrentSessionDuration(): Long

    /**
     * Track app going to background
     */
    suspend fun onBackground()

    /**
     * Track app coming to foreground
     */
    suspend fun onForeground()
}

/**
 * Sealed interface representing session states
 */
sealed interface SessionState {
    /**
     * Session is not active
     */
    data object Idle : SessionState

    /**
     * Session is active
     */
    data class Active(
        val sessionNumber: Int,
        val startTime: Long,
        val uploadsCompleted: Int = 0,
        val uploadsFailed: Int = 0
    ) : SessionState
}
