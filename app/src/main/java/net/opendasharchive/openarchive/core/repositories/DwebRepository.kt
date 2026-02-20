package net.opendasharchive.openarchive.core.repositories

import kotlinx.coroutines.flow.Flow
import net.opendasharchive.openarchive.core.domain.Archive
import net.opendasharchive.openarchive.core.domain.Evidence
import net.opendasharchive.openarchive.core.domain.Vault

interface DwebRepository {
    fun observeVaults(): Flow<List<Vault>>
    fun observeVault(id: Long): Flow<Vault?>
    suspend fun getVault(id: Long): Vault?
    suspend fun updateVaultMetadata(vault: Vault)

    fun observeArchives(vaultId: Long, archived: Boolean): Flow<List<Archive>>
    fun observeArchive(id: Long): Flow<Archive?>
    suspend fun getArchive(id: Long): Archive?
    suspend fun updateArchiveMetadata(archive: Archive)

    fun observeEvidence(archiveId: Long): Flow<List<Evidence>>
    suspend fun getEvidence(id: Long): Evidence?
    suspend fun updateEvidenceMetadata(evidence: Evidence)
}
