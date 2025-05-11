package net.opendasharchive.openarchive.services.tor

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal val torModule = module {
    single<ITorRepository>(named("tor")) { TorRepository() }
    viewModel { TorViewModel(get(), get(named("tor"))) }
}