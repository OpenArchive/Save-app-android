package net.opendasharchive.openarchive.analytics.crash

import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

/**
 * Firebase Crashlytics implementation of CrashReporter for GMS builds
 *
 * Provides crash reporting functionality using Google's Firebase Crashlytics service.
 * Used in Google Play Store builds to track crashes and non-fatal exceptions.
 */
class FirebaseCrashReporter : CrashReporter {
    private var crashlytics: FirebaseCrashlytics? = null

    override fun initialize() {
        try {
            crashlytics = FirebaseCrashlytics.getInstance()
            Timber.d("Firebase Crashlytics initialized successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize Firebase Crashlytics")
        }
    }

    override fun log(message: String) {
        crashlytics?.log(message)
    }

    override fun recordException(throwable: Throwable) {
        crashlytics?.recordException(throwable)
    }

    override fun setUserIdentifier(identifier: String) {
        crashlytics?.setUserId(identifier)
    }
}
