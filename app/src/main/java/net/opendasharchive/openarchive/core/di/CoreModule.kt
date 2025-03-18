package net.opendasharchive.openarchive.core.di

import android.content.Context
import com.google.api.services.drive.Drive
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import net.opendasharchive.openarchive.features.core.dialog.DefaultResourceProvider
import net.opendasharchive.openarchive.features.core.dialog.DialogStateManager
import net.opendasharchive.openarchive.features.core.dialog.ResourceProvider
import net.opendasharchive.openarchive.features.folders.BrowseFoldersViewModel
import net.opendasharchive.openarchive.features.main.MainViewModel
import net.opendasharchive.openarchive.features.main.ui.HomeViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val coreModule = module {
    // Provide a ResourceProvider using the application context.
    single<ResourceProvider> { DefaultResourceProvider(androidApplication()) }

    // Provide DialogStateManager and let Koin inject the ResourceProvider.
    viewModel { DialogStateManager(get()) }

    viewModel { HomeViewModel() }

    viewModel {
        MainViewModel()
    }

    viewModel {
        BrowseFoldersViewModel(
            context = get<Context>()
        )
    }
}


