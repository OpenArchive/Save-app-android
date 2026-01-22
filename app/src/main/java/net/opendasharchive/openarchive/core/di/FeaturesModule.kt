package net.opendasharchive.openarchive.core.di

import android.app.Application
import android.content.ContentResolver
import net.opendasharchive.openarchive.features.internetarchive.internetArchiveModule
import net.opendasharchive.openarchive.core.repositories.CollectionRepository
import net.opendasharchive.openarchive.core.repositories.MediaRepository
import net.opendasharchive.openarchive.core.repositories.ProjectRepository
import net.opendasharchive.openarchive.core.repositories.SpaceRepository
import net.opendasharchive.openarchive.core.repositories.SugarCollectionRepository
import net.opendasharchive.openarchive.core.repositories.SugarMediaRepository
import net.opendasharchive.openarchive.core.repositories.SugarProjectRepository
import net.opendasharchive.openarchive.core.repositories.SugarSpaceRepository
import net.opendasharchive.openarchive.features.main.ui.HomeViewModel
import net.opendasharchive.openarchive.features.main.ui.MainMediaViewModel
import net.opendasharchive.openarchive.features.media.PreviewMediaViewModel
import net.opendasharchive.openarchive.features.media.ReviewMediaViewModel
import net.opendasharchive.openarchive.features.spaces.SpaceListViewModel
import net.opendasharchive.openarchive.features.spaces.SpaceSetupViewModel
import net.opendasharchive.openarchive.services.SaveClientFactory
import net.opendasharchive.openarchive.services.SaveClientFactoryImpl
import net.opendasharchive.openarchive.services.webdav.WebDavRepository
import net.opendasharchive.openarchive.services.webdav.login.WebDavLoginViewModel
import net.opendasharchive.openarchive.services.webdav.detail.WebDavDetailViewModel
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
import net.opendasharchive.openarchive.upload.JobSchedulerUploadJobScheduler
import net.opendasharchive.openarchive.upload.UploadJobScheduler
import net.opendasharchive.openarchive.features.main.ui.AppRoute
import net.opendasharchive.openarchive.features.main.ui.Navigator
import net.opendasharchive.openarchive.features.settings.passcode.AppConfig
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.android.ext.koin.androidApplication

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
    single<SpaceRepository> { SugarSpaceRepository(get(named("io"))) }
    single<ProjectRepository> { SugarProjectRepository(get(named("io"))) }
    single<CollectionRepository> { SugarCollectionRepository(get(named("io"))) }
    single<MediaRepository> { SugarMediaRepository(get(named("io"))) }

    viewModelOf(::SnowbirdGroupViewModel)
    viewModelOf(::SnowbirdFileViewModel)
    viewModelOf(::SnowbirdRepoViewModel)

    viewModelOf(::HomeViewModel)
    viewModelOf(::SpaceListViewModel)
    viewModelOf(::SpaceSetupViewModel)

    // Main Media (Home Screen)
    viewModelOf(::MainMediaViewModel)

    // Media Review
    single<ContentResolver> {
        get<Application>().contentResolver
    }
    viewModelOf(::ReviewMediaViewModel)
    viewModelOf(::PreviewMediaViewModel)

    // WebDAV
    single<SaveClientFactory> { SaveClientFactoryImpl(get()) }
    single { WebDavRepository(get(), get()) }

    viewModelOf(::WebDavLoginViewModel)
    viewModelOf(::WebDavDetailViewModel)

    // Upload Manager
    viewModelOf(::UploadManagerViewModel)

    // Upload scheduling
    single<UploadJobScheduler> { JobSchedulerUploadJobScheduler(androidApplication()) }
}
