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
import net.opendasharchive.openarchive.db.VaultDao

class ArchiveRepositoryImpl(
    private val archiveDao: ArchiveDao,
    private val submissionDao: SubmissionDao,
    private val vaultDao: VaultDao,
    private val io: CoroutineDispatcher = Dispatchers.IO
) : ProjectRepository {

    override suspend fun getProjects(vaultId: Long, archived: Boolean): List<Archive> = withContext(io) {
        archiveDao.observeBySpace(vaultId, archived).first().map { it.toDomain() }
    }

    override fun observeProjects(vaultId: Long, archived: Boolean): Flow<List<Archive>> = archiveDao.observeBySpace(vaultId, archived)
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
                archiveDao.upsert(it.copy(description = newName))
            }
        }
    }

    override suspend fun archiveProject(id: Long, isArchived: Boolean): Boolean = withContext(io) {
        archiveDao.getById(id)?.let { archive ->
            var updatedArchive = archive.copy(archived = isArchived)

            // Port legacy behavior: apply space license if unarchiving and license is null
            if (!isArchived) {
                val space = vaultDao.getById(archive.spaceId)
                if (updatedArchive.licenseUrl.isNullOrBlank()) {
                    updatedArchive = updatedArchive.copy(licenseUrl = space?.licenseUrl)
                }
            }

            archiveDao.upsert(updatedArchive) > 0
        } ?: false
    }

    override suspend fun deleteProject(id: Long): Boolean = withContext(io) {
        archiveDao.getById(id)?.let {
            archiveDao.delete(it)
            true
        } ?: false
    }

    override suspend fun getActiveSubmission(projectId: Long): Submission = withContext(io) {
        val archive = archiveDao.getById(projectId) ?: throw IllegalStateException("Project not found")
        var submission = submissionDao.getById(archive.openCollectionId)

        if (submission == null || submission.uploadDate != null) {
            // Create new collection
            val newId =
                submissionDao.upsert(SubmissionEntity(projectId = projectId, uploadDate = null, serverUrl = null))
            archiveDao.upsert(archive.copy(openCollectionId = newId))
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
