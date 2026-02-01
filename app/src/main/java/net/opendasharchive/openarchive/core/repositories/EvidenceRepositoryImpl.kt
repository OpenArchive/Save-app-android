package net.opendasharchive.openarchive.core.repositories

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.core.domain.Evidence
import net.opendasharchive.openarchive.core.domain.EvidenceStatus
import net.opendasharchive.openarchive.core.domain.mappers.toDomain
import net.opendasharchive.openarchive.core.domain.mappers.toEvidenceEntity
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.db.ArchiveDao
import net.opendasharchive.openarchive.db.EvidenceDao
import net.opendasharchive.openarchive.db.SubmissionDao

class EvidenceRepositoryImpl(
    private val evidenceDao: EvidenceDao,
    private val submissionDao: SubmissionDao,
    private val archiveDao: ArchiveDao,
    private val io: CoroutineDispatcher = Dispatchers.IO
) : MediaRepository {

    override suspend fun getMediaForCollection(collectionId: Long): List<Evidence> = withContext(io) {
        evidenceDao.observeByCollection(collectionId).first().map { mapToDomain(it) }
    }

    override fun observeMediaForCollection(collectionId: Long): Flow<List<Evidence>> =
        evidenceDao.observeByCollection(collectionId)
            .map { entities -> entities.map { it.toDomain() } }
            .distinctUntilChanged()

    override suspend fun getMediaForProject(projectId: Long): List<Evidence> = withContext(io) {
        evidenceDao.observeByProject(projectId).first().map { mapToDomain(it) }
    }

    override fun observeMediaForProject(projectId: Long): Flow<List<Evidence>> =
        evidenceDao.observeByProject(projectId)
            .map { entities -> entities.map { it.toDomain() } }
            .distinctUntilChanged()

    override suspend fun getLocalMedia(): List<Evidence> = withContext(io) {
        evidenceDao.getByStatus(listOf(1)).map { mapToDomain(it) } // 1 is LOCAL
    }

    override suspend fun getEvidence(id: Long): Evidence? = withContext(io) {
        evidenceDao.getById(id)?.let { mapToDomain(it) }
    }

    private suspend fun mapToDomain(entity: net.opendasharchive.openarchive.db.EvidenceEntity): Evidence {
        val project = archiveDao.getById(entity.projectId)
        return entity.toDomain(vaultId = project?.spaceId ?: 0L)
    }

    override suspend fun setSelected(mediaId: Long, selected: Boolean) {
        // Selection is not persisted in Room
    }

    override suspend fun deleteMedia(mediaId: Long) {
        withContext(io) {
            evidenceDao.getById(mediaId)?.let { media ->
                val collectionId = media.collectionId
                val count = evidenceDao.getCountByCollection(collectionId)

                if (count < 2) {
                    submissionDao.getById(collectionId)?.let {
                        submissionDao.delete(it) // CASCADE will delete the media too
                    }
                } else {
                    evidenceDao.deleteById(mediaId)
                }
            }
        }
    }

    override suspend fun addEvidence(evidence: Evidence): Long = withContext(io) {
        evidenceDao.upsert(evidence.toEvidenceEntity())
    }

    override suspend fun updateEvidence(evidence: Evidence) {
        withContext(io) {
            val entity = evidence.toEvidenceEntity()
            if (evidenceDao.getById(entity.id) != null) {
                evidenceDao.upsert(entity)
            } else {
                AppLogger.w("Skipping update for media ${entity.id} as it was deleted from database")
            }
        }
    }

    override suspend fun queueAllForUpload(mediaIds: List<Long>) {
        withContext(io) {
            mediaIds.forEach { id ->
                evidenceDao.getById(id)?.let {
                    evidenceDao.upsert(it.copy(status = 2)) // 2 is QUEUED
                }
            }
        }
    }

    override suspend fun getQueue(): List<Evidence> = withContext(io) {
        val statuses = listOf(4, 2, 9) // UPLOADING, QUEUED, ERROR
        // Sorting is handled by evidenceDao.getByStatus (priority DESC, id DESC)
        evidenceDao.getByStatus(statuses).map { mapToDomain(it) }
    }

    override suspend fun updatePriority(mediaId: Long, priority: Int) {
        withContext(io) {
            evidenceDao.getById(mediaId)?.let {
                evidenceDao.upsert(it.copy(priority = priority))
            }
        }
    }

    override suspend fun retryMedia(mediaId: Long) {
        withContext(io) {
            evidenceDao.getById(mediaId)?.let {
                evidenceDao.upsert(it.copy(status = 2, progress = 0, statusMessage = ""))
            }
        }
    }
}
