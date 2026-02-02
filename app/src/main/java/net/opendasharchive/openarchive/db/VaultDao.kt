package net.opendasharchive.openarchive.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {
    @Query("SELECT * FROM vaults")
    fun observeVaults(): Flow<List<VaultEntity>>

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
