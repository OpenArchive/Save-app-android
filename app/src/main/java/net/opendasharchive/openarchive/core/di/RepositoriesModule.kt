package net.opendasharchive.openarchive.core.di

import net.opendasharchive.openarchive.core.repositories.CollectionRepository
import net.opendasharchive.openarchive.core.repositories.MediaRepository
import net.opendasharchive.openarchive.core.repositories.ProjectRepository
import net.opendasharchive.openarchive.core.repositories.SpaceRepository
import net.opendasharchive.openarchive.core.repositories.SugarCollectionRepository
import net.opendasharchive.openarchive.core.repositories.SugarMediaRepository
import net.opendasharchive.openarchive.core.repositories.SugarProjectRepository
import net.opendasharchive.openarchive.core.repositories.SugarSpaceRepository
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.core.repositories.*
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

val repositoriesModule = module {
    single { FileCleanupHelper(androidContext()) }

    // Home/Main repositories
    single<SpaceRepository> {
        if (Prefs.isRoomMigrated) get<VaultRepositoryImpl>()
        else SugarSpaceRepository(androidContext(), get(named("io")))
    }
    single<ProjectRepository> {
        if (Prefs.isRoomMigrated) get<ArchiveRepositoryImpl>()
        else SugarProjectRepository(get(), get(named("io")))
    }
    single<CollectionRepository> {
        if (Prefs.isRoomMigrated) get<SubmissionRepositoryImpl>()
        else SugarCollectionRepository(get(named("io")))
    }
    single<MediaRepository> {
        if (Prefs.isRoomMigrated) get<EvidenceRepositoryImpl>()
        else SugarMediaRepository(get(), get(named("io")))
    }
}
