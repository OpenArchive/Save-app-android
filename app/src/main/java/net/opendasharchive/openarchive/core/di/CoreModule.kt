package net.opendasharchive.openarchive.core.di

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import net.opendasharchive.openarchive.features.core.dialog.DefaultResourceProvider
import net.opendasharchive.openarchive.features.core.dialog.DialogStateManager
import net.opendasharchive.openarchive.features.core.dialog.ResourceProvider
import net.opendasharchive.openarchive.features.folders.BrowseFoldersViewModel
import net.opendasharchive.openarchive.features.folders.CreateNewFolderViewModel
import net.opendasharchive.openarchive.features.main.MainViewModel
import net.opendasharchive.openarchive.features.settings.FolderDetailViewModel
import net.opendasharchive.openarchive.features.settings.FoldersViewModel
import net.opendasharchive.openarchive.features.settings.SpaceSetupSuccessViewModel
import net.opendasharchive.openarchive.features.settings.license.SetupLicenseViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val coreModule = module {
    // Provide a ResourceProvider using the application context.
    single<ResourceProvider> { DefaultResourceProvider(androidApplication()) }

    // Provide the DialogStateManager as a Singleton
    single { DialogStateManager(resourceProvider = get()) }

    viewModel {
        MainViewModel()
    }

    viewModel {
        BrowseFoldersViewModel(
            webDavRepository = get()
        )
    }

    viewModel {
        CreateNewFolderViewModel()
    }

    viewModelOf(::SetupLicenseViewModel)

    viewModelOf(::SpaceSetupSuccessViewModel)

    viewModelOf(::FoldersViewModel)

    viewModelOf(::FolderDetailViewModel)
}


