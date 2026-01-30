package net.opendasharchive.openarchive.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {
    @Query("SELECT * FROM vaults")
    fun observeSpaces(): Flow<List<VaultEntity>>

    @Query("SELECT * FROM vaults WHERE id = :id")
    fun observeById(id: Long): Flow<VaultEntity?>

    @Query("SELECT * FROM vaults WHERE id = :id")
    suspend fun getById(id: Long): VaultEntity?

    @Query("SELECT * FROM vaults")
    suspend fun getAll(): List<VaultEntity>

    @Upsert
    suspend fun upsert(entity: VaultEntity): Long

    @Delete
    suspend fun delete(entity: VaultEntity)
}
