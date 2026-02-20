package net.opendasharchive.openarchive.core.repositories

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.core.domain.Archive
import net.opendasharchive.openarchive.core.domain.Submission
import net.opendasharchive.openarchive.core.domain.mappers.toArchiveEntity
import net.opendasharchive.openarchive.core.domain.mappers.toDomain
import net.opendasharchive.openarchive.db.ArchiveDao
import net.opendasharchive.openarchive.db.SubmissionDao
import net.opendasharchive.openarchive.db.SubmissionEntity
import net.opendasharchive.openarchive.db.EvidenceDao
import net.opendasharchive.openarchive.db.VaultDao

class ArchiveRepositoryImpl(
    private val fileCleanupHelper: FileCleanupHelper,
    private val archiveDao: ArchiveDao,
    private val submissionDao: SubmissionDao,
    private val vaultDao: VaultDao,
    private val evidenceDao: EvidenceDao,
    private val io: CoroutineDispatcher = Dispatchers.IO
) : ProjectRepository {

    override suspend fun getProjects(vaultId: Long, archived: Boolean): List<Archive> = withContext(io) {
        archiveDao.observeByVault(vaultId, archived).first().map { it.toDomain() }
    }

    override fun observeProjects(vaultId: Long, archived: Boolean): Flow<List<Archive>> = archiveDao.observeByVault(vaultId, archived)
        .map { entities -> entities.map { it.toDomain() } }
        .distinctUntilChanged()

    override suspend fun getProject(id: Long): Archive? = withContext(io) {
        archiveDao.getById(id)?.toDomain()
    }

    override fun observeProject(id: Long): Flow<Archive?> = archiveDao.observeById(id)
        .map { it?.toDomain() }
        .distinctUntilChanged()

    override suspend fun renameProject(id: Long, newName: String) {
        withContext(io) {
            archiveDao.getById(id)?.let {
                archiveDao.upsert(it.copy(description = newName, isRemote = false))
            }
        }
    }

    override suspend fun archiveProject(id: Long, isArchived: Boolean): Boolean = withContext(io) {
        archiveDao.getById(id)?.let { archive ->
            var updatedArchive = archive.copy(archived = isArchived)

            // Port legacy behavior: apply space license if unarchiving and license is null
            if (!isArchived) {
                val space = vaultDao.getById(archive.vaultId)
                if (updatedArchive.licenseUrl.isNullOrBlank()) {
                    updatedArchive = updatedArchive.copy(licenseUrl = space?.licenseUrl)
                }
            }

            archiveDao.upsert(updatedArchive) > 0
        } ?: false
    }

    override suspend fun deleteProject(id: Long): Boolean = withContext(io) {
        archiveDao.getById(id)?.let { archive ->
            // 1. Fetch evidence association before DB deletion
            val evidenceEntities = evidenceDao.getByArchive(id)
            val evidenceList = evidenceEntities.map { it.toDomain(vaultId = archive.vaultId) }

            // 2. Perform DB deletion first
            archiveDao.delete(archive)
            
            // 3. Clean up physical files after successful DB removal
            evidenceList.forEach { evidence ->
                fileCleanupHelper.deleteMediaFiles(evidence)
            }
            true
        } ?: false
    }

    override suspend fun getActiveSubmission(projectId: Long): Submission = withContext(io) {
        val archive = archiveDao.getById(projectId) ?: throw IllegalStateException("Project not found")
        var submission = submissionDao.getById(archive.openSubmissionId)

        if (submission == null || submission.uploadedAt != null) {
            // Create new submission
            val newId =
                submissionDao.upsert(SubmissionEntity(archiveId = projectId, uploadedAt = null, serverUrl = null))
            archiveDao.upsert(archive.copy(openSubmissionId = newId))
            submission = submissionDao.getById(newId)
        }

        submission!!.toDomain()
    }

    override suspend fun getProjectByName(vaultId: Long, name: String): Archive? = withContext(io) {
        archiveDao.getByName(vaultId, name)?.toDomain()
    }

    override suspend fun addProject(archive: Archive): Long = withContext(io) {
        archiveDao.upsert(archive.toArchiveEntity())
    }
}
