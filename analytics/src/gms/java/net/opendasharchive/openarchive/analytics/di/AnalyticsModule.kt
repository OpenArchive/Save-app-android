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
import net.opendasharchive.openarchive.analytics.providers.mixpanel.MixpanelProvider
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Koin dependency injection module for analytics
 *
 * Provides:
 * - Analytics providers (Mixpanel, Firebase, CleanInsights)
 * - AnalyticsManager (unified interface)
 * - SessionTracker (reactive session management)
 *
 * Usage in app module:
 * ```kotlin
 * startKoin {
 *     modules(
 *         analyticsModule(
 *             mixpanelToken = getString(R.string.mixpanel_key),
 *             cleanInsightsConsentChecker = { CleanInsightsManager.hasConsent() }
 *         )
 *     )
 * }
 * ```
 */
fun analyticsModule(
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
