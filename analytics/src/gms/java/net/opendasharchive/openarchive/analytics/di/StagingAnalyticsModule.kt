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
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Koin dependency injection module for STAGING/DEV analytics.
 *
 * Key differences from production analyticsModule:
 * - Uses EnhancedMixpanelProvider with user identification
 * - NO PII sanitization - full details visible for debugging
 * - Exposes EnhancedMixpanelProvider directly for user identification
 *
 * WARNING: This module should ONLY be used in staging/dev builds.
 * Production builds must use the standard analyticsModule.
 *
 * Usage in app module:
 * ```kotlin
 * val analyticsModule = if (BuildConfig.ENHANCED_ANALYTICS_ENABLED) {
 *     stagingAnalyticsModule(
 *         mixpanelToken = getString(R.string.mixpanel_key),
 *         cleanInsightsConsentChecker = { CleanInsightsManager.hasConsent() }
 *     )
 * } else {
 *     analyticsModule(
 *         mixpanelToken = getString(R.string.mixpanel_key),
 *         cleanInsightsConsentChecker = { CleanInsightsManager.hasConsent() }
 *     )
 * }
 * ```
 */
fun stagingAnalyticsModule(
    mixpanelToken: String,
    cleanInsightsConsentChecker: () -> Boolean
) = module {

    // Enhanced Mixpanel Provider - with user identification & NO PII sanitization
    // Also exposed as concrete type for direct access to identify() method
    single {
        EnhancedMixpanelProvider(
            context = androidContext(),
            token = mixpanelToken
        )
    }

    // Bind as AnalyticsProvider for the manager
    single<AnalyticsProvider>(qualifier = named("mixpanel")) {
        get<EnhancedMixpanelProvider>()
    }

    // Firebase Provider - same as production
    single<AnalyticsProvider>(qualifier = named("firebase")) {
        FirebaseProvider(
            context = androidContext()
        )
    }

    // CleanInsights Provider - same as production
    single<AnalyticsProvider>(qualifier = named("cleaninsights")) {
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
                get(qualifier = named("mixpanel")),
                get(qualifier = named("firebase")),
                get(qualifier = named("cleaninsights"))
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
