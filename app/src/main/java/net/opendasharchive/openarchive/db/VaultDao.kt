package net.opendasharchive.openarchive.db

import androidx.room3.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {
    @Query("SELECT * FROM vaults WHERE type IN (0, 1)")
    fun observeVaults(): Flow<List<VaultEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM vaults WHERE type = 5)")
    fun observeHasDwebSpace(): Flow<Boolean>

    @Query("SELECT * FROM vaults WHERE id = :id")
    fun observeById(id: Long): Flow<VaultEntity?>

    @Query("SELECT * FROM vaults WHERE id = :id")
    suspend fun getById(id: Long): VaultEntity?

    @Query("SELECT * FROM vaults WHERE type IN (0, 1)")
    suspend fun getAll(): List<VaultEntity>

    @Upsert
    suspend fun upsert(entity: VaultEntity): Long



    @Delete
    suspend fun delete(entity: VaultEntity)
}
