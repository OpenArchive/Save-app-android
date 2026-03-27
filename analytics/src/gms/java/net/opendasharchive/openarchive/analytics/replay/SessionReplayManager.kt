package net.opendasharchive.openarchive.analytics.replay

import android.content.Context
import com.mixpanel.android.sessionreplay.MPSessionReplay
import com.mixpanel.android.sessionreplay.models.MPSessionReplayConfig
import net.opendasharchive.openarchive.analytics.BuildConfig
import timber.log.Timber

/**
 * Manager for Mixpanel Session Replay functionality.
 *
 * Session Replay captures video-like recordings of user sessions for debugging
 * and understanding tester behavior in staging/dev builds.
 *
 * Privacy considerations:
 * - App masking is DISABLED - all content (EditText, TextView, ImageView, WebView) is recorded
 * - Only initialized AFTER user identification (email prompt)
 * - Only available when ENHANCED_ANALYTICS_ENABLED = true
 *
 * @param context Application context
 * @param token Mixpanel project token
 */
class SessionReplayManager(
    private val context: Context,
    private val token: String,
) {
    private var isInitialized = false

    /**
     * Initialize session replay with the given distinct ID.
     * Should be called after user identification.
     *
     * @param distinctId The user's distinct ID (typically email for staging)
     */
    fun initialize(distinctId: String) {
        if (isInitialized) {
            Timber.d("SessionReplay already initialized")
            return
        }

        try {
            val config =
                MPSessionReplayConfig(
                    wifiOnly = false,
                    flushInterval = 10L,
                    autoStartRecording = true,
                    recordingSessionsPercent = 100.0,
                    enableLogging = BuildConfig.DEBUG,
                    autoMaskedViews = emptySet(),
                )

            MPSessionReplay.initialize(context, token, distinctId, config)
            isInitialized = true
            Timber.i("SessionReplay initialized for user: $distinctId")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize SessionReplay")
        }
    }

    /**
     * Start recording the session.
     * Only works if initialize() was called first.
     */
    fun startRecording() {
        if (!isInitialized) {
            Timber.w("Cannot start recording - SessionReplay not initialized")
            return
        }

        try {
            MPSessionReplay.getInstance()?.startRecording()
            Timber.d("SessionReplay recording started")
        } catch (e: Exception) {
            Timber.e(e, "Failed to start SessionReplay recording")
        }
    }

    /**
     * Stop recording the session.
     */
    fun stopRecording() {
        try {
            MPSessionReplay.getInstance()?.stopRecording()
            Timber.d("SessionReplay recording stopped")
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop SessionReplay recording")
        }
    }

    /**
     * Flush recorded session data to Mixpanel.
     */
    fun flush() {
        try {
            MPSessionReplay.getInstance()?.flush()
            Timber.d("SessionReplay data flushed")
        } catch (e: Exception) {
            Timber.e(e, "Failed to flush SessionReplay data")
        }
    }

    /**
     * Check if session replay has been initialized.
     */
    fun isInitialized(): Boolean = isInitialized

    /**
     * Reset the session replay state.
     * Call this when the user logs out or resets identification.
     */
    fun reset() {
        stopRecording()
        isInitialized = false
        Timber.d("SessionReplay reset")
    }
}
