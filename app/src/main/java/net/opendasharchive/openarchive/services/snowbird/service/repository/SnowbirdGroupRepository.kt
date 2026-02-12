package net.opendasharchive.openarchive.services.snowbird.service.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.opendasharchive.openarchive.core.domain.DomainError
import net.opendasharchive.openarchive.core.domain.DomainResult
import net.opendasharchive.openarchive.core.domain.Vault
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.db.DwebDao
import net.opendasharchive.openarchive.db.VaultDao
import net.opendasharchive.openarchive.extensions.toDomainError
import net.opendasharchive.openarchive.services.snowbird.data.toDwebEntity
import net.opendasharchive.openarchive.services.snowbird.data.toDomain
import net.opendasharchive.openarchive.services.snowbird.data.toVaultEntity
import net.opendasharchive.openarchive.services.snowbird.service.ISnowbirdAPI
import net.opendasharchive.openarchive.services.snowbird.data.*

interface ISnowbirdGroupRepository {
    suspend fun createGroup(groupName: String): DomainResult<Vault>
    suspend fun fetchGroups(forceRefresh: Boolean = false): DomainResult<Unit>
    fun observeGroups(): Flow<List<Vault>>
    suspend fun joinGroup(uriString: String): DomainResult<JoinGroupResponse>
}

class SnowbirdGroupRepository(
    private val api: ISnowbirdAPI,
    private val vaultDao: VaultDao,
    private val dwebDao: DwebDao
) : ISnowbirdGroupRepository {

    override fun observeGroups(): Flow<List<Vault>> {
        return dwebDao.observeVaultsWithDweb().map { vaultList ->
            vaultList.map { it.toDomain() }
        }
    }

    override suspend fun createGroup(groupName: String): DomainResult<Vault> {
        return try {
            val response = api.createGroup(RequestName(groupName))
            
            // Deduplication lookup STRICTLY by DWeb key
            val existingVaultId = dwebDao.getVaultIdByKey(response.key)
            
            // Map SnowbirdGroup to Vault entity using the fixed ID (if exists) or 0 (for new)
            val vaultEntity = response.toVaultEntity(id = existingVaultId ?: 0L)
            val upsertedId = vaultDao.upsert(vaultEntity)
            val vaultId = existingVaultId ?: upsertedId
            
            val dwebEntity = response.toDwebEntity(vaultId)
            dwebDao.upsertVaultMetadata(dwebEntity)
            
            // Fetch the freshly saved vault with its metadata
            val savedVaultWithDweb = dwebDao.getVaultWithDwebById(vaultId)
            val savedVault = savedVaultWithDweb?.toDomain()
            if (savedVault != null) {
                DomainResult.Success(savedVault)
            } else {
                DomainResult.Error(DomainError.Unknown("Failed to retrieve saved group"))
            }
        } catch (e: Exception) {
            AppLogger.e(e)
            DomainResult.Error(e.toDomainError())
        }
    }

    override suspend fun fetchGroups(forceRefresh: Boolean): DomainResult<Unit> {
        return try {
            val response = api.fetchGroups()
            response.groups.forEach { groupDto ->
                // Deduplication lookup STRICTLY by DWeb key
                val existingVaultId = dwebDao.getVaultIdByKey(groupDto.key)
                
                // Map using the fixed ID (if exists) or 0 (for new)
                val vaultEntity = groupDto.toVaultEntity(id = existingVaultId ?: 0L)
                val upsertedId = vaultDao.upsert(vaultEntity)
                val vaultId = existingVaultId ?: upsertedId
                
                val dwebEntity = groupDto.toDwebEntity(vaultId)
                dwebDao.upsertVaultMetadata(dwebEntity)
            }
            DomainResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e(e)
            DomainResult.Error(e.toDomainError())
        }
    }

    override suspend fun joinGroup(uriString: String): DomainResult<JoinGroupResponse> {
        return try {
            val response = api.joinGroup(MembershipRequest(uriString))
            DomainResult.Success(response)
        } catch (e: Exception) {
            AppLogger.e(e)
            DomainResult.Error(e.toDomainError())
        }
    }
}