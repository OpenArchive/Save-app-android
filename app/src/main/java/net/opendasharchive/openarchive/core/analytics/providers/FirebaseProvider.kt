package net.opendasharchive.openarchive.core.analytics.providers

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import net.opendasharchive.openarchive.core.analytics.AnalyticsEvent
import net.opendasharchive.openarchive.core.analytics.AnalyticsProvider

/**
 * Firebase Analytics implementation of AnalyticsProvider
 * With automatic PII sanitization
 */
class FirebaseProvider(
    private val context: Context
) : AnalyticsProvider {

    private var firebaseAnalytics: FirebaseAnalytics? = null

    override fun initialize() {
        firebaseAnalytics = FirebaseAnalytics.getInstance(context)
    }

    override fun trackEvent(event: AnalyticsEvent) {
        val eventName = sanitizeFirebaseEventName("${event.category}_${event.action}")

        // Convert properties to Bundle with PII sanitization
        val bundle = Bundle().apply {
            event.properties.forEach { (key, value) ->
                val sanitizedKey = sanitizeFirebaseParameterName(key)
                when (value) {
                    is String -> putString(sanitizedKey, sanitizePII(value))
                    is Int -> putInt(sanitizedKey, value)
                    is Long -> putLong(sanitizedKey, value)
                    is Double -> putDouble(sanitizedKey, value)
                    is Float -> putDouble(sanitizedKey, value.toDouble())
                    is Boolean -> putBoolean(sanitizedKey, value)
                    else -> putString(sanitizedKey, value.toString())
                }
            }

            // Add event label if present
            event.label?.let {
                putString("label", sanitizePII(it))
            }

            // Add event value if present
            event.value?.let {
                putDouble("value", it)
            }
        }

        firebaseAnalytics?.logEvent(eventName, bundle)
    }

    override fun setUserProperty(key: String, value: Any) {
        val sanitizedKey = sanitizeFirebaseParameterName(key)
        val sanitizedValue = when (value) {
            is String -> sanitizePII(value)
            else -> value.toString()
        }

        firebaseAnalytics?.setUserProperty(sanitizedKey, sanitizedValue)
    }

    override fun persist() {
        // Firebase automatically persists events
    }

    override fun getProviderName(): String = "Firebase"

    /**
     * Sanitize event name to conform to Firebase requirements
     * Max 40 characters, alphanumeric + underscore only
     */
    private fun sanitizeFirebaseEventName(name: String): String {
        return name
            .replace(Regex("[^a-zA-Z0-9_]"), "_")
            .take(40)
            .lowercase()
    }

    /**
     * Sanitize parameter name to conform to Firebase requirements
     * Max 40 characters, alphanumeric + underscore only
     */
    private fun sanitizeFirebaseParameterName(name: String): String {
        return name
            .replace(Regex("[^a-zA-Z0-9_]"), "_")
            .take(40)
            .lowercase()
    }

    /**
     * Sanitizes personally identifiable information (PII) from strings
     * GDPR-compliant: removes file paths, URLs, emails, usernames, IP addresses
     */
    private fun sanitizePII(input: String): String {
        var sanitized = input

        // Firebase has a 100-character limit for parameter values
        if (sanitized.length > 100) {
            sanitized = sanitized.take(97) + "..."
        }

        // Remove file paths
        sanitized = sanitized.replace(Regex("/[\\w/.-]+"), "[FILE_PATH]")

        // Remove URLs
        sanitized = sanitized.replace(Regex("https?://[\\w.-]+(/[\\w.-]*)*"), "[URL]")

        // Remove email addresses
        sanitized = sanitized.replace(Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"), "[EMAIL]")

        // Remove IP addresses
        sanitized = sanitized.replace(Regex("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b"), "[IP]")

        // Remove potential usernames
        sanitized = sanitized.replace(Regex("(?i)(user|username|login|account)\\s*[=:]\\s*\\S+"), "$1=[REDACTED]")

        // Remove potential passwords
        sanitized = sanitized.replace(Regex("(?i)(password|passwd|pwd|pass)\\s*[=:]\\s*\\S+"), "$1=[REDACTED]")

        // Remove potential tokens/keys
        sanitized = sanitized.replace(Regex("(?i)(token|key|secret|api[-_]?key)\\s*[=:]\\s*\\S+"), "$1=[REDACTED]")

        return sanitized
    }
}
