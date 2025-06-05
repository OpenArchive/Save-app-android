package net.opendasharchive.openarchive.core.di

import android.content.Context
import net.opendasharchive.openarchive.features.settings.passcode.AppConfig
import net.opendasharchive.openarchive.features.settings.passcode.HapticManager
import net.opendasharchive.openarchive.features.settings.passcode.HashingStrategy
import net.opendasharchive.openarchive.features.settings.passcode.PBKDF2HashingStrategy
import net.opendasharchive.openarchive.features.settings.passcode.PasscodeRepository
import net.opendasharchive.openarchive.features.settings.passcode.passcode_entry.PasscodeEntryViewModel
import net.opendasharchive.openarchive.features.settings.passcode.passcode_setup.PasscodeSetupViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val passcodeModule = module {
    single {
        AppConfig(
            passcodeLength = 6,
            enableHapticFeedback = true,
            maxRetryLimitEnabled = false,
            biometricAuthEnabled = false,
            maxFailedAttempts = 5,
            isDwebEnabled = false
        )
    }

    single {
        HapticManager(
            appConfig = get<AppConfig>(),
        )
    }

    single<HashingStrategy> {
        PBKDF2HashingStrategy()
    }

    single<PasscodeRepository> {
        val hashingStrategy: HashingStrategy = PBKDF2HashingStrategy()

        PasscodeRepository(
            context = get<Context>(),
            config = get<AppConfig>(),
            hashingStrategy = hashingStrategy
        )
    }

    viewModel { PasscodeEntryViewModel(get(), get()) }
    viewModel { PasscodeSetupViewModel(get(), get()) }
}