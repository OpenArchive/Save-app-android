package net.opendasharchive.openarchive.services.snowbird

import net.opendasharchive.openarchive.services.snowbird.service.db.RefreshGroupResponse
import net.opendasharchive.openarchive.services.snowbird.service.db.RequestName
import net.opendasharchive.openarchive.services.snowbird.service.db.SnowbirdGroup
import net.opendasharchive.openarchive.services.snowbird.service.db.SnowbirdRepo
import net.opendasharchive.openarchive.services.snowbird.service.db.toRepo
import net.opendasharchive.openarchive.extensions.toSnowbirdError
import net.opendasharchive.openarchive.services.snowbird.service.ISnowbirdAPI
import timber.log.Timber

interface ISnowbirdRepoRepository {
    suspend fun createRepo(groupKey: String, repoName: String): SnowbirdResult<SnowbirdRepo>
    suspend fun fetchRepos(groupKey: String, forceRefresh: Boolean = false): SnowbirdResult<List<SnowbirdRepo>>
    suspend fun refreshGroupContent(groupKey: String): SnowbirdResult<RefreshGroupResponse>
}

class SnowbirdRepoRepository(val api: ISnowbirdAPI) : ISnowbirdRepoRepository {

    override suspend fun createRepo(groupKey: String, repoName: String): SnowbirdResult<SnowbirdRepo> {
        Timber.d("Creating repo: groupKey=$groupKey, repoName=$repoName")

        return try {
            val response = api.createRepo(groupKey, RequestName(repoName))
            val repo = response.toRepo(groupKey)
            SnowbirdResult.Success(repo)
        } catch (e: Exception) {
            SnowbirdResult.Error(e.toSnowbirdError())
        }
    }

    override suspend fun fetchRepos(groupKey: String, forceRefresh: Boolean): SnowbirdResult<List<SnowbirdRepo>> {
        return if (forceRefresh) {
            fetchFromNetwork(groupKey)
        } else {
            fetchFromCache(groupKey)
        }
    }

    private suspend fun fetchFromNetwork(groupKey: String): SnowbirdResult<List<SnowbirdRepo>> {
        return try {
            val response = api.fetchRepos(groupKey)
            val repoList = response.repos.map { it.toRepo(groupKey) }
            SnowbirdResult.Success(repoList)
        } catch (e: Exception) {
            SnowbirdResult.Error(e.toSnowbirdError())
        }
    }

    private fun fetchFromCache(groupKey: String): SnowbirdResult<List<SnowbirdRepo>> {
        return SnowbirdResult.Success(SnowbirdRepo.getAllFor(SnowbirdGroup.get(groupKey)))
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


