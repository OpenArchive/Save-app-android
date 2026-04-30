package net.opendasharchive.openarchive.db

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SubmissionDao {
    @Query("SELECT * FROM submissions WHERE archiveId = :archiveId ORDER BY id DESC")
    fun observeByArchive(archiveId: Long): Flow<List<SubmissionEntity>>

    @Query("SELECT * FROM submissions WHERE id = :id")
    suspend fun getById(id: Long): SubmissionEntity?

    @Upsert
    suspend fun upsert(entity: SubmissionEntity): Long

    @Delete
    suspend fun delete(entity: SubmissionEntity)
}
