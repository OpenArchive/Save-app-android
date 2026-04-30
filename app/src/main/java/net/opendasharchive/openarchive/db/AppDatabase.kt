package net.opendasharchive.openarchive.db

import android.annotation.SuppressLint
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.TypeConverters
import androidx.room3.AutoMigration
import androidx.room3.DeleteColumn
import androidx.room3.migration.AutoMigrationSpec

@SuppressLint("RestrictedApi")
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
        ),
        AutoMigration(
            from = 2,
            to = 3
        )
    ],
    version = 3,
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
