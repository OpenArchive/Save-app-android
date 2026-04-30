package net.opendasharchive.openarchive.analytics.crash

/**
 * Interface for crash reporting abstraction
 *
 * Allows different implementations for GMS (Firebase Crashlytics) and FOSS (ACRA) builds.
 * This abstraction ensures that crash reporting functionality can be swapped based on
 * the build variant without changing the calling code.
 */
interface CrashReporter {
    /**
     * Initialize the crash reporter
     * Should be called once during app startup
     */
    fun initialize()

    /**
     * Log a message to the crash reporter
     * Used for breadcrumbs and context information
     *
     * @param message The message to log
     */
    fun log(message: String)

    /**
     * Record an exception to the crash reporter
     * For non-fatal exceptions that should be tracked
     *
     * @param throwable The exception to record
     */
    fun recordException(throwable: Throwable)

    /**
     * Set a user identifier for crash reports
     * Helps associate crashes with specific users (when privacy allows)
     *
     * @param identifier The user identifier (should be anonymized if needed)
     */
    fun setUserIdentifier(identifier: String)
}
