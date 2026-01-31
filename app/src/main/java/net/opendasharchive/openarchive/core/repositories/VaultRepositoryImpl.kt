package net.opendasharchive.openarchive.core.repositories

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.core.domain.Vault
import net.opendasharchive.openarchive.core.domain.mappers.toDomain
import net.opendasharchive.openarchive.core.domain.mappers.toVaultEntity
import net.opendasharchive.openarchive.db.ArchiveDao
import net.opendasharchive.openarchive.db.VaultDao
import net.opendasharchive.openarchive.util.Prefs

class VaultRepositoryImpl(
    private val vaultDao: VaultDao,
    private val archiveDao: ArchiveDao,
    private val settingsRepository: SettingsRepository,
    private val io: CoroutineDispatcher = Dispatchers.IO
) : SpaceRepository {

    override suspend fun getSpaces(): List<Vault> = withContext(io) {
        vaultDao.getAll().map { it.toDomain() }
    }

    override fun observeSpaces(): Flow<List<Vault>> = vaultDao.observeSpaces()
        .map { entities -> entities.map { it.toDomain() } }
        .distinctUntilChanged()

    override suspend fun getCurrentSpace(): Vault? = withContext(io) {
        val id = settingsRepository.observeCurrentSpaceId().first()
        val entity = if (id == -1L) {
            vaultDao.getAll().firstOrNull()
        } else {
            vaultDao.getById(id) ?: vaultDao.getAll().firstOrNull()
        }
        entity?.toDomain()
    }

    override fun observeCurrentSpace(): Flow<Vault?> = settingsRepository.observeCurrentSpaceId()
        .flatMapLatest { id ->
            if (id == -1L) {
                vaultDao.observeSpaces().map { it.firstOrNull() }
            } else {
                vaultDao.observeById(id).flatMapLatest { entity ->
                    if (entity == null) vaultDao.observeSpaces().map { it.firstOrNull() }
                    else flowOf(entity)
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

    override suspend fun updateSpace(vaultId: Long, vault: Vault): Boolean = withContext(io) {
        val entity = vault.toVaultEntity().copy(id = vaultId)
        val success = vaultDao.upsert(entity) > 0
        if (success) {
            archiveDao.updateLicenseForSpace(vaultId, vault.licenseUrl)
        }
        success
    }

    override suspend fun addSpace(vault: Vault): Long = withContext(io) {
        vaultDao.upsert(vault.toVaultEntity())
    }

    override suspend fun deleteSpace(id: Long): Boolean = withContext(io) {
        vaultDao.getById(id)?.let {
            vaultDao.delete(it)
            true
        } ?: false
    }
}
