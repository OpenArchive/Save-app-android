package net.opendasharchive.openarchive.core.di

import android.app.Application
import android.content.ContentResolver
import net.opendasharchive.openarchive.features.main.ui.Navigator
import net.opendasharchive.openarchive.features.main.ui.HomeViewModel
import net.opendasharchive.openarchive.features.main.ui.MainMediaViewModel
import net.opendasharchive.openarchive.features.media.PreviewMediaViewModel
import net.opendasharchive.openarchive.features.media.ReviewMediaViewModel
import net.opendasharchive.openarchive.features.spaces.SpaceListViewModel
import net.opendasharchive.openarchive.features.spaces.SpaceSetupViewModel
import net.opendasharchive.openarchive.upload.JobSchedulerUploadJobScheduler
import net.opendasharchive.openarchive.upload.UploadJobScheduler
import net.opendasharchive.openarchive.upload.UploadManagerViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.scope.dsl.activityRetainedScope
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val featuresModule = module {
    includes(
        internetArchiveModule,
        webDavModule,
        snowbirdModule,
        repositoriesModule
    )

    activityRetainedScope {
        scoped { Navigator() }
    }

    viewModelOf(::HomeViewModel)
    viewModelOf(::SpaceListViewModel)
    viewModelOf(::SpaceSetupViewModel)
    viewModelOf(::MainMediaViewModel)

    single<ContentResolver> {
        get<Application>().contentResolver
    }
    viewModelOf(::ReviewMediaViewModel)
    viewModelOf(::PreviewMediaViewModel)

    viewModelOf(::UploadManagerViewModel)
    single<UploadJobScheduler> { JobSchedulerUploadJobScheduler(androidApplication()) }
}
