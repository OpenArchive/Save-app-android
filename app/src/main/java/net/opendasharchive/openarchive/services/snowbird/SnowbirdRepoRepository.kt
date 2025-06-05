package net.opendasharchive.openarchive.services.snowbird

import net.opendasharchive.openarchive.db.RequestName
import net.opendasharchive.openarchive.db.SnowbirdGroup
import net.opendasharchive.openarchive.db.SnowbirdRepo
import net.opendasharchive.openarchive.extensions.toSnowbirdError
import net.opendasharchive.openarchive.services.snowbird.service.ISnowbirdAPI
import timber.log.Timber

interface ISnowbirdRepoRepository {
    suspend fun createRepo(groupKey: String, repoName: String): SnowbirdResult<SnowbirdRepo>
    suspend fun fetchRepos(groupKey: String, forceRefresh: Boolean = false): SnowbirdResult<List<SnowbirdRepo>>
}

class SnowbirdRepoRepository(val api: ISnowbirdAPI) : ISnowbirdRepoRepository {
    override suspend fun createRepo(groupKey: String, repoName: String): SnowbirdResult<SnowbirdRepo> {
        Timber.d("Creating repo: groupKey=$groupKey, repoName=$repoName")

        return try {
            val response = api.createRepo(groupKey, RequestName(repoName))
            SnowbirdResult.Success(response)
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
            SnowbirdResult.Success(response.repos)
        } catch (e: Exception) {
            SnowbirdResult.Error(e.toSnowbirdError())
        }
    }

    private fun fetchFromCache(groupKey: String): SnowbirdResult<List<SnowbirdRepo>> {
        return SnowbirdResult.Success(SnowbirdRepo.getAllFor(SnowbirdGroup.get(groupKey)))
    }
}