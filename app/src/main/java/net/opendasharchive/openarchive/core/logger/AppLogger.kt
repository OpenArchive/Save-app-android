package net.opendasharchive.openarchive.core.logger

import android.content.Context
import com.google.firebase.crashlytics.FirebaseCrashlytics
import net.opendasharchive.openarchive.core.analytics.AnalyticsEvent
import net.opendasharchive.openarchive.core.analytics.AnalyticsManager
import net.opendasharchive.openarchive.core.logger.AppLogger.init
import timber.log.Timber


/**
 * A utility object for centralized logging in Android applications.
 * Integrates with Timber, Firebase Crashlytics, and Analytics for comprehensive error tracking.
 *
 * Features:
 * - Logs to Logcat via Timber
 * - Sends errors to Firebase Crashlytics with breadcrumbs
 * - Tracks critical errors in Analytics (GDPR-compliant)
 * - User journey breadcrumbs for crash analysis
 */
object AppLogger {

    private var crashlytics: FirebaseCrashlytics? = null
    private var currentScreen: String = "Unknown"

    /**
     * Initializes the logger
     * @param context The context used to initialize services
     * @param initDebugger Legacy parameter (unused)
     */
    fun init(context: Context, initDebugger: Boolean) {
        Timber.plant(DebugTreeWithTag())

        try {
            crashlytics = FirebaseCrashlytics.getInstance()
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize Firebase Crashlytics")
        }
    }

    /**
     * Set current screen for breadcrumb context
     */
    fun setCurrentScreen(screenName: String) {
        currentScreen = screenName
        crashlytics?.log("Screen: $screenName")
    }

    /**
     * Add breadcrumb for user journey tracking
     * This helps understand what user was doing before a crash
     */
    fun breadcrumb(action: String, details: String? = null) {
        val breadcrumb = if (details != null) "$action: $details" else action
        crashlytics?.log("[$currentScreen] $breadcrumb")
    }

    // Info Level Logging
    // REMOVED Analytics.log() - info logs are for debugging, not analytics
    fun i(message: String, vararg args: Any?) {
        Timber.i(message + args.joinToString(" "))
    }

    fun i(message: String, throwable: Throwable) {
        Timber.i(throwable, message)
    }

    // Debug Level Logging
    fun d(message: String, vararg args: Any?) {
        Timber.d(message + args.joinToString(" "))
    }

    fun d(message: String, throwable: Throwable) {
        Timber.d(throwable, message)
    }

    // Error Level Logging
    /**
     * Log error message only (no exception)
     * This is for minor errors that don't require stack traces
     */
    fun e(message: String, vararg args: Any?) {
        val fullMessage = message + args.joinToString(" ")
        Timber.e(fullMessage)

        // Add breadcrumb for context
        crashlytics?.log("ERROR: $fullMessage")
    }

    /**
     * Log error with exception
     * Sends to Firebase Crashlytics + Analytics
     */
    fun e(message: String, throwable: Throwable) {
        Timber.e(throwable, message)

        // Send to Firebase Crashlytics (non-fatal exception)
        crashlytics?.let {
            it.log("[$currentScreen] ERROR: $message")
            it.recordException(throwable)
        }

        // Track in Analytics (GDPR-safe - only error category, no PII)
        val errorCategory = categorizeError(throwable)
        AnalyticsManager.trackError(
            errorCategory = errorCategory,
            screenName = currentScreen
        )
    }

    /**
     * Log exception only
     * Sends to Firebase Crashlytics + Analytics
     */
    fun e(throwable: Throwable) {
        Timber.e(throwable)

        // Send to Firebase Crashlytics (non-fatal exception)
        crashlytics?.let {
            it.log("[$currentScreen] EXCEPTION: ${throwable.message}")
            it.recordException(throwable)
        }

        // Track in Analytics (GDPR-safe)
        val errorCategory = categorizeError(throwable)
        AnalyticsManager.trackError(
            errorCategory = errorCategory,
            screenName = currentScreen
        )
    }

    /**
     * Categorize error for analytics (GDPR-safe)
     */
    private fun categorizeError(throwable: Throwable): String {
        return when (throwable) {
            is java.io.IOException -> "network"
            is java.io.FileNotFoundException -> "file_not_found"
            is SecurityException -> "permission"
            is IllegalStateException -> "illegal_state"
            is IllegalArgumentException -> "illegal_argument"
            is NullPointerException -> "null_pointer"
            is OutOfMemoryError -> "out_of_memory"
            else -> throwable::class.simpleName ?: "unknown"
        }
    }

    // Warning Level Logging
    fun w(message: String, vararg args: Any?) {
        Timber.w("%s%s", message, args.joinToString(" "))
    }

    fun w(message: String, throwable: Throwable) {
        Timber.w(throwable, message)
    }

    // Verbose Level Logging
    fun v(message: String, vararg args: Any?) {
        Timber.v("%s%s", message, args.joinToString(" "))
    }

    // Tagged Logging Methods
    fun tagD(tag: String, message: String, vararg args: Any?) {
        Timber.tag(tag).d("%s%s", message, args.joinToString(" "))
    }

    fun tagI(tag: String, message: String, vararg args: Any?) {
        Timber.tag(tag).i("%s%s", message, args.joinToString(" "))
    }

    fun tagE(tag: String, message: String, vararg args: Any?) {
        Timber.tag(tag).e("%s%s", message, args.joinToString(" "))
    }

    private class DebugTreeWithTag : Timber.DebugTree() {
        override fun createStackElementTag(element: StackTraceElement): String? {
            // Customize the tag to include the class name and line number
            return "${element.fileName}:${element.lineNumber}"
        }
    }


    val imageLogger = object : coil3.util.Logger {
        override var minLevel: coil3.util.Logger.Level = coil3.util.Logger.Level.Verbose

        override fun log(
            tag: String,
            level: coil3.util.Logger.Level,
            message: String?,
            throwable: Throwable?
        ) {
            Timber.tag("Coil3:$tag").log(level.ordinal, throwable, message)
        }
    }
}