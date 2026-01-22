package net.opendasharchive.openarchive.core.repositories

import kotlinx.coroutines.flow.Flow
import net.opendasharchive.openarchive.core.domain.Archive
import net.opendasharchive.openarchive.core.domain.Submission

interface ProjectRepository {
    suspend fun getProjects(vaultId: Long): List<Archive>
    fun observeProjects(vaultId: Long): Flow<List<Archive>>
    suspend fun getProject(id: Long): Archive?
    suspend fun renameProject(id: Long, newName: String)
    suspend fun archiveProject(id: Long, isArchived: Boolean): Boolean
    suspend fun deleteProject(id: Long): Boolean
    suspend fun getActiveSubmission(projectId: Long): Submission
    suspend fun getProjectByName(vaultId: Long, name: String): Archive?
    suspend fun addProject(archive: Archive): Long
}