package net.opendasharchive.openarchive.core.di

import android.app.Application
import android.content.Context
import net.opendasharchive.openarchive.features.internetarchive.internetArchiveModule
import net.opendasharchive.openarchive.features.settings.passcode.AppConfig
import net.opendasharchive.openarchive.features.settings.passcode.HapticManager
import net.opendasharchive.openarchive.features.settings.passcode.HashingStrategy
import net.opendasharchive.openarchive.features.settings.passcode.PBKDF2HashingStrategy
import net.opendasharchive.openarchive.features.settings.passcode.passcode_entry.PasscodeEntryViewModel
import net.opendasharchive.openarchive.features.settings.passcode.PasscodeRepository
import net.opendasharchive.openarchive.features.settings.passcode.passcode_setup.PasscodeSetupViewModel
import net.opendasharchive.openarchive.services.snowbird.ISnowbirdFileRepository
import net.opendasharchive.openarchive.services.snowbird.ISnowbirdGroupRepository
import net.opendasharchive.openarchive.services.snowbird.ISnowbirdRepoRepository
import net.opendasharchive.openarchive.services.snowbird.SnowbirdFileRepository
import net.opendasharchive.openarchive.services.snowbird.SnowbirdFileViewModel
import net.opendasharchive.openarchive.services.snowbird.SnowbirdGroupRepository
import net.opendasharchive.openarchive.services.snowbird.SnowbirdGroupViewModel
import net.opendasharchive.openarchive.services.snowbird.SnowbirdRepoRepository
import net.opendasharchive.openarchive.services.snowbird.SnowbirdRepoViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val featuresModule = module {
    includes(internetArchiveModule)
    // TODO: have some registry of feature modules




//    single<ISnowbirdFileRepository> { SnowbirdFileRepository(get(named("retrofit"))) }
//    single<ISnowbirdGroupRepository> { SnowbirdGroupRepository(get(named("retrofit"))) }
//    single<ISnowbirdRepoRepository> { SnowbirdRepoRepository(get(named("retrofit"))) }

    single<ISnowbirdFileRepository> { SnowbirdFileRepository(get(named("unixSocket"))) }
    single<ISnowbirdGroupRepository> { SnowbirdGroupRepository(get(named("unixSocket"))) }
    single<ISnowbirdRepoRepository> { SnowbirdRepoRepository(get(named("unixSocket"))) }
    viewModel { (application: Application) -> SnowbirdGroupViewModel(application, get()) }
    viewModel { (application: Application) -> SnowbirdFileViewModel(application, get()) }
    viewModel { (application: Application) -> SnowbirdRepoViewModel(application, get()) }
}