package net.opendasharchive.openarchive.core.di

import android.app.Application
import net.opendasharchive.openarchive.features.internetarchive.internetArchiveModule
import net.opendasharchive.openarchive.features.main.data.CollectionRepository
import net.opendasharchive.openarchive.features.main.data.MediaRepository
import net.opendasharchive.openarchive.features.main.data.ProjectRepository
import net.opendasharchive.openarchive.features.main.data.SpaceRepository
import net.opendasharchive.openarchive.features.main.data.SugarCollectionRepository
import net.opendasharchive.openarchive.features.main.data.SugarMediaRepository
import net.opendasharchive.openarchive.features.main.data.SugarProjectRepository
import net.opendasharchive.openarchive.features.main.data.SugarSpaceRepository
import net.opendasharchive.openarchive.features.main.ui.HomeViewModel
import net.opendasharchive.openarchive.features.main.ui.MainMediaViewModel
import net.opendasharchive.openarchive.features.media.PreviewMediaViewModel
import net.opendasharchive.openarchive.features.media.ReviewMediaViewModel
import net.opendasharchive.openarchive.features.spaces.SpaceListViewModel
import net.opendasharchive.openarchive.services.SaveClientFactory
import net.opendasharchive.openarchive.services.SaveClientFactoryImpl
import net.opendasharchive.openarchive.services.webdav.WebDavRepository
import net.opendasharchive.openarchive.services.webdav.WebDavViewModel
import net.opendasharchive.openarchive.services.snowbird.ISnowbirdFileRepository
import net.opendasharchive.openarchive.services.snowbird.ISnowbirdGroupRepository
import net.opendasharchive.openarchive.services.snowbird.ISnowbirdRepoRepository
import net.opendasharchive.openarchive.services.snowbird.SnowbirdFileRepository
import net.opendasharchive.openarchive.services.snowbird.SnowbirdFileViewModel
import net.opendasharchive.openarchive.services.snowbird.SnowbirdGroupRepository
import net.opendasharchive.openarchive.services.snowbird.SnowbirdGroupViewModel
import net.opendasharchive.openarchive.services.snowbird.SnowbirdRepoRepository
import net.opendasharchive.openarchive.services.snowbird.SnowbirdRepoViewModel
import net.opendasharchive.openarchive.upload.UploadManagerViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val featuresModule = module {
    includes(internetArchiveModule)
    // TODO: have some registry of feature modules


    single<ISnowbirdFileRepository> { SnowbirdFileRepository(get(named("retrofit"))) }
    single<ISnowbirdGroupRepository> { SnowbirdGroupRepository(get(named("retrofit"))) }
    single<ISnowbirdRepoRepository> { SnowbirdRepoRepository(get(named("retrofit"))) }

//    single<ISnowbirdFileRepository> { SnowbirdFileRepository(get(named("unixSocket"))) }
//    single<ISnowbirdGroupRepository> { SnowbirdGroupRepository(get(named("unixSocket"))) }
//    single<ISnowbirdRepoRepository> { SnowbirdRepoRepository(get(named("unixSocket"))) }

    // Home/Main repositories (Sugar-backed)
    single<SpaceRepository> { SugarSpaceRepository() }
    single<ProjectRepository> { SugarProjectRepository() }
    single<CollectionRepository> { SugarCollectionRepository() }
    single<MediaRepository> { SugarMediaRepository() }

    viewModel { (application: Application) ->
        SnowbirdGroupViewModel(
            application = application,
            repository = get()
        )
    }

    viewModel { (application: Application) ->
        SnowbirdFileViewModel(
            application = application,
            repository = get()
        )
    }

    viewModel { (application: Application) ->
        SnowbirdRepoViewModel(
            application = application,
            repository = get()
        )
    }

    viewModel { HomeViewModel(get(), get()) }

    viewModelOf(::SpaceListViewModel)

    // Main Media (Home Screen)
    viewModel { (projectId: Long) ->
        MainMediaViewModel(
            projectId = projectId,
            collectionRepository = get(),
            mediaRepository = get(),
            projectRepository = get()
        )
    }

    // Media Review
    viewModel { (savedStateHandle: androidx.lifecycle.SavedStateHandle) ->
        ReviewMediaViewModel(savedStateHandle, get<Application>().contentResolver)
    }
    viewModel { (savedStateHandle: androidx.lifecycle.SavedStateHandle) ->
        PreviewMediaViewModel(savedStateHandle)
    }

    // WebDAV
    single<SaveClientFactory> { SaveClientFactoryImpl(get()) }
    single { WebDavRepository(get(), get()) }
    viewModel { WebDavViewModel(get(), get(), get()) }

    // Upload Manager
    viewModelOf(::UploadManagerViewModel)
}
