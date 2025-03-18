package net.opendasharchive.openarchive.services.snowbird

import net.opendasharchive.openarchive.db.JoinGroupResponse
import net.opendasharchive.openarchive.db.MembershipRequest
import net.opendasharchive.openarchive.db.RequestName
import net.opendasharchive.openarchive.db.SnowbirdGroup
import net.opendasharchive.openarchive.extensions.toSnowbirdError
import net.opendasharchive.openarchive.services.snowbird.service.ISnowbirdAPI

interface ISnowbirdGroupRepository {
    suspend fun createGroup(groupName: String): SnowbirdResult<SnowbirdGroup>
    suspend fun fetchGroup(groupKey: String): SnowbirdResult<SnowbirdGroup>
    suspend fun fetchGroups(forceRefresh: Boolean = false): SnowbirdResult<List<SnowbirdGroup>>
    suspend fun joinGroup(uriString: String): SnowbirdResult<JoinGroupResponse>
}

class SnowbirdGroupRepository(val api: ISnowbirdAPI) : ISnowbirdGroupRepository {
    private var lastFetchTime: Long = 0
    private val cacheValidityPeriod: Long = 5 * 60 * 1000

    override suspend fun createGroup(groupName: String): SnowbirdResult<SnowbirdGroup> {
        return try {
            val response = api.createGroup(
                RequestName(groupName))
            SnowbirdResult.Success(response)
        } catch (e: Exception) {
            SnowbirdResult.Error(e.toSnowbirdError())
        }
    }

    override suspend fun fetchGroup(groupKey: String): SnowbirdResult<SnowbirdGroup> {
        return try {
            val response = api.fetchGroup(groupKey)
            SnowbirdResult.Success(response)
        } catch (e: Exception) {
            SnowbirdResult.Error(e.toSnowbirdError())
        }
    }

    override suspend fun fetchGroups(forceRefresh: Boolean): SnowbirdResult<List<SnowbirdGroup>> {
        val currentTime = System.currentTimeMillis()
        val shouldFetchFromNetwork = forceRefresh || currentTime - lastFetchTime > cacheValidityPeriod

        return if (forceRefresh) {
            fetchFromNetwork()
        } else {
            fetchFromCache()
        }
    }

    override suspend fun joinGroup(uriString: String): SnowbirdResult<JoinGroupResponse> {
        return try {
            val response = api.joinGroup(
                MembershipRequest(uriString))
            SnowbirdResult.Success(response)
        } catch (e: Exception) {
            e.printStackTrace()
            SnowbirdResult.Error(e.toSnowbirdError())
        }
    }

    private suspend fun fetchFromNetwork(): SnowbirdResult<List<SnowbirdGroup>> {
        return try {
            val response = api.fetchGroups()
            SnowbirdResult.Success(response.groups)
        } catch (e: Exception) {
            SnowbirdResult.Error(e.toSnowbirdError())
        }
    }

    private fun fetchFromCache(): SnowbirdResult<List<SnowbirdGroup>> {
        return SnowbirdResult.Success(SnowbirdGroup.getAll())
    }
}