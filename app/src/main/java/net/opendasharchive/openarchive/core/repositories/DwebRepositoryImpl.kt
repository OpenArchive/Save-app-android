package net.opendasharchive.openarchive.core.repositories

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.core.domain.Archive
import net.opendasharchive.openarchive.core.domain.Evidence
import net.opendasharchive.openarchive.core.domain.Vault
import net.opendasharchive.openarchive.core.domain.mappers.*
import net.opendasharchive.openarchive.db.ArchiveDao
import net.opendasharchive.openarchive.db.DwebDao
import net.opendasharchive.openarchive.db.EvidenceDao
import net.opendasharchive.openarchive.db.VaultDao

class DwebRepositoryImpl(
    private val dwebDao: DwebDao,
    private val vaultDao: VaultDao,
    private val archiveDao: ArchiveDao,
    private val evidenceDao: EvidenceDao,
    private val io: CoroutineDispatcher = Dispatchers.IO
) : DwebRepository {

    override fun observeVaults(): Flow<List<Vault>> = dwebDao.observeVaultsWithDweb()
        .map { entities -> entities.map { it.toDomain() } }
        .distinctUntilChanged()

    override fun observeVault(id: Long): Flow<Vault?> = dwebDao.observeVaultWithDwebById(id)
        .map { it?.toDomain() }
        .distinctUntilChanged()

    override suspend fun getVault(id: Long): Vault? = withContext(io) {
        dwebDao.getVaultWithDwebById(id)?.toDomain()
    }

    override suspend fun updateVaultMetadata(vault: Vault) {
        withContext(io) {
            vault.toDwebEntity()?.let {
                dwebDao.upsertVaultMetadata(it)
            }
        }
    }

    override fun observeArchives(vaultId: Long, archived: Boolean): Flow<List<Archive>> = 
        dwebDao.observeArchivesWithDweb(vaultId, archived)
            .map { entities -> entities.map { it.toDomain() } }
            .distinctUntilChanged()

    override fun observeArchive(id: Long): Flow<Archive?> = dwebDao.observeArchiveWithDwebById(id)
        .map { it?.toDomain() }
        .distinctUntilChanged()

    override suspend fun getArchive(id: Long): Archive? = withContext(io) {
        dwebDao.getArchiveWithDwebById(id)?.toDomain()
    }

    override suspend fun updateArchiveMetadata(archive: Archive) {
        withContext(io) {
            archive.toDwebEntity()?.let {
                dwebDao.upsertArchiveMetadata(it)
            }
        }
    }

    override fun observeEvidence(archiveId: Long): Flow<List<Evidence>> =
        dwebDao.observeEvidenceWithDweb(archiveId)
            .map { entities ->
                val project = archiveDao.getById(archiveId)
                entities.map { it.toDomain(vaultId = project?.vaultId ?: 0L) }
            }
            .distinctUntilChanged()

    override suspend fun getEvidence(id: Long): Evidence? = withContext(io) {
        dwebDao.getEvidenceWithDwebById(id)?.let { composite ->
            val project = archiveDao.getById(composite.evidence.archiveId)
            composite.toDomain(vaultId = project?.vaultId ?: 0L)
        }
    }

    override suspend fun updateEvidenceMetadata(evidence: Evidence) {
        withContext(io) {
            dwebDao.upsertEvidenceMetadata(evidence.toDwebEntity())
        }
    }
}
