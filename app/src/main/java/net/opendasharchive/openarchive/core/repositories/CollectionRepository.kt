package net.opendasharchive.openarchive.core.repositories

import kotlinx.coroutines.flow.Flow
import net.opendasharchive.openarchive.core.domain.Submission

interface CollectionRepository {
    suspend fun getCollections(projectId: Long): List<Submission>
    fun observeCollections(projectId: Long): Flow<List<Submission>>
    suspend fun deleteCollection(id: Long)
}