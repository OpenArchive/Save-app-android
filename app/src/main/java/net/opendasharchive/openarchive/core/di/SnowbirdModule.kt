package net.opendasharchive.openarchive.core.di

import net.opendasharchive.openarchive.services.snowbird.ISnowbirdFileRepository
import net.opendasharchive.openarchive.services.snowbird.ISnowbirdGroupRepository
import net.opendasharchive.openarchive.services.snowbird.ISnowbirdRepoRepository
import net.opendasharchive.openarchive.services.snowbird.SnowbirdDashboardViewModel
import net.opendasharchive.openarchive.services.snowbird.SnowbirdFileRepository
import net.opendasharchive.openarchive.services.snowbird.SnowbirdFileViewModel
import net.opendasharchive.openarchive.services.snowbird.SnowbirdGroupRepository
import net.opendasharchive.openarchive.services.snowbird.SnowbirdGroupViewModel
import net.opendasharchive.openarchive.services.snowbird.SnowbirdRepoRepository
import net.opendasharchive.openarchive.services.snowbird.SnowbirdRepoViewModel
import net.opendasharchive.openarchive.services.snowbird.util.SnowbirdFileStorage
import net.opendasharchive.openarchive.util.ProcessingTracker
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val snowbirdModule = module {
    // Each ViewModel gets its own instance of ProcessingTracker
    factory { ProcessingTracker() }

    single<ISnowbirdFileRepository> { 
        SnowbirdFileRepository(
            api = get(named("retrofit")),
            evidenceDao = get(),
            dwebDao = get()
        ) 
    }
    single<ISnowbirdGroupRepository> { 
        SnowbirdGroupRepository(
            api = get(named("retrofit")),
            vaultDao = get(),
            dwebDao = get()
        ) 
    }
    single<ISnowbirdRepoRepository> { 
        SnowbirdRepoRepository(
            api = get(named("retrofit")),
            archiveDao = get(),
            dwebDao = get()
        ) 
    }

    single { SnowbirdFileStorage(get()) }

    viewModelOf(::SnowbirdGroupViewModel)
    viewModelOf(::SnowbirdFileViewModel)
    viewModelOf(::SnowbirdRepoViewModel)
    viewModelOf(::SnowbirdDashboardViewModel)
}
