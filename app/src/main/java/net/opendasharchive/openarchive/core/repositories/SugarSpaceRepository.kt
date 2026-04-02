package net.opendasharchive.openarchive.core.repositories

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.core.domain.VaultAuth
import net.opendasharchive.openarchive.core.domain.Vault
import net.opendasharchive.openarchive.core.domain.VaultType
import net.opendasharchive.openarchive.core.domain.mappers.toDomain
import net.opendasharchive.openarchive.core.domain.mappers.toEntity
import net.opendasharchive.openarchive.core.security.VaultCredentialStore
import net.opendasharchive.openarchive.db.sugar.Space


/**
 * Sugar-backed implementations; keep all ORM calls off the main thread.
 *
 * Credentials (passwords) are stored in [VaultCredentialStore] (AES-GCM + Android Keystore)
 * rather than in the SugarORM SQLite database. Legacy plaintext passwords are migrated lazily
 * on first access via [getVaultAuth].
 */
class SugarSpaceRepository(
    private val context: Context,
    private val credentialStore: VaultCredentialStore,
    private val io: CoroutineDispatcher = Dispatchers.IO
) : SpaceRepository {

    override suspend fun getSpaces(): List<Vault> = withContext(io) {
        Space.getAll().asSequence()
            .toList()
            .map { it.toDomain() }
            .filter { it.type == VaultType.INTERNET_ARCHIVE || it.type == VaultType.PRIVATE_SERVER }
    }

    override fun observeSpaces(): Flow<List<Vault>> = InvalidationBus.spaces
        .map { getSpaces() }
        .distinctUntilChanged()

    override fun observeHasDwebSpace(): Flow<Boolean> = InvalidationBus.spaces
        .map {
            Space.getAll().asSequence().toList().any { entity ->
                entity.toDomain().type == VaultType.DWEB_STORAGE
            }
        }
        .distinctUntilChanged()

    override suspend fun getCurrentSpace(): Vault? = withContext(io) {
        Space.current?.toDomain()
    }

    override fun observeCurrentSpace(): Flow<Vault?> = kotlinx.coroutines.flow.combine(
        InvalidationBus.spaces,
        InvalidationBus.currentSpace
    ) { _, _ ->
        getCurrentSpace() ?: getSpaces().firstOrNull()
    }.distinctUntilChanged()

    override fun observeSpace(id: Long): Flow<Vault?> = InvalidationBus.spaces
        .map { getSpaceById(id) }
        .distinctUntilChanged()

    override suspend fun setCurrentSpace(id: Long) {
        withContext(io) {
            val space = Space.get(id)
            if (space != null) {
                Space.current = space
                InvalidationBus.invalidateCurrentSpace()
                InvalidationBus.invalidateProjects()
                InvalidationBus.invalidateCollections()
                InvalidationBus.invalidateMedia()
            }
        }
    }

    override suspend fun updateSpace(vaultId: Long, vault: Vault): Boolean =
        withContext(io) {
            // Guard: migrate any legacy plaintext password from Sugar DB to SecureStorage
            // before wiping it. The domain model always carries password="" (toDomain()),
            // so if this call happens before getVaultAuth() the credential would be lost.
            val existingSpace = Space.get(vaultId)
            if (existingSpace != null && existingSpace.password.isNotBlank() && !credentialStore.hasSecret(vaultId)) {
                credentialStore.putSecret(vaultId, existingSpace.password)
            }

            val entity = vault.toEntity()
            entity.id = vaultId
            entity.password = ""  // never persist password in Sugar DB
            val savedId = entity.save()
            if (savedId > 0) {
                if (vault.password.isNotBlank()) {
                    credentialStore.putSecret(vaultId, vault.password)
                }
                InvalidationBus.invalidateSpaces()
                InvalidationBus.invalidateCurrentSpace()
            }
            return@withContext savedId > 0
        }

    override suspend fun addSpace(vault: Vault): Long = withContext(io) {
        val entity = vault.toEntity()
        entity.password = ""  // never persist password in Sugar DB
        val id = entity.save()
        if (id > 0) {
            if (vault.password.isNotBlank()) {
                credentialStore.putSecret(id, vault.password)
            }
            InvalidationBus.invalidateSpaces()
        }
        id
    }

    override suspend fun getSpaceById(id: Long): Vault? = withContext(io) {
        Space.get(id)?.toDomain()
    }

    override suspend fun getVaultAuth(vaultId: Long): VaultAuth? = withContext(io) {
        val space = Space.get(vaultId) ?: return@withContext null

        // Lazy migration: if a plaintext password exists in the Sugar DB and the
        // secure store has no entry yet, migrate it now and wipe it from Sugar.
        val secret = if (credentialStore.hasSecret(vaultId)) {
            credentialStore.getSecret(vaultId)
        } else if (space.password.isNotBlank()) {
            credentialStore.putSecret(vaultId, space.password)
            space.password = ""
            space.save()
            credentialStore.getSecret(vaultId)
        } else {
            null
        }

        VaultAuth(
            vaultId = space.id,
            type = space.toDomain().type,
            username = space.username,
            secret = secret.orEmpty()
        )
    }

    override suspend fun deleteSpace(id: Long): Boolean = withContext(io) {
        val deleted = Space.get(id)?.delete() ?: false
        if (deleted) {
            credentialStore.deleteSecret(id)
            InvalidationBus.invalidateSpaces()
            InvalidationBus.invalidateCurrentSpace()
        }
        deleted
    }
}
