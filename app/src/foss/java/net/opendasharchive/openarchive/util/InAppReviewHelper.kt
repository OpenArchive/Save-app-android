package net.opendasharchive.openarchive.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.analytics.api.AnalyticsManager

/**
 * FOSS InAppReviewHelper stub for F-Droid builds
 *
 * Shows a custom dialog directing users to rate the app on F-Droid instead of using
 * Google Play In-App Review API (which is not available in FOSS builds).
 */
object InAppReviewHelper {
    private const val KEY_LAUNCH_COUNT = "launch_count"
    private const val KEY_LAST_REVIEW_TIME = "last_review_time"
    private const val MIN_LAUNCHES = 5
    private const val THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000

    fun init(context: Context) {
        Prefs.load(context)
    }

    fun requestReviewInfo(context: Context, analyticsManager: AnalyticsManager) {
        // No-op for FOSS builds - no pre-fetching needed
    }

    fun onAppLaunched(): Boolean {
        val previousCount = Prefs.getInt(KEY_LAUNCH_COUNT, 0)
        val newCount = previousCount + 1
        Prefs.putInt(KEY_LAUNCH_COUNT, newCount)

        val lastReviewTime = Prefs.getLong(KEY_LAST_REVIEW_TIME, 0L)
        val now = System.currentTimeMillis()

        return newCount >= MIN_LAUNCHES && (now - lastReviewTime) >= THIRTY_DAYS_MS
    }

    fun showReviewIfPossible(activity: Activity, reviewManager: Any?, analyticsManager: AnalyticsManager) {
        try {
            // Show custom F-Droid review dialog
            AlertDialog.Builder(activity)
                .setTitle(R.string.rate_app_title)
                .setMessage(R.string.rate_app_message_fdroid)
                .setPositiveButton(R.string.rate_now) { _, _ ->
                    val fdroidUri = Uri.parse("https://f-droid.org/packages/${activity.packageName}")
                    activity.startActivity(Intent(Intent.ACTION_VIEW, fdroidUri))
                    markReviewDone()
                }
                .setNegativeButton(R.string.later, null)
                .show()
        } catch (e: Exception) {
            // Silently fail if dialog can't be shown
        }
    }

    fun markReviewDone() {
        Prefs.putInt(KEY_LAUNCH_COUNT, 0)
        Prefs.putLong(KEY_LAST_REVIEW_TIME, System.currentTimeMillis())
    }
}
