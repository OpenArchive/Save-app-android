package net.opendasharchive.openarchive.analytics.di

import net.opendasharchive.openarchive.analytics.api.AnalyticsManager
import net.opendasharchive.openarchive.analytics.api.AnalyticsManagerImpl
import net.opendasharchive.openarchive.analytics.api.session.SessionTracker
import net.opendasharchive.openarchive.analytics.api.session.SessionTrackerImpl
import net.opendasharchive.openarchive.analytics.core.AnalyticsProvider
import net.opendasharchive.openarchive.analytics.crash.CrashReporter
import net.opendasharchive.openarchive.analytics.crash.FirebaseCrashReporter
import net.opendasharchive.openarchive.analytics.providers.cleaninsights.CleanInsightsProvider
import net.opendasharchive.openarchive.analytics.providers.firebase.FirebaseProvider
import net.opendasharchive.openarchive.analytics.providers.mixpanel.EnhancedMixpanelProvider
import net.opendasharchive.openarchive.analytics.providers.mixpanel.MixpanelProvider
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Factory function that returns the appropriate analytics module based on build configuration.
 *
 * @param mixpanelToken The Mixpanel API token
 * @param cleanInsightsConsentChecker Function that checks if user has given consent
 * @param enhancedAnalyticsEnabled Whether to use enhanced analytics (staging/dev) or production analytics
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
    mixpanelToken: String,
    cleanInsightsConsentChecker: () -> Boolean,
    enhancedAnalyticsEnabled: Boolean = false
): Module = if (enhancedAnalyticsEnabled) {
    stagingAnalyticsModule(mixpanelToken, cleanInsightsConsentChecker)
} else {
    productionAnalyticsModule(mixpanelToken, cleanInsightsConsentChecker)
}

/**
 * Koin dependency injection module for PRODUCTION analytics.
 *
 * Features:
 * - Anonymous user tracking (no identify() calls)
 * - PII sanitization enabled
 * - Standard Mixpanel, Firebase, and CleanInsights providers
 */
fun productionAnalyticsModule(
    mixpanelToken: String,
    cleanInsightsConsentChecker: () -> Boolean
) = module {

    // Providers - Each provider is a singleton
    single<AnalyticsProvider>(qualifier = org.koin.core.qualifier.named("mixpanel")) {
        MixpanelProvider(
            context = androidContext(),
            token = mixpanelToken
        )
    }

    single<AnalyticsProvider>(qualifier = org.koin.core.qualifier.named("firebase")) {
        FirebaseProvider(
            context = androidContext()
        )
    }

    single<AnalyticsProvider>(qualifier = org.koin.core.qualifier.named("cleaninsights")) {
        CleanInsightsProvider(
            context = androidContext(),
            campaignId = "main",
            consentChecker = cleanInsightsConsentChecker
        )
    }

    // AnalyticsManager - Unified interface for all providers
    single<AnalyticsManager> {
        AnalyticsManagerImpl(
            providers = listOf(
                get(qualifier = org.koin.core.qualifier.named("mixpanel")),
                get(qualifier = org.koin.core.qualifier.named("firebase")),
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

    // Crash Reporting - Firebase Crashlytics
    single<CrashReporter> { FirebaseCrashReporter() }
}
