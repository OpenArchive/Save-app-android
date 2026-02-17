package net.opendasharchive.openarchive.services.snowbird

import net.opendasharchive.openarchive.db.JoinGroupResponse
import net.opendasharchive.openarchive.db.MembershipRequest
import net.opendasharchive.openarchive.db.RequestName
import net.opendasharchive.openarchive.db.SnowbirdGroup
import net.opendasharchive.openarchive.db.SnowbirdError
import net.opendasharchive.openarchive.extensions.toSnowbirdError
import net.opendasharchive.openarchive.services.snowbird.service.ISnowbirdAPI
import net.opendasharchive.openarchive.services.snowbird.service.ServiceStatus
import net.opendasharchive.openarchive.services.snowbird.service.SnowbirdService

interface ISnowbirdGroupRepository {
    suspend fun createGroup(groupName: String): SnowbirdResult<SnowbirdGroup>
    suspend fun fetchGroup(groupKey: String): SnowbirdResult<SnowbirdGroup>
    suspend fun fetchGroups(forceRefresh: Boolean = false): SnowbirdResult<List<SnowbirdGroup>>
    suspend fun joinGroup(uriString: String): SnowbirdResult<JoinGroupResponse>
}

class SnowbirdGroupRepository(val api: ISnowbirdAPI) : ISnowbirdGroupRepository {
    private var lastFetchTime: Long = 0
    private val cacheValidityPeriod: Long = 5 * 60 * 1000

    private fun ensureServerReadyForNetwork(): SnowbirdResult.Error? {
        return if (SnowbirdService.getCurrentStatus() is ServiceStatus.Connected) {
            null
        } else {
            SnowbirdResult.Error(SnowbirdError.GeneralError("DWeb server is not ready. Enable DWeb Server and wait for it to connect."))
        }
    }

    override suspend fun createGroup(groupName: String): SnowbirdResult<SnowbirdGroup> {
        return try {
            ensureServerReadyForNetwork()?.let { return it }
            val response = api.createGroup(
                RequestName(groupName)
            )
            SnowbirdResult.Success(response)
        } catch (e: Exception) {
            SnowbirdResult.Error(e.toSnowbirdError())
        }
    }

    override suspend fun fetchGroup(groupKey: String): SnowbirdResult<SnowbirdGroup> {
        return try {
            ensureServerReadyForNetwork()?.let { return it }
            val response = api.fetchGroup(groupKey)
            SnowbirdResult.Success(response)
        } catch (e: Exception) {
            SnowbirdResult.Error(e.toSnowbirdError())
        }
    }

    override suspend fun fetchGroups(forceRefresh: Boolean): SnowbirdResult<List<SnowbirdGroup>> {
        val currentTime = System.currentTimeMillis()
        val shouldFetchFromNetwork = forceRefresh || currentTime - lastFetchTime > cacheValidityPeriod

        return if (shouldFetchFromNetwork) {
            fetchFromNetwork()
        } else {
            fetchFromCache()
        }
    }

    override suspend fun joinGroup(uriString: String): SnowbirdResult<JoinGroupResponse> {
        return try {
            ensureServerReadyForNetwork()?.let { return it }
            val response = api.joinGroup(
                MembershipRequest(uriString)
            )
            SnowbirdResult.Success(response)
        } catch (e: Exception) {
            e.printStackTrace()
            SnowbirdResult.Error(e.toSnowbirdError())
        }
    }

    private suspend fun fetchFromNetwork(): SnowbirdResult<List<SnowbirdGroup>> {
        return try {
            ensureServerReadyForNetwork()?.let { return it }
            val response = api.fetchGroups()
            lastFetchTime = System.currentTimeMillis()
            SnowbirdResult.Success(response.groups)
        } catch (e: Exception) {
            SnowbirdResult.Error(e.toSnowbirdError())
        }
    }

    private fun fetchFromCache(): SnowbirdResult<List<SnowbirdGroup>> {
        return SnowbirdResult.Success(SnowbirdGroup.getAll())
    }
}