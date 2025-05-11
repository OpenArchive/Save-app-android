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
}
