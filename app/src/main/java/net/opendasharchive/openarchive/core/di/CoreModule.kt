package net.opendasharchive.openarchive.core.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import net.opendasharchive.openarchive.core.config.AppConfig
import net.opendasharchive.openarchive.core.security.C2paKeyStore
import net.opendasharchive.openarchive.core.security.SecurityManager
import net.opendasharchive.openarchive.features.core.dialog.DefaultResourceProvider
import net.opendasharchive.openarchive.features.core.dialog.DialogStateManager
import net.opendasharchive.openarchive.features.core.dialog.ResourceProvider
import net.opendasharchive.openarchive.features.folders.BrowseFoldersViewModel
import net.opendasharchive.openarchive.features.folders.CreateNewFolderViewModel
import net.opendasharchive.openarchive.features.settings.FolderDetailViewModel
import net.opendasharchive.openarchive.features.settings.FoldersViewModel
import net.opendasharchive.openarchive.features.settings.SpaceSetupSuccessViewModel
import net.opendasharchive.openarchive.features.settings.license.SetupLicenseViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val coreModule = module {

    single {
        AppConfig(
            passcodeLength = 6,
            enableHapticFeedback = true,
            maxRetryLimitEnabled = false,
            biometricAuthEnabled = false,
            maxFailedAttempts = 5,
            isDwebEnabled = true,
            useCustomCamera = true,
            useMocks = false, // Default to true for now as requested for testing
            simulateErrors = false,
            mockDelayMs = 500L
        )
    }

    // Provide a ResourceProvider using the application context.
    single<ResourceProvider> { DefaultResourceProvider(androidApplication()) }

    // Provide the DialogStateManager as a Singleton
    single<DialogStateManager> { DialogStateManager(resourceProvider = get()) }

    // Dispatchers
    single<CoroutineDispatcher>(named("io")) { Dispatchers.IO }
    single<CoroutineDispatcher>(named("main")) { Dispatchers.Main }

    viewModelOf(::BrowseFoldersViewModel)

    viewModelOf(::CreateNewFolderViewModel)

    viewModelOf(::SetupLicenseViewModel)

    viewModelOf(::SpaceSetupSuccessViewModel)

    viewModelOf(::FoldersViewModel)

    viewModelOf(::FolderDetailViewModel)

    // Default SharedPreferences for general settings
    single<SharedPreferences>(named("default_prefs")) {
        PreferenceManager.getDefaultSharedPreferences(androidApplication())
    }

    // Centrally managed security flags
    single { SecurityManager(get(named("default_prefs"))) }

    // Secure storage for C2PA signing keys (Android Keystore backed)
    single { C2paKeyStore(androidApplication()) }
}


