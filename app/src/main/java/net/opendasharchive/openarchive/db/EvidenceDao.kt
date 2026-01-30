package net.opendasharchive.openarchive.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EvidenceDao {
    @Query("SELECT * FROM evidence WHERE collectionId = :collectionId ORDER BY status, id DESC")
    fun observeByCollection(collectionId: Long): Flow<List<EvidenceEntity>>

    @Query("SELECT * FROM evidence WHERE projectId = :projectId ORDER BY id DESC")
    fun observeByProject(projectId: Long): Flow<List<EvidenceEntity>>

    @Query("SELECT * FROM evidence WHERE status IN (:statuses) ORDER BY createDate DESC")
    suspend fun getByStatus(statuses: List<Int>): List<EvidenceEntity>

    @Query("SELECT * FROM evidence WHERE id = :id")
    suspend fun getById(id: Long): EvidenceEntity?

    @Query("SELECT COUNT(*) FROM evidence WHERE collectionId = :collectionId")
    suspend fun getCountByCollection(collectionId: Long): Long

    @Upsert
    suspend fun upsert(entity: EvidenceEntity): Long

    @Query("DELETE FROM evidence WHERE id = :id")
    suspend fun deleteById(id: Long)
}
