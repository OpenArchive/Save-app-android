package net.opendasharchive.openarchive.core.repositories

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.core.domain.Vault
import net.opendasharchive.openarchive.core.domain.mappers.toDomain
import net.opendasharchive.openarchive.core.domain.mappers.toEntity
import net.opendasharchive.openarchive.db.sugar.Space

/**
 * Sugar-backed implementations; keep all ORM calls off the main thread.
 */
class SugarSpaceRepository(private val io: CoroutineDispatcher = Dispatchers.IO) : SpaceRepository {

    override suspend fun getSpaces(): List<Vault> = withContext(io) {
        Space.getAll().asSequence().toList().map { it.toDomain() }
    }

    override fun observeSpaces(): Flow<List<Vault>> = InvalidationBus.spaces
        .map { getSpaces() }
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
            val entity = vault.toEntity()
            entity.id = vaultId
            val savedId = entity.save()
            if (savedId > 0) {
                InvalidationBus.invalidateSpaces()
                InvalidationBus.invalidateCurrentSpace()
            }
            return@withContext savedId > 0
        }

    override suspend fun addSpace(vault: Vault): Long = withContext(io) {
        val id = vault.toEntity().save()
        if (id > 0) InvalidationBus.invalidateSpaces()
        id
    }

    override suspend fun getSpaceById(id: Long): Vault? = withContext(io) {
        Space.get(id)?.toDomain()
    }

    override suspend fun deleteSpace(id: Long): Boolean = withContext(io) {
        val deleted = Space.get(id)?.delete() ?: false
        if (deleted) {
            InvalidationBus.invalidateSpaces()
            InvalidationBus.invalidateCurrentSpace()
        }
        deleted
    }
}