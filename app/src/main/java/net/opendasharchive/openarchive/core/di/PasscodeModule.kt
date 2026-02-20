package net.opendasharchive.openarchive.core.di

import android.content.Context
import android.content.SharedPreferences
import net.opendasharchive.openarchive.core.config.AppConfig
import net.opendasharchive.openarchive.features.settings.passcode.HapticManager
import net.opendasharchive.openarchive.features.settings.passcode.HashingStrategy
import net.opendasharchive.openarchive.features.settings.passcode.PBKDF2HashingStrategy
import net.opendasharchive.openarchive.features.settings.passcode.PasscodeFlowState
import net.opendasharchive.openarchive.features.settings.passcode.PasscodeGate
import net.opendasharchive.openarchive.features.settings.passcode.PasscodeRepository
import net.opendasharchive.openarchive.features.settings.passcode.PrefAead
import net.opendasharchive.openarchive.features.settings.passcode.passcode_entry.PasscodeEntryViewModel
import net.opendasharchive.openarchive.features.settings.passcode.passcode_setup.PasscodeSetupViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val passcodeModule = module {

    single {
        HapticManager(
            appConfig = get<AppConfig>(),
        )
    }

    single<HashingStrategy> {
        PBKDF2HashingStrategy()
    }

    // SharedPreferences injected once
    single<SharedPreferences> {
        get<Context>().applicationContext
            .getSharedPreferences("secret_shared_prefs", Context.MODE_PRIVATE)
    }

    // Crypto primitive — singleton
    single { PrefAead(get<Context>()) }

    single { PasscodeFlowState() }
    single { PasscodeGate(get()) }

    single<PasscodeRepository> {

        PasscodeRepository(
            prefs = get(),
            config = get(),
            hashingStrategy = get(),
            aead = get(),
        )
    }

    viewModelOf(::PasscodeEntryViewModel)
    viewModelOf(::PasscodeSetupViewModel)
}
