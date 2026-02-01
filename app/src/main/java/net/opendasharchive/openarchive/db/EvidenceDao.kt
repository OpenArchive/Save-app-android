package net.opendasharchive.openarchive.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EvidenceDao {
    @Query("""
        SELECT * FROM evidence 
        WHERE collectionId = :collectionId 
        ORDER BY 
            CASE 
                WHEN status IN (2, 4) THEN 0 -- Active (Queued, Uploading)
                WHEN status = 9 THEN 1       -- Error
                WHEN status IN (0, 1) THEN 2    -- Local/New
                WHEN status = 5 THEN 3      -- Uploaded
                ELSE 4
            END,
            CASE WHEN status = 5 THEN uploadDate ELSE 0 END DESC,
            priority DESC, 
            id DESC
    """)
    fun observeByCollection(collectionId: Long): Flow<List<EvidenceEntity>>

    @Query("""
        SELECT * FROM evidence 
        WHERE projectId = :projectId 
        ORDER BY 
            CASE 
                WHEN status IN (2, 4) THEN 0 -- Active
                WHEN status = 9 THEN 1       -- Error
                WHEN status IN (0, 1) THEN 2    -- Local/New
                WHEN status = 5 THEN 3      -- Uploaded
                ELSE 4
            END,
            CASE WHEN status = 5 THEN uploadDate ELSE 0 END DESC,
            priority DESC, 
            id DESC
    """)
    fun observeByProject(projectId: Long): Flow<List<EvidenceEntity>>

    @Query("SELECT * FROM evidence WHERE status IN (:statuses) ORDER BY priority DESC, id DESC")
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
