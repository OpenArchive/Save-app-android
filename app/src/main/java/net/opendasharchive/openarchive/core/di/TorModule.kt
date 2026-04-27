package net.opendasharchive.openarchive.core.di

import net.opendasharchive.openarchive.services.tor.TorServiceManager
import net.opendasharchive.openarchive.upload.UploadGate
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Koin module for Tor-related dependencies.
 *
 * Provides TorServiceManager as a singleton to manage the embedded Tor service.
 */
val torModule = module {
    single { TorServiceManager(androidContext(), get()) }
    single { UploadGate(androidApplication(), get(), get(), get()) }
}
