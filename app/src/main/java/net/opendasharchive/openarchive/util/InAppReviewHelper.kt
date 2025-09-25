package net.opendasharchive.openarchive.util

import android.app.Activity
import android.content.Context
import net.opendasharchive.openarchive.BuildConfig
import net.opendasharchive.openarchive.core.logger.AppLogger

object InAppReviewHelper {
    private const val KEY_LAUNCH_COUNT = "launch_count"
    private const val KEY_LAST_REVIEW_TIME = "last_review_time"
    private const val MIN_LAUNCHES = 5
    private const val THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000

    private var reviewInfo: Any? = null

    fun init(context: Context) {
        Prefs.load(context)
    }

    fun incrementLaunchCountAndShowReviewIfNeeded(activity: Activity) {
        if (!BuildConfig.INCLUDE_GOOGLE_SERVICES) {
            AppLogger.d("InAppReviewHelper", "In-app review not available in F-Droid build")
            return
        }

        val currentCount = Prefs.getInt(KEY_LAUNCH_COUNT, 0) + 1
        Prefs.putInt(KEY_LAUNCH_COUNT, currentCount)

        if (shouldRequestReview()) {
            requestReviewFlow(activity)
        }
    }

    fun shouldRequestReview(): Boolean {
        if (!BuildConfig.INCLUDE_GOOGLE_SERVICES) return false

        val launchCount = Prefs.getInt(KEY_LAUNCH_COUNT, 0)
        val lastReviewTime = Prefs.getLong(KEY_LAST_REVIEW_TIME, 0)
        val currentTime = System.currentTimeMillis()

        return launchCount >= MIN_LAUNCHES &&
                (currentTime - lastReviewTime) > THIRTY_DAYS_MS
    }

    fun requestReviewFlow(activity: Activity) {
        if (!BuildConfig.INCLUDE_GOOGLE_SERVICES) return

        try {
            val reviewManagerFactoryClass = Class.forName("com.google.android.play.core.review.ReviewManagerFactory")
            val createMethod = reviewManagerFactoryClass.getDeclaredMethod("create", Context::class.java)
            val manager = createMethod.invoke(null, activity)

            val requestReviewFlowMethod = manager.javaClass.getDeclaredMethod("requestReviewFlow")
            val request = requestReviewFlowMethod.invoke(manager)
            // Simplified for F-Droid compatibility
        } catch (e: Exception) {
            AppLogger.e("InAppReviewHelper", "Error requesting review flow", e)
        }
    }

    fun requestReviewInfo(context: Context) {
        // No-op for F-Droid builds
    }

    fun onAppLaunched(): Boolean {
        if (!BuildConfig.INCLUDE_GOOGLE_SERVICES) return false

        val lastLaunchTime = Prefs.getLong("last_launch_time", 0)
        val currentTime = System.currentTimeMillis()
        Prefs.putLong("last_launch_time", currentTime)

        return (currentTime - lastLaunchTime) > (10 * 60 * 1000)
    }

    fun showReviewIfPossible(activity: Activity) {
        if (!BuildConfig.INCLUDE_GOOGLE_SERVICES) return
        // Implementation would use reflection for Google Play Services
    }

    fun launchReviewFlow(activity: Activity) {
        if (!BuildConfig.INCLUDE_GOOGLE_SERVICES) return
        // Implementation would use reflection for Google Play Services
    }

    fun markReviewDone() {
        Prefs.putLong(KEY_LAST_REVIEW_TIME, System.currentTimeMillis())
    }
}