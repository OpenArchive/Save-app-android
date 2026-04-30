package net.opendasharchive.openarchive.core.repositories

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.core.domain.VaultAuth
import net.opendasharchive.openarchive.core.domain.Vault
import net.opendasharchive.openarchive.core.domain.VaultType
import net.opendasharchive.openarchive.core.domain.mappers.toDomain
import net.opendasharchive.openarchive.core.domain.mappers.toVaultEntity
import net.opendasharchive.openarchive.core.security.VaultCredentialStore
import net.opendasharchive.openarchive.db.ArchiveDao
import net.opendasharchive.openarchive.db.VaultDao


class VaultRepositoryImpl(
    private val context: Context,
    private val vaultDao: VaultDao,
    private val archiveDao: ArchiveDao,
    private val settingsRepository: SettingsRepository,
    private val credentialStore: VaultCredentialStore,
    private val io: CoroutineDispatcher = Dispatchers.IO
) : SpaceRepository {

    override suspend fun getSpaces(): List<Vault> = withContext(io) {
        vaultDao.getAll().map { it.toDomain() }
    }

    override fun observeSpaces(): Flow<List<Vault>> = vaultDao.observeVaults()
        .map { entities -> entities.map { it.toDomain() } }
        .distinctUntilChanged()

    override fun observeHasDwebSpace(): Flow<Boolean> = vaultDao.observeHasDwebSpace()
        .distinctUntilChanged()

    override suspend fun getCurrentSpace(): Vault? = withContext(io) {
        val id = settingsRepository.observeCurrentSpaceId().first()
        val regularSpaces = vaultDao.getAll()
        val entity = if (id == -1L) {
            regularSpaces.firstOrNull()
        } else {
            val selected = vaultDao.getById(id)
            if (selected?.type == VaultType.INTERNET_ARCHIVE || selected?.type == VaultType.PRIVATE_SERVER) {
                selected
            } else {
                regularSpaces.firstOrNull()
            }
        }
        entity?.toDomain()
    }

    override fun observeCurrentSpace(): Flow<Vault?> = settingsRepository.observeCurrentSpaceId()
        .flatMapLatest { id ->
            if (id == -1L) {
                vaultDao.observeVaults().map { it.firstOrNull() }
            } else {
                vaultDao.observeById(id).flatMapLatest { entity ->
                    if (entity == null || entity.type == VaultType.DWEB_STORAGE) {
                        vaultDao.observeVaults().map { it.firstOrNull() }
                    } else {
                        flowOf(entity)
                    }
                }
            }
        }
        .map { it?.toDomain() }
        .distinctUntilChanged()

    override fun observeSpace(id: Long): Flow<Vault?> = vaultDao.observeById(id)
        .map { it?.toDomain() }
        .distinctUntilChanged()

    override suspend fun setCurrentSpace(id: Long) {
        withContext(io) {
            settingsRepository.setCurrentSpaceId(id)
        }
    }

    override suspend fun getSpaceById(id: Long): Vault? = withContext(io) {
        vaultDao.getById(id)?.toDomain()
    }

    override suspend fun getVaultAuth(vaultId: Long): VaultAuth? = withContext(io) {
        val vault = vaultDao.getById(vaultId)?.toDomain() ?: return@withContext null
        val secret = credentialStore.getSecret(vaultId) ?: return@withContext null
        VaultAuth(
            vaultId = vault.id,
            type = vault.type,
            username = vault.username,
            secret = secret
        )
    }

    override suspend fun updateSpace(vaultId: Long, vault: Vault): Boolean = withContext(io) {
        val oldVault = vaultDao.getById(vaultId)
        val entity = vault.toVaultEntity().copy(id = vaultId)
        val success = vaultDao.upsert(entity) > 0
        if (success) {
            if (vault.password.isNotBlank()) {
                credentialStore.putSecret(vaultId, vault.password)
            }
            if (oldVault?.host != entity.host || oldVault.username != entity.username) {
                archiveDao.resetRemoteStatusForVault(vaultId)
            }
            archiveDao.updateLicenseForVault(vaultId, vault.licenseUrl)
        }
        success
    }

    override suspend fun addSpace(vault: Vault): Long = withContext(io) {
        val vaultId = vaultDao.upsert(vault.toVaultEntity())
        if (vaultId > 0 && vault.password.isNotBlank()) {
            credentialStore.putSecret(vaultId, vault.password)
        }
        vaultId
    }

    override suspend fun deleteSpace(id: Long): Boolean = withContext(io) {
        vaultDao.getById(id)?.let {
            vaultDao.delete(it)
            credentialStore.deleteSecret(id)
            true
        } ?: false
    }
}
