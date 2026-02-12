package net.opendasharchive.openarchive.services.snowbird

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.opendasharchive.openarchive.core.domain.Vault
import net.opendasharchive.openarchive.db.DwebDao
import net.opendasharchive.openarchive.db.VaultDao
import net.opendasharchive.openarchive.extensions.toSnowbirdError
import net.opendasharchive.openarchive.services.snowbird.data.toDwebEntity
import net.opendasharchive.openarchive.services.snowbird.data.toDomain
import net.opendasharchive.openarchive.services.snowbird.data.toVaultEntity
import net.opendasharchive.openarchive.services.snowbird.service.ISnowbirdAPI
import net.opendasharchive.openarchive.services.snowbird.data.*

interface ISnowbirdGroupRepository {
    suspend fun createGroup(groupName: String): SnowbirdResult<Vault>
    suspend fun fetchGroups(forceRefresh: Boolean = false): SnowbirdResult<Unit>
    fun observeGroups(): Flow<List<Vault>>
    suspend fun joinGroup(uriString: String): SnowbirdResult<JoinGroupResponse>
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

    override suspend fun createGroup(groupName: String): SnowbirdResult<Vault> {
        return try {
            val response = api.createGroup(RequestName(groupName))
            // Map SnowbirdGroup to Vault entities and save to Room
            val vaultEntity = response.toVaultEntity()
            val vaultId = vaultDao.upsert(vaultEntity)
            val dwebEntity = response.toDwebEntity(vaultId)
            dwebDao.upsertVaultMetadata(dwebEntity)
            
            // Fetch the freshly saved vault with its metadata
            val savedVaultWithDweb = dwebDao.getVaultWithDwebById(vaultId)
            val savedVault = savedVaultWithDweb?.toDomain()
            if (savedVault != null) {
                SnowbirdResult.Success(savedVault)
            } else {
                SnowbirdResult.Error(SnowbirdError.GeneralError("Failed to retrieve saved group"))
            }
        } catch (e: Exception) {
            SnowbirdResult.Error(e.toSnowbirdError())
        }
    }

    override suspend fun fetchGroups(forceRefresh: Boolean): SnowbirdResult<Unit> {
        return try {
            val response = api.fetchGroups()
            // In a real implementation, we would sync/upsert these into Room
            // For now, let's upsert them
            response.groups.forEach { groupDto ->
                // Check if already exists by key to avoid duplicates if possible, 
                // but since we don't have a direct key-to-vault-id mapping easily, 
                // we might need to handle this carefully.
                // For simplicity now, just upsert.
                val vaultEntity = groupDto.toVaultEntity()
                val vaultId = vaultDao.upsert(vaultEntity)
                val dwebEntity = groupDto.toDwebEntity(vaultId)
                dwebDao.upsertVaultMetadata(dwebEntity)
            }
            SnowbirdResult.Success(Unit)
        } catch (e: Exception) {
            SnowbirdResult.Error(e.toSnowbirdError())
        }
    }

    override suspend fun joinGroup(uriString: String): SnowbirdResult<JoinGroupResponse> {
        return try {
            val response = api.joinGroup(MembershipRequest(uriString))
            SnowbirdResult.Success(response)
        } catch (e: Exception) {
            SnowbirdResult.Error(e.toSnowbirdError())
        }
    }
}