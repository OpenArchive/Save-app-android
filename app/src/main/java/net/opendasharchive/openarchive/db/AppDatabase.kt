package net.opendasharchive.openarchive.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.AutoMigration
import androidx.room.DeleteColumn
import androidx.room.migration.AutoMigrationSpec

@Database(
    entities = [
        VaultEntity::class,
        ArchiveEntity::class,
        SubmissionEntity::class,
        EvidenceEntity::class,
        MigrationStateEntity::class,
        VaultDwebEntity::class,
        ArchiveDwebEntity::class,
        EvidenceDwebEntity::class
    ],
    autoMigrations = [
        AutoMigration(
            from = 1,
            to = 2,
            spec = RemoveVaultPasswordColumnMigration::class
        )
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vaultDao(): VaultDao
    abstract fun archiveDao(): ArchiveDao
    abstract fun submissionDao(): SubmissionDao
    abstract fun evidenceDao(): EvidenceDao
    abstract fun migrationDao(): MigrationDao
    abstract fun dwebDao(): DwebDao
}

@DeleteColumn(tableName = "vaults", columnName = "password")
class RemoveVaultPasswordColumnMigration : AutoMigrationSpec