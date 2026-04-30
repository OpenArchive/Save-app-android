package net.opendasharchive.openarchive.core.di

import androidx.room3.Room
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.db.AppDatabase
import net.opendasharchive.openarchive.core.repositories.*
import net.opendasharchive.openarchive.core.security.TinkVaultCredentialStore
import net.opendasharchive.openarchive.core.security.VaultCredentialStore
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidApplication(),
            AppDatabase::class.java,
            "openarchive.db_room"
        )
//            .setQueryCallback(queryCallback = { sqlQuery, bindArgs ->
//                AppLogger.d("SQL Query: $sqlQuery, Bind Args: $bindArgs")
//            }, executor = Dispatchers.IO.asExecutor()  )
            .build()
    }

    single { get<AppDatabase>().vaultDao() }
    single { get<AppDatabase>().archiveDao() }
    single { get<AppDatabase>().submissionDao() }
    single { get<AppDatabase>().evidenceDao() }
    single { get<AppDatabase>().migrationDao() }
    single { get<AppDatabase>().dwebDao() }

    single {
        PreferenceDataStoreFactory.create(
            produceFile = { androidContext().preferencesDataStoreFile("settings") }
        )
    }

    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
    single<VaultCredentialStore> { TinkVaultCredentialStore(androidContext(), get(named("io"))) }

    single { VaultRepositoryImpl(androidContext(), get(), get(), get(), get(), get(named("io"))) }
    single { ArchiveRepositoryImpl(get(), get(), get(), get(), get(), get(named("io"))) }
    single { SubmissionRepositoryImpl(get(), get(named("io"))) }
    single { EvidenceRepositoryImpl(get(), get(), get(), get(), get(named("io"))) }
}
