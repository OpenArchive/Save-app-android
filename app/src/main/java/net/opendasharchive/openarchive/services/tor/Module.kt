package net.opendasharchive.openarchive.services.tor

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

internal val torModule = module {
    single<ITorRepository> { TorRepository() }
    viewModel { TorViewModel(get(), get()) }
}