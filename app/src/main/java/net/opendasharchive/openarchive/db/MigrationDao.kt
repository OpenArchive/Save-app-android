package net.opendasharchive.openarchive.db

import androidx.room.*

@Dao
interface MigrationDao {
    @Query("SELECT * FROM migration_state WHERE id = 1")
    suspend fun getMigrationState(): MigrationStateEntity?

    @Upsert
    suspend fun upsert(state: MigrationStateEntity)
}
