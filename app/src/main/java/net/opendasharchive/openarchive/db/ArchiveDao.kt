package net.opendasharchive.openarchive.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ArchiveDao {
    @Query("SELECT * FROM archives WHERE vaultId = :vaultId AND archived = :archived ORDER BY id DESC")
    fun observeByVault(vaultId: Long, archived: Boolean): Flow<List<ArchiveEntity>>

    @Query("SELECT * FROM archives WHERE id = :id")
    fun observeById(id: Long): Flow<ArchiveEntity?>

    @Query("SELECT * FROM archives WHERE id = :id")
    suspend fun getById(id: Long): ArchiveEntity?

    @Query("SELECT * FROM archives WHERE vaultId = :vaultId AND description = :name LIMIT 1")
    suspend fun getByName(vaultId: Long, name: String): ArchiveEntity?

    @Query("UPDATE archives SET licenseUrl = :licenseUrl WHERE vaultId = :vaultId")
    suspend fun updateLicenseForVault(vaultId: Long, licenseUrl: String?)

    @Query("UPDATE archives SET isRemote = 0 WHERE vaultId = :vaultId")
    suspend fun resetRemoteStatusForVault(vaultId: Long)



    @Upsert
    suspend fun upsert(entity: ArchiveEntity): Long



    @Delete
    suspend fun delete(entity: ArchiveEntity)
}
