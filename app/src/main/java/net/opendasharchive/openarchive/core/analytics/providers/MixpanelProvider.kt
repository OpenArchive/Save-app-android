package net.opendasharchive.openarchive.core.analytics.providers

import android.content.Context
import com.mixpanel.android.mpmetrics.MixpanelAPI
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.analytics.AnalyticsEvent
import net.opendasharchive.openarchive.core.analytics.AnalyticsProvider
import org.json.JSONObject

/**
 * Mixpanel implementation of AnalyticsProvider
 * With automatic PII sanitization
 */
class MixpanelProvider(
    private val context: Context
) : AnalyticsProvider {

    private var mixpanel: MixpanelAPI? = null

    override fun initialize() {
        val token = context.getString(R.string.mixpanel_key)
        mixpanel = MixpanelAPI.getInstance(context, token, false)
    }

    override fun trackEvent(event: AnalyticsEvent) {
        val eventName = "${event.category}_${event.action}"

        // Convert properties to JSONObject with PII sanitization
        val properties = JSONObject()

        event.properties.forEach { (key, value) ->
            val sanitizedValue = when (value) {
                is String -> sanitizePII(value)
                else -> value
            }
            properties.put(key, sanitizedValue)
        }

        // Add event label if present
        event.label?.let {
            properties.put("label", sanitizePII(it))
        }

        // Add event value if present
        event.value?.let {
            properties.put("value", it)
        }

        mixpanel?.track(eventName, properties)
    }

    override fun setUserProperty(key: String, value: Any) {
        val sanitizedValue = when (value) {
            is String -> sanitizePII(value)
            else -> value
        }

        mixpanel?.people?.set(key, sanitizedValue)
    }

    override fun persist() {
        mixpanel?.flush()
    }

    override fun getProviderName(): String = "Mixpanel"

    /**
     * Sanitizes personally identifiable information (PII) from strings
     * GDPR-compliant: removes file paths, URLs, emails, usernames, IP addresses
     */
    private fun sanitizePII(input: String): String {
        var sanitized = input

        // Remove file paths (e.g., /storage/emulated/0/..., /data/user/...)
        sanitized = sanitized.replace(Regex("/[\\w/.-]+"), "[FILE_PATH]")

        // Remove URLs (http://, https://)
        sanitized = sanitized.replace(Regex("https?://[\\w.-]+(/[\\w.-]*)*"), "[URL]")

        // Remove email addresses
        sanitized = sanitized.replace(Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"), "[EMAIL]")

        // Remove IP addresses
        sanitized = sanitized.replace(Regex("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b"), "[IP_ADDRESS]")

        // Remove potential usernames
        sanitized = sanitized.replace(Regex("(?i)(user|username|login|account)\\s*[=:]\\s*\\S+"), "$1=[REDACTED]")

        // Remove potential passwords
        sanitized = sanitized.replace(Regex("(?i)(password|passwd|pwd|pass)\\s*[=:]\\s*\\S+"), "$1=[REDACTED]")

        // Remove potential tokens/keys
        sanitized = sanitized.replace(Regex("(?i)(token|key|secret|api[-_]?key)\\s*[=:]\\s*\\S+"), "$1=[REDACTED]")

        return sanitized
    }
}
