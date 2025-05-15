package net.opendasharchive.openarchive.core.di

import net.opendasharchive.openarchive.services.tor.ITorRepository
import net.opendasharchive.openarchive.services.tor.TorRepository
import net.opendasharchive.openarchive.services.tor.TorViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal val torModule = module {
    single<ITorRepository>(named("tor")) { TorRepository() }
    viewModel { TorViewModel(get(), get(named("tor"))) }
}