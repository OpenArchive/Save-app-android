package net.opendasharchive.openarchive.db

import androidx.room3.*
import kotlinx.coroutines.flow.Flow
import net.opendasharchive.openarchive.core.domain.EvidenceStatus

@Dao
interface EvidenceDao {
    @Query("""
        SELECT * FROM evidence 
        WHERE submissionId = :submissionId 
        ORDER BY 
            CASE 
                WHEN status IN (2, 4) THEN 0 -- Active (Queued, Uploading)
                WHEN status = 9 THEN 1       -- Error
                WHEN status IN (0, 1) THEN 2    -- Local/New
                WHEN status = 5 THEN 3      -- Uploaded
                ELSE 4
            END,
            CASE WHEN status = 5 THEN uploadedAt ELSE 0 END DESC,
            priority DESC, 
            id DESC
    """)
    fun observeBySubmission(submissionId: Long): Flow<List<EvidenceEntity>>

    @Query("""
        SELECT * FROM evidence 
        WHERE archiveId = :archiveId 
        ORDER BY 
            CASE 
                WHEN status IN (2, 4) THEN 0 -- Active
                WHEN status = 9 THEN 1       -- Error
                WHEN status IN (0, 1) THEN 2    -- Local/New
                WHEN status = 5 THEN 3      -- Uploaded
                ELSE 4
            END,
            CASE WHEN status = 5 THEN uploadedAt ELSE 0 END DESC,
            priority DESC, 
            id DESC
    """)
    fun observeByArchive(archiveId: Long): Flow<List<EvidenceEntity>>

    @Query("SELECT * FROM evidence WHERE archiveId = :archiveId")
    suspend fun getByArchive(archiveId: Long): List<EvidenceEntity>


    @Query("SELECT * FROM evidence WHERE status IN (:statuses) ORDER BY priority DESC, id DESC")
    suspend fun getByStatus(statuses: List<EvidenceStatus>): List<EvidenceEntity>

    @Query("SELECT * FROM evidence WHERE status IN (:statuses) ORDER BY priority DESC, id DESC")
    fun observeByStatus(statuses: List<EvidenceStatus>): Flow<List<EvidenceEntity>>

    @Query("SELECT * FROM evidence WHERE id = :id")
    suspend fun getById(id: Long): EvidenceEntity?

    @Query("SELECT COUNT(*) FROM evidence WHERE submissionId = :submissionId")
    suspend fun getCountBySubmission(submissionId: Long): Long

    @Query("SELECT id FROM evidence WHERE archiveId = :archiveId AND mediaHashString = :hash LIMIT 1")
    suspend fun getEvidenceIdByHash(archiveId: Long, hash: String): Long?

    @Upsert
    suspend fun upsert(entity: EvidenceEntity): Long



    @Query("DELETE FROM evidence WHERE id = :id")
    suspend fun deleteById(id: Long)
}
