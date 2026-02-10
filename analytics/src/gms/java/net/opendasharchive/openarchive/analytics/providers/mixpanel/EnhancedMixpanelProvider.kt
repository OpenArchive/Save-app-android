package net.opendasharchive.openarchive.analytics.providers.mixpanel

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.mixpanel.android.mpmetrics.MixpanelAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.analytics.BuildConfig
import net.opendasharchive.openarchive.analytics.api.AnalyticsEvent
import net.opendasharchive.openarchive.analytics.core.AnalyticsProvider
import net.opendasharchive.openarchive.analytics.replay.SessionReplayManager
import org.json.JSONObject

/**
 * Enhanced Mixpanel implementation for staging/dev builds.
 *
 * Key differences from production MixpanelProvider:
 * - User identification via identify() - links events to real users
 * - NO PII sanitization - raw file paths, emails, URLs visible for debugging
 * - User profiles with device info, app version, environment
 *
 * WARNING: This provider should ONLY be used in staging/dev builds.
 * Production builds must use the standard MixpanelProvider with PII sanitization.
 */
class EnhancedMixpanelProvider(
    private val context: Context,
    private val token: String
) : AnalyticsProvider {

    private var mixpanel: MixpanelAPI? = null
    private var sessionReplayManager: SessionReplayManager? = null
    private var currentUserId: String? = null

    override suspend fun initialize() {
        withContext(Dispatchers.IO) {
            mixpanel = MixpanelAPI.getInstance(context, token, false)
            // Initialize session replay manager (but don't start recording yet - wait for user identification)
            sessionReplayManager = SessionReplayManager(context, token)
        }
    }

    /**
     * Identify the current user for enhanced tracking.
     * This links all subsequent events to this specific user.
     *
     * @param userId Unique user identifier (typically email for staging)
     * @param email Optional email address for user profile
     * @param name Optional display name for user profile
     */
    fun identifyUser(userId: String, email: String? = null, name: String? = null) {
        mixpanel?.identify(userId)
        currentUserId = userId

        // Set user profile properties for easy identification in Mixpanel dashboard
        mixpanel?.people?.apply {
            email?.let { set("\$email", it) }
            name?.let { set("\$name", it) }
            set("environment", getEnvironmentName())
            set("device", "${Build.MANUFACTURER} ${Build.MODEL}")
            set("device_sdk", Build.VERSION.SDK_INT)
            set("app_version", getAppVersion())
            set("first_seen", System.currentTimeMillis())
        }

        // Start Session Replay after user is identified
        sessionReplayManager?.initialize(userId)
    }

    /**
     * Reset user identification (e.g., on logout).
     */
    fun resetUser() {
        sessionReplayManager?.reset()
        mixpanel?.reset()
        currentUserId = null
    }

    override suspend fun trackEvent(event: AnalyticsEvent) {
        withContext(Dispatchers.IO) {
            val eventName = "${event.category}_${event.action}"

            // Convert properties to JSONObject WITHOUT PII sanitization
            // Raw values visible for debugging in staging
            val properties = JSONObject()

            event.properties.forEach { (key, value) ->
                // NO sanitization - full details visible in staging
                properties.put(key, value)
            }

            // Add event label if present (unsanitized)
            event.label?.let {
                properties.put("label", it)
            }

            // Add event value if present
            event.value?.let {
                properties.put("value", it)
            }

            // Add environment context to every event
            properties.put("environment", getEnvironmentName())
            properties.put("enhanced_tracking", true)

            // Add user context if identified
            currentUserId?.let {
                properties.put("user_id", it)
            }

            mixpanel?.track(eventName, properties)
        }
    }

    override suspend fun setUserProperty(key: String, value: Any) {
        withContext(Dispatchers.IO) {
            // NO PII sanitization for staging - raw values for debugging
            mixpanel?.people?.set(key, value)
        }
    }

    override suspend fun flush() {
        withContext(Dispatchers.IO) {
            mixpanel?.flush()
            sessionReplayManager?.flush()
        }
    }

    override fun getProviderName(): String = "EnhancedMixpanel"

    /**
     * Get the current user ID if identified.
     */
    fun getCurrentUserId(): String? = currentUserId

    /**
     * Check if a user has been identified.
     */
    fun isUserIdentified(): Boolean = currentUserId != null

    private fun getEnvironmentName(): String {
        return when {
            BuildConfig.DEBUG -> "dev"
            else -> "staging"
        }
    }

    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "unknown"
        } catch (e: PackageManager.NameNotFoundException) {
            "unknown"
        }
    }
}
