package net.opendasharchive.openarchive.util

import android.app.Activity
import android.content.Context
import com.google.android.play.core.review.ReviewException
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.analytics.api.AnalyticsEvent
import net.opendasharchive.openarchive.analytics.api.AnalyticsManager
import net.opendasharchive.openarchive.core.logger.AppLogger

object InAppReviewHelper {
    // Keys for our Prefs helper:
    private const val KEY_LAUNCH_COUNT = "launch_count"
    private const val KEY_LAST_REVIEW_TIME = "last_review_time"

    // After this many launches, we become eligible:
    private const val MIN_LAUNCHES = 5

    // 30 days in ms
    private const val THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000

    // Once requestReviewFlow() succeeds, we cache this:
    private var reviewInfo: ReviewInfo? = null

    // ReviewManager instance
    private var reviewManager: ReviewManager? = null

    // Coroutine scope for analytics
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * Call once (e.g. in Application.onCreate or first Activity) so that Prefs.load(...) runs.
     */
    fun init(context: Context) {
        Prefs.load(context)
    }

    /**
     * Call early (e.g. in onCreate of MainActivity) to asynchronously fetch ReviewInfo.
     */
    fun requestReviewInfo(context: Context, analyticsManager: AnalyticsManager) {
        reviewManager = ReviewManagerFactory.create(context)
        reviewManager?.requestReviewFlow()
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    reviewInfo = task.result
                    AppLogger.d("InAppReview", "ReviewInfo obtained successfully.")
                    // Track review prompt shown
                    scope.launch {
                        analyticsManager.trackEvent(AnalyticsEvent.ReviewPromptShown)
                    }
                } else {
                    (task.exception as? ReviewException)?.let { ex ->
                        AppLogger.e("InAppReview", "Error requesting review flow: ${ex.errorCode}", ex)
                        // Track review error
                        scope.launch {
                            analyticsManager.trackEvent(AnalyticsEvent.ReviewPromptError(ex.errorCode))
                        }
                    }
                    reviewInfo = null
                }
            }
    }

    /**
     * Call this immediately on app launch (in onCreate). It increments the stored launch count
     * and returns TRUE if we are now eligible to show a review (≥ MIN_LAUNCHES AND ≥ 30 days since last).
     *
     * It does NOT actually show anything. It only says “yes/no.”
     */
    fun onAppLaunched(): Boolean {
        val previousCount = Prefs.getInt(KEY_LAUNCH_COUNT, 0)
        val newCount = previousCount + 1
        Prefs.putInt(KEY_LAUNCH_COUNT, newCount)

        val lastReviewTime = Prefs.getLong(KEY_LAST_REVIEW_TIME, 0L)
        val now = System.currentTimeMillis()

        return if (newCount >= MIN_LAUNCHES && (now - lastReviewTime) >= THIRTY_DAYS_MS) {
            true
        } else {
            false
        }
    }

    /**
     * Once you decide it's time to actually show the prompt (e.g. in onResume, after UI ready),
     * call this. If reviewInfo is non-null it will launch; otherwise it just logs "no Info."
     *
     * @param activity The activity to launch the review flow in
     * @param reviewManagerParam Ignored - kept for API compatibility with FOSS build
     * @param analyticsManager Analytics manager for tracking events
     */
    fun showReviewIfPossible(activity: Activity, reviewManagerParam: Any?, analyticsManager: AnalyticsManager) {
        reviewInfo?.let { info ->
            reviewManager?.launchReviewFlow(activity, info)
                ?.addOnCompleteListener {
                    AppLogger.d("InAppReview", "Review flow finished.")
                    // Track review flow completed
                    scope.launch {
                        analyticsManager.trackEvent(AnalyticsEvent.ReviewPromptCompleted)
                    }
                    reviewInfo = null
                } ?: run {
                AppLogger.d("InAppReview", "ReviewManager was null; cannot launch review flow.")
            }
        } ?: run {
            AppLogger.d("InAppReview", "ReviewInfo was null; cannot launch review flow.")
        }
    }

    /**
     * After you do showReviewIfPossible(...), call this to reset counters.
     * That ensures we won’t prompt again for another 30 days.
     */
    fun markReviewDone() {
        val now = System.currentTimeMillis()
        Prefs.putInt(KEY_LAUNCH_COUNT, 0)
        Prefs.putLong(KEY_LAST_REVIEW_TIME, now)
    }
}