package net.opendasharchive.openarchive.analytics.di

import net.opendasharchive.openarchive.analytics.api.AnalyticsManager
import net.opendasharchive.openarchive.analytics.api.AnalyticsManagerImpl
import net.opendasharchive.openarchive.analytics.api.session.SessionTracker
import net.opendasharchive.openarchive.analytics.api.session.SessionTrackerImpl
import net.opendasharchive.openarchive.analytics.core.AnalyticsProvider
import net.opendasharchive.openarchive.analytics.crash.AcraCrashReporter
import net.opendasharchive.openarchive.analytics.crash.CrashReporter
import net.opendasharchive.openarchive.analytics.providers.cleaninsights.CleanInsightsProvider
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * FOSS Analytics Module - CleanInsights only
 *
 * This module is used for F-Droid builds and only includes CleanInsights,
 * a privacy-focused, GDPR-compliant analytics provider.
 *
 * Note: Enhanced analytics is NOT available in FOSS builds.
 * The enhancedAnalyticsEnabled parameter is ignored.
 *
 * Usage in app module:
 * ```kotlin
 * startKoin {
 *     modules(
 *         analyticsModule(
 *             mixpanelToken = getString(R.string.mixpanel_key),
 *             cleanInsightsConsentChecker = { CleanInsightsManager.hasConsent() },
 *             enhancedAnalyticsEnabled = BuildConfig.ENHANCED_ANALYTICS_ENABLED
 *         )
 *     )
 * }
 * ```
 */
fun analyticsModule(
    mixpanelToken: String,  // Ignored in FOSS build, kept for signature compatibility
    cleanInsightsConsentChecker: () -> Boolean,
    enhancedAnalyticsEnabled: Boolean = false  // Ignored in FOSS build
) = module {

    // CleanInsights Provider - Privacy-focused analytics
    single<AnalyticsProvider>(qualifier = org.koin.core.qualifier.named("cleaninsights")) {
        CleanInsightsProvider(
            context = androidContext(),
            campaignId = "main",
            consentChecker = cleanInsightsConsentChecker
        )
    }

    // AnalyticsManager - Unified interface with only CleanInsights
    single<AnalyticsManager> {
        AnalyticsManagerImpl(
            providers = listOf(
                get(qualifier = org.koin.core.qualifier.named("cleaninsights"))
            )
        )
    }

    // SessionTracker - Reactive session management
    single<SessionTracker> {
        SessionTrackerImpl(
            analyticsManager = get(),
            context = androidContext()
        )
    }

    // Crash Reporting - ACRA
    single<CrashReporter> { AcraCrashReporter(androidContext()) }
}
