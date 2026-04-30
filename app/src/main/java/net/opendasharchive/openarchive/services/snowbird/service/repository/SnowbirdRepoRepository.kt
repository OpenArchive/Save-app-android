package net.opendasharchive.openarchive.services.snowbird.service.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.opendasharchive.openarchive.core.domain.Archive
import net.opendasharchive.openarchive.core.domain.DomainError
import net.opendasharchive.openarchive.db.ArchiveDao
import net.opendasharchive.openarchive.core.domain.DomainResult
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.db.DwebDao
import net.opendasharchive.openarchive.db.SubmissionDao
import net.opendasharchive.openarchive.db.SubmissionEntity
import net.opendasharchive.openarchive.extensions.toDomainError
import net.opendasharchive.openarchive.services.snowbird.data.*
import net.opendasharchive.openarchive.services.snowbird.service.ISnowbirdAPI

interface ISnowbirdRepoRepository {
    suspend fun createRepo(vaultId: Long, groupKey: String, repoName: String): DomainResult<Archive>
    suspend fun fetchRepos(vaultId: Long, groupKey: String, forceRefresh: Boolean = false): DomainResult<Unit>
    fun observeRepos(vaultId: Long, archived: Boolean = false): Flow<List<Archive>>
    suspend fun refreshGroupContent(groupKey: String): DomainResult<RefreshGroupResponse>
}

class SnowbirdRepoRepository(
    private val api: ISnowbirdAPI,
    private val archiveDao: ArchiveDao,
    private val submissionDao: SubmissionDao,
    private val dwebDao: DwebDao
) : ISnowbirdRepoRepository {

    override fun observeRepos(vaultId: Long, archived: Boolean): Flow<List<Archive>> {
        return dwebDao.observeArchivesWithDweb(vaultId, archived).map { archiveList ->
            archiveList.map { it.toDomain() }
        }
    }

    override suspend fun createRepo(vaultId: Long, groupKey: String, repoName: String): DomainResult<Archive> {
        return try {
            val response = api.createRepo(groupKey, RequestName(repoName))

            // Deduplication lookup STRICTLY by DWeb key
            val existingArchiveId = dwebDao.getArchiveIdByKey(response.key)

            // Map SnowbirdRepo to Archive entity using the fixed ID (if exists) or 0 (for new)
            val archiveEntity = response.toArchiveEntity(
                vaultId = vaultId,
                submissionId = 0L,
                id = existingArchiveId ?: 0L
            )
            val upsertedId = archiveDao.upsert(archiveEntity)
            val archiveId = existingArchiveId ?: upsertedId


            // For now, let's just fetch all repos after creation or handle it if we have the data.
            fetchRepos(vaultId, groupKey, forceRefresh = true)

            val savedArchive = archiveDao.getByName(vaultId, repoName)
            if (savedArchive != null) {
                val archivedWithDweb = dwebDao.getArchiveWithDwebById(savedArchive.id)
                if (archivedWithDweb != null) {
                    DomainResult.Success(archivedWithDweb.toDomain())
                } else {
                    DomainResult.Error(DomainError.Unknown("Failed to retrieve metadata"))
                }
            } else {
                 DomainResult.Error(DomainError.Unknown("Failed to retrieve saved repo"))
            }
        } catch (e: Exception) {
            AppLogger.e(e)
            DomainResult.Error(e.toDomainError())
        }
    }

    override suspend fun fetchRepos(vaultId: Long, groupKey: String, forceRefresh: Boolean): DomainResult<Unit> {
        return try {
            val response = api.fetchRepos(groupKey)
            response.repos.forEach { repoDto ->
                // Deduplication lookup STRICTLY by DWeb key
                val existingArchiveId = dwebDao.getArchiveIdByKey(repoDto.key)
                
                // If not found by key, fallback to finding by name (legacy or first-time sync)
                val fallbackId = archiveDao.getByName(vaultId, repoDto.name ?: "")?.id
                val currentId = existingArchiveId ?: fallbackId
                
                // 1. Upsert Archive FIRST with placeholder submissionId to get its internal ID
                val upsertedId = archiveDao.upsert(repoDto.toArchiveEntity(vaultId, 0, id = currentId ?: 0L))
                val archiveId = currentId ?: upsertedId
                
                // 2. Ensure there's an open submission for this archive
                val archive = archiveDao.getById(archiveId)
                var submissionId = archive?.openSubmissionId ?: 0L
                if (submissionId == 0L) {
                    submissionId = submissionDao.upsert(
                        SubmissionEntity(
                            archiveId = archiveId,
                            uploadedAt = null,
                            serverUrl = null
                        )
                    )
                }
                
                // 3. Now update with the correct submissionId and ID
                archiveDao.upsert(repoDto.toArchiveEntity(vaultId, submissionId, id = archiveId))
                
                val dwebEntity = repoDto.toDwebEntity(archiveId)
                dwebDao.upsertArchiveMetadata(dwebEntity)
            }
            DomainResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e(e)
            DomainResult.Error(e.toDomainError())
        }
    }

    override suspend fun refreshGroupContent(groupKey: String): DomainResult<RefreshGroupResponse> {
        return try {
            val response = api.refreshGroupContent(groupKey)
            DomainResult.Success(response)
        } catch (e: Exception) {
            AppLogger.e(e)
            DomainResult.Error(e.toDomainError())
        }
    }
}
