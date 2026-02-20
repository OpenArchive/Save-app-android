package net.opendasharchive.openarchive.core.di

import net.opendasharchive.openarchive.services.internetarchive.data.InternetArchiveAuthenticator
import net.opendasharchive.openarchive.services.internetarchive.data.InternetArchiveRepository
import net.opendasharchive.openarchive.services.internetarchive.presentation.details.InternetArchiveDetailsViewModel
import net.opendasharchive.openarchive.services.internetarchive.presentation.login.InternetArchiveLoginViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val internetArchiveModule = module {
    single { InternetArchiveAuthenticator(get(), get()) }
    single { InternetArchiveRepository() }

    viewModelOf(::InternetArchiveDetailsViewModel)
    viewModelOf(::InternetArchiveLoginViewModel)
}
