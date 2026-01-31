package net.opendasharchive.openarchive.core.repositories

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.core.domain.Submission
import net.opendasharchive.openarchive.core.domain.mappers.toDomain
import net.opendasharchive.openarchive.core.domain.mappers.toEntity
import net.opendasharchive.openarchive.db.sugar.Collection

class SugarCollectionRepository(private val io: CoroutineDispatcher = Dispatchers.IO) : CollectionRepository {

    override suspend fun getCollections(projectId: Long): List<Submission> =
        withContext(io) {
            Collection.getByProjectRecentFirst(projectId).map { it.toDomain() }
        }

    override fun observeCollections(projectId: Long): Flow<List<Submission>> {
        return InvalidationBus.collections
            .map { getCollections(projectId) }
            .distinctUntilChanged()
    }

    override suspend fun getCollection(id: Long): Submission? = withContext(io) {
        Collection.get(id)?.toDomain()
    }

    override suspend fun updateCollection(submission: Submission) {
        withContext(io) {
            submission.toEntity().save()
            InvalidationBus.invalidateCollections()
        }
    }

    override suspend fun deleteCollection(id: Long) {
        withContext(io) {
            val deleted = Collection.get(id)?.delete() ?: false
            if (deleted) InvalidationBus.invalidateCollections()
        }
    }
}