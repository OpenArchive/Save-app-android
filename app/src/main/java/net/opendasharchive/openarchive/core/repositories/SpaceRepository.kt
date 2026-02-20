package net.opendasharchive.openarchive.core.repositories

import kotlinx.coroutines.flow.Flow
import net.opendasharchive.openarchive.core.domain.Vault

interface SpaceRepository {
    suspend fun getSpaces(): List<Vault>
    fun observeSpaces(): Flow<List<Vault>>
    suspend fun getCurrentSpace(): Vault?
    fun observeCurrentSpace(): Flow<Vault?>
    suspend fun setCurrentSpace(id: Long)
    fun observeSpace(id: Long): Flow<Vault?>

    suspend fun getSpaceById(id: Long): Vault?
    suspend fun updateSpace(vaultId: Long, vault: Vault): Boolean
    suspend fun addSpace(vault: Vault): Long
    suspend fun deleteSpace(id: Long): Boolean
}

