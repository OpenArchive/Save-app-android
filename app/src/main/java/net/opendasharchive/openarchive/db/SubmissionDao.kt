package net.opendasharchive.openarchive.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SubmissionDao {
    @Query("SELECT * FROM submissions WHERE projectId = :projectId ORDER BY id DESC")
    fun observeByProject(projectId: Long): Flow<List<SubmissionEntity>>

    @Query("SELECT * FROM submissions WHERE id = :id")
    suspend fun getById(id: Long): SubmissionEntity?

    @Upsert
    suspend fun upsert(entity: SubmissionEntity): Long

    @Delete
    suspend fun delete(entity: SubmissionEntity)
}
