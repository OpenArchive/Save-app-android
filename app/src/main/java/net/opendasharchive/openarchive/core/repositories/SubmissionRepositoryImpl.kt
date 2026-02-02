package net.opendasharchive.openarchive.core.repositories

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.core.domain.Submission
import net.opendasharchive.openarchive.core.domain.mappers.toDomain
import net.opendasharchive.openarchive.core.domain.mappers.toSubmissionEntity
import net.opendasharchive.openarchive.db.SubmissionDao

class SubmissionRepositoryImpl(
    private val submissionDao: SubmissionDao,
    private val io: CoroutineDispatcher = Dispatchers.IO
) : CollectionRepository {

    override suspend fun getCollections(projectId: Long): List<Submission> = withContext(io) {
        submissionDao.observeByArchive(projectId).first().map { it.toDomain() }
    }

    override fun observeCollections(projectId: Long): Flow<List<Submission>> = submissionDao.observeByArchive(projectId)
        .map { entities -> entities.map { it.toDomain() } }
        .distinctUntilChanged()

    override suspend fun getCollection(id: Long): Submission? = withContext(io) {
        submissionDao.getById(id)?.toDomain()
    }

    override suspend fun updateCollection(submission: Submission) {
        withContext(io) {
            submissionDao.upsert(submission.toSubmissionEntity())
        }
    }

    override suspend fun deleteCollection(id: Long) {
        withContext(io) {
            submissionDao.getById(id)?.let {
                submissionDao.delete(it)
            }
        }
    }
}
