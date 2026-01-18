package net.opendasharchive.openarchive.core.logger

import android.app.Application
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.analytics.api.AnalyticsManager
import net.opendasharchive.openarchive.analytics.crash.CrashReporter
import net.opendasharchive.openarchive.analytics.crash.FirebaseCrashReporter
import timber.log.Timber


/**
 * A utility object for centralized logging in Android applications.
 * Integrates with Timber, Crash Reporting, and Analytics for comprehensive error tracking.
 *
 * GMS Version - Uses Firebase Crashlytics via CrashReporter abstraction
 *
 * Features:
 * - Logs to Logcat via Timber
 * - Sends errors to Firebase Crashlytics with breadcrumbs
 * - Tracks critical errors in Analytics (GDPR-compliant)
 * - User journey breadcrumbs for crash analysis
 */
object AppLogger {

    private var crashReporter: CrashReporter? = null
    private var currentScreen: String = "Unknown"
    private var analyticsManager: AnalyticsManager? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * Initialize crash reporting (ACRA for FOSS builds).
     * No-op for GMS builds - Firebase Crashlytics is initialized automatically.
     */
    @Suppress("UNUSED_PARAMETER")
    fun initAcra(app: Application) {
        // No-op for GMS builds - Firebase Crashlytics handles crash reporting
    }

    /**
     * Initializes the logger
     * @param context The context used to initialize services
     * @param initDebugger Legacy parameter (unused)
     */
    fun init(context: Context, initDebugger: Boolean) {
        Timber.plant(DebugTreeWithTag())

        try {
            crashReporter = FirebaseCrashReporter().apply {
                initialize()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize Crash Reporter")
        }
    }

    /**
     * Set analytics manager for error tracking
     */
    fun setAnalyticsManager(manager: AnalyticsManager) {
        analyticsManager = manager
    }

    /**
     * Set current screen for breadcrumb context
     */
    fun setCurrentScreen(screenName: String) {
        currentScreen = screenName
        crashReporter?.log("Screen: $screenName")
    }

    /**
     * Add breadcrumb for user journey tracking
     * This helps understand what user was doing before a crash
     */
    fun breadcrumb(action: String, details: String? = null) {
        val breadcrumb = if (details != null) "$action: $details" else action
        crashReporter?.log("[$currentScreen] $breadcrumb")
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
        crashReporter?.log("ERROR: $fullMessage")
    }

    /**
     * Log error with exception
     * Sends to Crash Reporter + Analytics
     */
    fun e(message: String, throwable: Throwable) {
        Timber.e(throwable, message)

        // Send to Crash Reporter (non-fatal exception)
        crashReporter?.let {
            it.log("[$currentScreen] ERROR: $message")
            it.recordException(throwable)
        }

        // Track in Analytics (GDPR-safe - only error category, no PII)
        val errorCategory = categorizeError(throwable)
        analyticsManager?.let { manager ->
            scope.launch {
                manager.trackError(
                    errorCategory = errorCategory,
                    screenName = currentScreen
                )
            }
        }
    }

    /**
     * Log exception only
     * Sends to Crash Reporter + Analytics
     */
    fun e(throwable: Throwable) {
        Timber.e(throwable)

        // Send to Crash Reporter (non-fatal exception)
        crashReporter?.let {
            it.log("[$currentScreen] EXCEPTION: ${throwable.message}")
            it.recordException(throwable)
        }

        // Track in Analytics (GDPR-safe)
        val errorCategory = categorizeError(throwable)
        analyticsManager?.let { manager ->
            scope.launch {
                manager.trackError(
                    errorCategory = errorCategory,
                    screenName = currentScreen
                )
            }
        }
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
