package net.opendasharchive.openarchive.core.di

import net.opendasharchive.openarchive.services.webdav.data.WebDavAuthenticator
import net.opendasharchive.openarchive.services.webdav.data.WebDavRepository
import net.opendasharchive.openarchive.services.webdav.presentation.login.WebDavLoginViewModel
import net.opendasharchive.openarchive.services.webdav.presentation.detail.WebDavDetailViewModel
import net.opendasharchive.openarchive.services.SaveClientFactory
import net.opendasharchive.openarchive.services.SaveClientFactoryImpl
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val webDavModule = module {
    single<SaveClientFactory> { SaveClientFactoryImpl(get()) }
    single { WebDavAuthenticator(get()) }
    single { WebDavRepository(get()) }

    viewModelOf(::WebDavLoginViewModel)
    viewModelOf(::WebDavDetailViewModel)
}
