package net.opendasharchive.openarchive.features.main

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.delay
import net.opendasharchive.openarchive.analytics.api.AnalyticsManager
import net.opendasharchive.openarchive.util.InAppReviewHelper
import org.koin.compose.koinInject

@Composable
fun CheckForInAppReview(
    analyticsManager: AnalyticsManager = koinInject()
) {
    val context = LocalContext.current
    val activity = context as? Activity ?: return

    // Create ReviewManager for launching the flow later
    val reviewManager = remember { ReviewManagerFactory.create(context) }

    LaunchedEffect(Unit) {
        // 1. Asynchronously fetch ReviewInfo (stored in InAppReviewHelper singleton)
        // We do this early to ensure it's ready if we decide to show it.
        InAppReviewHelper.requestReviewInfo(context, analyticsManager)

        // 2. Check eligibility (increment launch count, check criteria)
        val shouldPrompt = InAppReviewHelper.onAppLaunched()

        if (shouldPrompt) {
            // 3. Wait for UI to settle and ReviewInfo to fetch
            // Original logic waited 2s after onResume. We'll wait 3s here to be safe and ensure stability.
            delay(3000)

            // 4. Show review if info is available
            InAppReviewHelper.showReviewIfPossible(activity, reviewManager, analyticsManager)

            // 5. Mark as done to prevent spamming
            InAppReviewHelper.markReviewDone()
        }
    }
}
