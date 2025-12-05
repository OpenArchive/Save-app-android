package net.opendasharchive.openarchive

import android.app.Activity
import android.content.Context
import android.content.Intent
import net.opendasharchive.openarchive.features.settings.ConsentActivity
import org.cleaninsights.sdk.*

@Suppress("unused")
object CleanInsightsManager  {

    private const val CI_CAMPAIGN = "main"

    private var mCi: CleanInsights? = null

    private var mCompleted: ((granted: Boolean) -> Unit)? = null

    fun init(context: Context) {
        mCi = CleanInsights(
            context.assets.open("cleaninsights.json").reader().use { it.readText() },
            context.filesDir)
    }

    fun hasConsent(): Boolean {
        return mCi?.isCampaignCurrentlyGranted(CI_CAMPAIGN) ?: false
    }

    fun deny() {
        mCi?.deny(CI_CAMPAIGN)

        mCompleted?.invoke(false)
        mCompleted = null
    }

    fun grant() {
        mCi?.grant(CI_CAMPAIGN)
        mCi?.grant(Feature.Lang)

        mCompleted?.invoke(true)
        mCompleted = null
    }

    fun getConsent(context: Activity, completed: (granted: Boolean) -> Unit) {
        if (mCi == null) {
            return completed(false)
        }

        mCi?.requestConsent(CI_CAMPAIGN, object : ConsentRequestUi {
            override fun show(
                campaignId: String,
                campaign: Campaign,
                complete: ConsentRequestUiComplete
            ) {
                mCompleted = completed

                context.startActivity(Intent(context, ConsentActivity::class.java))
            }

            override fun show(feature: Feature, complete: ConsentRequestUiComplete) {
                complete(true)
            }
        }, completed)
    }

    fun measureView(view: String) {
        mCi?.measureVisit(listOf(view), CI_CAMPAIGN)
    }

    fun measureEvent(category: String, action: String, name: String? = null, value: Double? = null) {
        mCi?.measureEvent(category, action, CI_CAMPAIGN, name, value)
    }

    fun persist() {
        mCi?.persist()
    }

    // ========== Enhanced Tracking Methods ==========

    /**
     * Track screen views with optional time spent
     * @param screenName Name of the screen (e.g., "MainActivity", "SettingsFragment")
     * @param timeSpentSeconds Optional time spent on screen in seconds
     */
    fun trackScreenView(screenName: String, timeSpentSeconds: Long? = null) {
        measureView(screenName)
        timeSpentSeconds?.let {
            measureEvent("screen_time", "view_duration", screenName, it.toDouble())
        }
    }

    /**
     * Track navigation between screens
     * @param fromScreen Source screen name
     * @param toScreen Destination screen name
     * @param trigger What triggered the navigation (e.g., "button_click", "back_press")
     */
    fun trackNavigation(fromScreen: String, toScreen: String, trigger: String? = null) {
        measureEvent("navigation", "screen_change", "$fromScreen->$toScreen")
        trigger?.let {
            measureEvent("navigation", "trigger", it)
        }
    }

    /**
     * Track backend/server usage
     * @param action Action performed (e.g., "configured", "upload_started", "upload_completed")
     * @param backendType Type of backend (e.g., "Internet Archive", "Private Server", "DWeb Service", "Storacha")
     * @param value Optional numeric value (e.g., duration, file size)
     */
    fun trackBackendAction(action: String, backendType: String, value: Double? = null) {
        measureEvent("backend", action, backendType, value)
    }

    /**
     * Track upload events
     * @param backendType Type of backend
     * @param success Whether upload succeeded
     * @param durationSeconds Optional duration in seconds
     * @param fileSizeKB Optional file size in KB
     */
    fun trackUpload(
        backendType: String,
        success: Boolean,
        durationSeconds: Long? = null,
        fileSizeKB: Long? = null
    ) {
        val action = if (success) "upload_completed" else "upload_failed"
        measureEvent("upload", action, backendType, durationSeconds?.toDouble())

        fileSizeKB?.let {
            measureEvent("upload", "file_size", backendType, it.toDouble())
        }
    }

    /**
     * Track download events
     * @param backendType Type of backend
     * @param success Whether download succeeded
     * @param durationSeconds Optional duration in seconds
     */
    fun trackDownload(backendType: String, success: Boolean, durationSeconds: Long? = null) {
        val action = if (success) "download_completed" else "download_failed"
        measureEvent("download", action, backendType, durationSeconds?.toDouble())
    }

    /**
     * Track media capture/selection
     * @param action Action performed (e.g., "captured", "selected", "deleted")
     * @param mediaType Type of media (e.g., "photo", "video", "document")
     * @param source Source of media (e.g., "camera", "gallery", "files")
     * @param count Number of items
     */
    fun trackMediaAction(action: String, mediaType: String? = null, source: String? = null, count: Int? = null) {
        measureEvent("media", action, mediaType ?: "unknown", count?.toDouble())
        source?.let {
            measureEvent("media", "source", it)
        }
    }

    /**
     * Track app lifecycle events
     * @param event Event type (e.g., "app_opened", "app_closed", "app_backgrounded")
     * @param sessionDurationSeconds Optional session duration in seconds
     * @param isFirstLaunch Whether this is the first app launch
     */
    fun trackAppLifecycle(event: String, sessionDurationSeconds: Long? = null, isFirstLaunch: Boolean? = null) {
        measureEvent("app", event, null, sessionDurationSeconds?.toDouble())
        isFirstLaunch?.let {
            if (it) measureEvent("app", "first_launch", null)
        }
    }

    /**
     * Track feature usage
     * @param feature Feature name (e.g., "proofmode", "tor", "dark_mode")
     * @param enabled Whether the feature was enabled or disabled
     */
    fun trackFeatureToggle(feature: String, enabled: Boolean) {
        val action = if (enabled) "enabled" else "disabled"
        measureEvent("feature", action, feature)
    }

    /**
     * Track errors (GDPR-compliant - no PII)
     * @param errorCategory Category of error (e.g., "network", "permission", "upload", "auth")
     * @param screenName Screen where error occurred
     * @param backendType Optional backend type if relevant
     */
    fun trackError(errorCategory: String, screenName: String, backendType: String? = null) {
        measureEvent("error", errorCategory, screenName)
        backendType?.let {
            measureEvent("error", "backend", it)
        }
    }

    /**
     * Track session start
     * @param isFirstSession Whether this is the user's first session
     */
    fun trackSessionStart(isFirstSession: Boolean = false) {
        measureEvent("session", "started", if (isFirstSession) "first" else "returning")
    }

    /**
     * Track session end
     * @param lastScreen Last screen user was on
     * @param durationSeconds Session duration in seconds
     */
    fun trackSessionEnd(lastScreen: String, durationSeconds: Long) {
        measureEvent("session", "ended", lastScreen, durationSeconds.toDouble())
    }
}
