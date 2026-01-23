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

class SugarProjectRepository(private val io: CoroutineDispatcher = Dispatchers.IO) : ProjectRepository {

    override suspend fun getProjects(vaultId: Long): List<Archive> = withContext(io) {
        Space.get(vaultId)?.projects?.map { it.toDomain() } ?: emptyList()
    }

    override fun observeProjects(vaultId: Long): Flow<List<Archive>> = InvalidationBus.projects
        .map { getProjects(vaultId) }
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
        val deleted = Project.getById(id)?.delete() ?: false
        if (deleted) InvalidationBus.invalidateProjects()
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