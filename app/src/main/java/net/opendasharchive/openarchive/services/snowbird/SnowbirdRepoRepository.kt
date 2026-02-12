package net.opendasharchive.openarchive.services.snowbird

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.opendasharchive.openarchive.core.domain.Archive
import net.opendasharchive.openarchive.db.ArchiveDao
import net.opendasharchive.openarchive.db.DwebDao
import net.opendasharchive.openarchive.db.VaultDao
import net.opendasharchive.openarchive.extensions.toSnowbirdError
import net.opendasharchive.openarchive.services.snowbird.data.*
import net.opendasharchive.openarchive.services.snowbird.service.ISnowbirdAPI

interface ISnowbirdRepoRepository {
    suspend fun createRepo(vaultId: Long, groupKey: String, repoName: String): SnowbirdResult<Archive>
    suspend fun fetchRepos(vaultId: Long, groupKey: String, forceRefresh: Boolean = false): SnowbirdResult<Unit>
    fun observeRepos(vaultId: Long, archived: Boolean = false): Flow<List<Archive>>
    suspend fun refreshGroupContent(groupKey: String): SnowbirdResult<RefreshGroupResponse>
}

class SnowbirdRepoRepository(
    private val api: ISnowbirdAPI,
    private val archiveDao: ArchiveDao,
    private val dwebDao: DwebDao
) : ISnowbirdRepoRepository {

    override fun observeRepos(vaultId: Long, archived: Boolean): Flow<List<Archive>> {
        return dwebDao.observeArchivesWithDweb(vaultId, archived).map { archiveList ->
            archiveList.map { it.toDomain() }
        }
    }

    override suspend fun createRepo(vaultId: Long, groupKey: String, repoName: String): SnowbirdResult<Archive> {
        return try {
            val response = api.createRepo(groupKey, RequestName(repoName))
            // The API response for createRepo is tricky, it returns CreateRepoResponse which needs mapping to a repo
            // In the original code, it was: val repo = response.toRepo(groupKey)
            // Let's assume we can map it to our entities.
            // Wait, I should check CreateRepoResponse and toRepo extension.

            // For now, let's just fetch all repos after creation or handle it if we have the data.
            fetchRepos(vaultId, groupKey, forceRefresh = true)

            val savedArchive = archiveDao.getByName(vaultId, repoName)
            if (savedArchive != null) {
                val archivedWithDweb = dwebDao.getArchiveWithDwebById(savedArchive.id)
                if (archivedWithDweb != null) {
                    SnowbirdResult.Success(archivedWithDweb.toDomain())
                } else {
                    SnowbirdResult.Error(SnowbirdError.GeneralError("Failed to retrieve metadata"))
                }
            } else {
                 SnowbirdResult.Error(SnowbirdError.GeneralError("Failed to retrieve saved repo"))
            }
        } catch (e: Exception) {
            SnowbirdResult.Error(e.toSnowbirdError())
        }
    }

    override suspend fun fetchRepos(vaultId: Long, groupKey: String, forceRefresh: Boolean): SnowbirdResult<Unit> {
        return try {
            val response = api.fetchRepos(groupKey)
            response.repos.forEach { repoDto ->
                val archiveEntity = repoDto.toArchiveEntity(vaultId)
                val archiveId = archiveDao.upsert(archiveEntity)
                val dwebEntity = repoDto.toDwebEntity(archiveId)
                dwebDao.upsertArchiveMetadata(dwebEntity)
            }
            SnowbirdResult.Success(Unit)
        } catch (e: Exception) {
            SnowbirdResult.Error(e.toSnowbirdError())
        }
    }

    override suspend fun refreshGroupContent(groupKey: String): SnowbirdResult<RefreshGroupResponse> {
        return try {
            val response = api.refreshGroupContent(groupKey)
            SnowbirdResult.Success(response)
        } catch (e: Exception) {
            SnowbirdResult.Error(e.toSnowbirdError())
        }
    }
}


