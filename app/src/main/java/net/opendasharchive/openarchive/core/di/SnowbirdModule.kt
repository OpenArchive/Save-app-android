package net.opendasharchive.openarchive.core.di

import net.opendasharchive.openarchive.services.snowbird.ISnowbirdFileRepository
import net.opendasharchive.openarchive.services.snowbird.ISnowbirdGroupRepository
import net.opendasharchive.openarchive.services.snowbird.ISnowbirdRepoRepository
import net.opendasharchive.openarchive.services.snowbird.SnowbirdFileRepository
import net.opendasharchive.openarchive.services.snowbird.SnowbirdFileViewModel
import net.opendasharchive.openarchive.services.snowbird.SnowbirdGroupRepository
import net.opendasharchive.openarchive.services.snowbird.SnowbirdGroupViewModel
import net.opendasharchive.openarchive.services.snowbird.SnowbirdRepoRepository
import net.opendasharchive.openarchive.services.snowbird.SnowbirdRepoViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val snowbirdModule = module {
    single<ISnowbirdFileRepository> { SnowbirdFileRepository(get(named("retrofit"))) }
    single<ISnowbirdGroupRepository> { SnowbirdGroupRepository(get(named("retrofit"))) }
    single<ISnowbirdRepoRepository> { SnowbirdRepoRepository(get(named("retrofit"))) }

    viewModelOf(::SnowbirdGroupViewModel)
    viewModelOf(::SnowbirdFileViewModel)
    viewModelOf(::SnowbirdRepoViewModel)
}
