package net.opendasharchive.openarchive.core.repositories

import com.orm.SugarRecord
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.core.domain.Archive
import net.opendasharchive.openarchive.core.domain.Submission
import net.opendasharchive.openarchive.core.domain.mappers.toDomain
import net.opendasharchive.openarchive.core.domain.mappers.toEntity
import net.opendasharchive.openarchive.db.sugar.Project
import net.opendasharchive.openarchive.db.sugar.Space

import net.opendasharchive.openarchive.db.sugar.Media

class SugarProjectRepository(
    private val fileCleanupHelper: FileCleanupHelper,
    private val io: CoroutineDispatcher = Dispatchers.IO
) : ProjectRepository {

    override suspend fun getProjects(vaultId: Long, archived: Boolean): List<Archive> = withContext(io) {
        val projects = Space.get(vaultId)?.projects ?: emptyList()
        projects.filter { it.isArchived == archived }.map { it.toDomain() }
    }

    override fun observeProjects(vaultId: Long, archived: Boolean): Flow<List<Archive>> = InvalidationBus.projects
        .map { getProjects(vaultId, archived) }
        .distinctUntilChanged()

    override suspend fun getProject(id: Long): Archive? = withContext(io) {
        Project.getById(id)?.toDomain()
    }

    override fun observeProject(id: Long): Flow<Archive?> {
        return InvalidationBus.projects
            .map { getProject(id) }
            .distinctUntilChanged()
    }

    override suspend fun renameProject(id: Long, newName: String) {
        withContext(io) {
            Project.getById(id)?.let {
                it.description = newName
                it.save()
                InvalidationBus.invalidateProjects()
            }
        }
    }

    override suspend fun archiveProject(id: Long, isArchived: Boolean): Boolean = withContext(io) {
        Project.getById(id)?.let {
            it.isArchived = isArchived
            val saved = it.save() > 0
            if (saved) InvalidationBus.invalidateProjects()
            saved
        } ?: false
    }

    override suspend fun deleteProject(id: Long): Boolean = withContext(io) {
        val project = Project.getById(id) ?: return@withContext false
        
        // 1. Fetch media association before DB deletion
        val mediaList = SugarRecord.find(
            Media::class.java, "project_id = ?",
            id.toString()
        )
        
        // 2. Perform DB deletion first
        val deleted = project.delete()
        if (deleted) {
            InvalidationBus.invalidateProjects()
            
            // 3. Clean up physical files after successful DB removal
            mediaList.forEach { media ->
                fileCleanupHelper.deleteMediaFiles(media)
            }
        }
        deleted
    }

    override suspend fun getActiveSubmission(projectId: Long): Submission =
        withContext(io) {
            Project.getById(projectId)!!.openCollection.toDomain()
        }

    override suspend fun getProjectByName(vaultId: Long, name: String): Archive? =
        withContext(io) {
            SugarRecord.find(
                Project::class.java,
                "space_id = ? AND description = ?",
                vaultId.toString(),
                name
            ).firstOrNull()?.toDomain()
        }

    override suspend fun addProject(archive: Archive): Long = withContext(io) {
        val id = archive.toEntity().save()
        if (id > 0) InvalidationBus.invalidateProjects()
        id
    }
}