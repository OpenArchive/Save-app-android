package net.opendasharchive.openarchive.util

import android.app.Activity
import android.content.Context
import net.opendasharchive.openarchive.analytics.api.AnalyticsManager

@Suppress("UNUSED_PARAMETER")
object InAppReviewHelper {
    fun init(context: Context) = Unit
    fun requestReviewInfo(context: Context, analyticsManager: AnalyticsManager) = Unit
    fun onAppLaunched(): Boolean = false
    fun showReviewIfPossible(activity: Activity, reviewManager: Any?, analyticsManager: AnalyticsManager) = Unit
    fun markReviewDone() = Unit
}
