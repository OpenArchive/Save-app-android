package net.opendasharchive.openarchive.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        VaultEntity::class,
        ArchiveEntity::class,
        SubmissionEntity::class,
        EvidenceEntity::class,
        MigrationStateEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vaultDao(): VaultDao
    abstract fun archiveDao(): ArchiveDao
    abstract fun submissionDao(): SubmissionDao
    abstract fun evidenceDao(): EvidenceDao
    abstract fun migrationDao(): MigrationDao
}
