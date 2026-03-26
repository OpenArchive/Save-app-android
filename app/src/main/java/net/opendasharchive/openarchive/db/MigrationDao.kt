package net.opendasharchive.openarchive.db

import androidx.room3.*

@Dao
interface MigrationDao {
    @Query("SELECT * FROM migration_state WHERE id = 1")
    suspend fun getMigrationState(): MigrationStateEntity?

    @Upsert
    suspend fun upsert(state: MigrationStateEntity)
}
