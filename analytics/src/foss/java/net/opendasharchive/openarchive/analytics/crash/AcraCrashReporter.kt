package net.opendasharchive.openarchive.analytics.crash

import android.content.Context
import org.acra.ACRA
import timber.log.Timber

/**
 * ACRA implementation of CrashReporter for FOSS builds
 *
 * Provides crash reporting functionality using ACRA (Application Crash Reports for Android),
 * a FOSS alternative to proprietary crash reporting services. Used in F-Droid builds to
 * maintain FOSS compliance while still providing crash reporting capabilities.
 *
 * ACRA is privacy-focused and allows users to control what data is sent.
 *
 * @param context Application context needed for ACRA operations
 */
class AcraCrashReporter(private val context: Context) : CrashReporter {

    override fun initialize() {
        try {
            // ACRA is initialized in SaveApp.attachBaseContext()
            // This method just confirms it's ready
            Timber.d("ACRA Crash Reporter ready")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize ACRA")
        }
    }

    override fun log(message: String) {
        try {
            // ACRA doesn't support breadcrumbs like Crashlytics
            // We use custom data instead to store context
            ACRA.errorReporter.putCustomData("last_log", message)
        } catch (e: Exception) {
            Timber.e(e, "Failed to log message to ACRA")
        }
    }

    override fun recordException(throwable: Throwable) {
        try {
            // Record non-fatal exception
            ACRA.errorReporter.handleSilentException(throwable)
        } catch (e: Exception) {
            Timber.e(e, "Failed to record exception to ACRA")
        }
    }

    override fun setUserIdentifier(identifier: String) {
        // For privacy in FOSS builds, we don't set user identifiers
        // Users appreciate F-Droid builds being more privacy-focused
        // If needed in the future, you could set an anonymized identifier here
    }
}
