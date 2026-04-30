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
    private val fileCleanupHelper: FileCleanupHelper,
    private val evidenceDao: EvidenceDao,
    private val submissionDao: SubmissionDao,
    private val archiveDao: ArchiveDao,
    private val io: CoroutineDispatcher = Dispatchers.IO
) : MediaRepository {

    override suspend fun getMediaForCollection(collectionId: Long): List<Evidence> = withContext(io) {
        // Submission flow doesn't typically join DWeb metadata directly but could if needed
        evidenceDao.observeBySubmission(collectionId).first().map { mapToDomain(it) }
    }

    override fun observeMediaForCollection(collectionId: Long): Flow<List<Evidence>> =
        evidenceDao.observeBySubmission(collectionId)
            .map { entities -> entities.map { it.toDomain() } }
            .distinctUntilChanged()

    override suspend fun getMediaForProject(projectId: Long): List<Evidence> = withContext(io) {
        evidenceDao.observeByArchive(projectId).first().map { mapToDomain(it) }
    }

    override fun observeMediaForProject(projectId: Long): Flow<List<Evidence>> =
        evidenceDao.observeByArchive(projectId)
            .map { entities -> entities.map { it.toDomain() } }
            .distinctUntilChanged()

    override suspend fun getLocalMedia(): List<Evidence> = withContext(io) {
        evidenceDao.getByStatus(listOf(EvidenceStatus.LOCAL)).map { mapToDomain(it) }
    }

    override fun observeLocalMedia(): Flow<List<Evidence>> =
        evidenceDao.observeByStatus(listOf(EvidenceStatus.LOCAL))
            .map { entities ->
                entities.map { mapToDomain(it) }
            }
            .distinctUntilChanged()

    override suspend fun getEvidence(id: Long): Evidence? = withContext(io) {
        evidenceDao.getById(id)?.let { mapToDomain(it) }
    }

    private suspend fun mapToDomain(entity: net.opendasharchive.openarchive.db.EvidenceEntity): Evidence {
        val project = archiveDao.getById(entity.archiveId)
        return entity.toDomain(vaultId = project?.vaultId ?: 0L)
    }

    override suspend fun setSelected(mediaId: Long, selected: Boolean) {
        // Selection is not persisted in Room
    }

    override suspend fun deleteMedia(mediaId: Long) {
        withContext(io) {
            evidenceDao.getById(mediaId)?.let { entity ->
                val evidence = mapToDomain(entity)
                val submissionId = entity.submissionId
                val count = evidenceDao.getCountBySubmission(submissionId)

                // Perform DB deletion first
                if (count < 2) {
                    submissionDao.getById(submissionId)?.let {
                        submissionDao.delete(it) // CASCADE will delete the media too
                    }
                } else {
                    evidenceDao.deleteById(mediaId)
                }

                InvalidationBus.invalidateMedia()
                InvalidationBus.invalidateCollections()

                // Clean up physical files after successful DB removal
                fileCleanupHelper.deleteMediaFiles(evidence)
            }
        }
    }

    override suspend fun addEvidence(evidence: Evidence): Long = withContext(io) {
        val id = evidenceDao.upsert(evidence.toEvidenceEntity())
        if (id > 0) {
            InvalidationBus.invalidateMedia()
            InvalidationBus.invalidateCollections()
        }
        id
    }

    override suspend fun updateEvidence(evidence: Evidence) {
        withContext(io) {
            val entity = evidence.toEvidenceEntity()
            if (evidenceDao.getById(entity.id) != null) {
                evidenceDao.upsert(entity)
                InvalidationBus.invalidateMedia()
            } else {
                AppLogger.w("Skipping update for media ${entity.id} as it was deleted from database")
            }
        }
    }

    override suspend fun queueAllForUpload(mediaIds: List<Long>) {
        withContext(io) {
            mediaIds.forEach { id ->
                evidenceDao.getById(id)?.let {
                    evidenceDao.upsert(it.copy(status = EvidenceStatus.QUEUED))
                }
            }
            InvalidationBus.invalidateMedia()
        }
    }

    override suspend fun getQueue(): List<Evidence> = withContext(io) {
        val statuses = listOf(EvidenceStatus.UPLOADING, EvidenceStatus.QUEUED, EvidenceStatus.ERROR)
        // Sorting is handled by evidenceDao.getByStatus (priority DESC, id DESC)
        evidenceDao.getByStatus(statuses).map { mapToDomain(it) }
    }

    override suspend fun updatePriority(mediaId: Long, priority: Int) {
        withContext(io) {
            evidenceDao.getById(mediaId)?.let {
                evidenceDao.upsert(it.copy(priority = priority))
                InvalidationBus.invalidateMedia()
            }
        }
    }

    override suspend fun updatePriorities(priorities: List<Pair<Long, Int>>) {
        withContext(io) {
            priorities.forEach { (mediaId, priority) ->
                evidenceDao.getById(mediaId)?.let {
                    evidenceDao.upsert(it.copy(priority = priority))
                }
            }
            InvalidationBus.invalidateMedia()
        }
    }

    override suspend fun retryMedia(mediaId: Long) {
        withContext(io) {
            evidenceDao.getById(mediaId)?.let {
                evidenceDao.upsert(it.copy(status = EvidenceStatus.QUEUED, progress = 0, statusMessage = ""))
                InvalidationBus.invalidateMedia()
            }
        }
    }
}
