package net.opendasharchive.openarchive.core.di

import net.opendasharchive.openarchive.features.settings.passcode.AppConfig
import net.opendasharchive.openarchive.services.snowbird.presentation.dashboard.SnowbirdDashboardViewModel
import net.opendasharchive.openarchive.services.snowbird.presentation.file.SnowbirdFileViewModel
import net.opendasharchive.openarchive.services.snowbird.presentation.group.SnowbirdCreateGroupViewModel
import net.opendasharchive.openarchive.services.snowbird.presentation.group.SnowbirdGroupViewModel
import net.opendasharchive.openarchive.services.snowbird.presentation.repo.SnowbirdRepoViewModel
import net.opendasharchive.openarchive.services.snowbird.service.repository.ISnowbirdFileRepository
import net.opendasharchive.openarchive.services.snowbird.service.repository.ISnowbirdGroupRepository
import net.opendasharchive.openarchive.services.snowbird.service.repository.ISnowbirdRepoRepository
import net.opendasharchive.openarchive.services.snowbird.service.repository.MockSnowbirdFileRepository
import net.opendasharchive.openarchive.services.snowbird.service.repository.MockSnowbirdGroupRepository
import net.opendasharchive.openarchive.services.snowbird.service.repository.MockSnowbirdRepoRepository
import net.opendasharchive.openarchive.services.snowbird.service.repository.SnowbirdFileRepository
import net.opendasharchive.openarchive.services.snowbird.service.repository.SnowbirdGroupRepository
import net.opendasharchive.openarchive.services.snowbird.service.repository.SnowbirdRepoRepository
import net.opendasharchive.openarchive.services.snowbird.util.SnowbirdFileStorage
import net.opendasharchive.openarchive.util.ProcessingTracker
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val snowbirdModule = module {
    // Each ViewModel gets its own instance of ProcessingTracker
    factory { ProcessingTracker() }

    single<ISnowbirdFileRepository> {
        val appConfig = get<AppConfig>()
        if (appConfig.useMocks) {
            MockSnowbirdFileRepository(
                assetManager = androidContext().assets,
                config = appConfig,
                evidenceDao = get(),
                archiveDao = get(),
                dwebDao = get()
            )
        } else {
            SnowbirdFileRepository(
                api = get(named("retrofit")),
                evidenceDao = get(),
                archiveDao = get(),
                dwebDao = get()
            )
        }
    }

    single<ISnowbirdGroupRepository> {
        val appConfig = get<AppConfig>()
        if (appConfig.useMocks) {
            MockSnowbirdGroupRepository(
                assetManager = androidContext().assets,
                config = appConfig,
                vaultDao = get(),
                dwebDao = get()
            )
        } else {
            SnowbirdGroupRepository(
                api = get(named("retrofit")),
                vaultDao = get(),
                dwebDao = get()
            )
        }
    }

    single<ISnowbirdRepoRepository> {
        val appConfig = get<AppConfig>()
        if (appConfig.useMocks) {
            MockSnowbirdRepoRepository(
                assetManager = androidContext().assets,
                config = appConfig,
                archiveDao = get(),
                submissionDao = get(),
                dwebDao = get()
            )
        } else {
            SnowbirdRepoRepository(
                api = get(named("retrofit")),
                archiveDao = get(),
                submissionDao = get(),
                dwebDao = get()
            )
        }
    }

    single { SnowbirdFileStorage(get()) }

    viewModelOf(::SnowbirdGroupViewModel)
    viewModelOf(::SnowbirdFileViewModel)
    viewModelOf(::SnowbirdRepoViewModel)
    viewModelOf(::SnowbirdDashboardViewModel)
    viewModelOf(::SnowbirdCreateGroupViewModel)
}
