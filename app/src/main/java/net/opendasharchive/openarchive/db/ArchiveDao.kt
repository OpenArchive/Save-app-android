package net.opendasharchive.openarchive.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ArchiveDao {
    @Query("SELECT * FROM archives WHERE spaceId = :spaceId AND archived = :archived ORDER BY id DESC")
    fun observeBySpace(spaceId: Long, archived: Boolean): Flow<List<ArchiveEntity>>

    @Query("SELECT * FROM archives WHERE id = :id")
    fun observeById(id: Long): Flow<ArchiveEntity?>

    @Query("SELECT * FROM archives WHERE id = :id")
    suspend fun getById(id: Long): ArchiveEntity?

    @Query("SELECT * FROM archives WHERE spaceId = :spaceId AND description = :name LIMIT 1")
    suspend fun getByName(spaceId: Long, name: String): ArchiveEntity?

    @Query("UPDATE archives SET licenseUrl = :licenseUrl WHERE spaceId = :spaceId")
    suspend fun updateLicenseForSpace(spaceId: Long, licenseUrl: String?)

    @Upsert
    suspend fun upsert(entity: ArchiveEntity): Long

    @Delete
    suspend fun delete(entity: ArchiveEntity)
}
