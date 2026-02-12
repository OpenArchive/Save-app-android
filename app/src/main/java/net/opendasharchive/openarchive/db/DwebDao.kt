package net.opendasharchive.openarchive.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DwebDao {

    // --- Vaults ---

    @Transaction
    @Query("SELECT * FROM vaults")
    fun observeVaultsWithDweb(): Flow<List<VaultWithDweb>>

    @Transaction
    @Query("SELECT * FROM vaults WHERE id = :id")
    fun observeVaultWithDwebById(id: Long): Flow<VaultWithDweb?>

    @Transaction
    @Query("SELECT v.* FROM vaults v INNER JOIN vault_dweb_metadata m ON v.id = m.vaultId WHERE m.vaultKey = :key")
    suspend fun getVaultWithDwebByKey(key: String): VaultWithDweb?

    @Transaction
    @Query("SELECT * FROM vaults WHERE id = :id")
    suspend fun getVaultWithDwebById(id: Long): VaultWithDweb?

    @Upsert
    suspend fun upsertVaultMetadata(entity: VaultDwebEntity)

    // --- Archives ---

    @Transaction
    @Query("SELECT * FROM archives WHERE vaultId = :vaultId AND archived = :archived ORDER BY id DESC")
    fun observeArchivesWithDweb(vaultId: Long, archived: Boolean): Flow<List<ArchiveWithDweb>>

    @Transaction
    @Query("SELECT * FROM archives WHERE id = :id")
    fun observeArchiveWithDwebById(id: Long): Flow<ArchiveWithDweb?>

    @Transaction
    @Query("SELECT * FROM archives WHERE id = :id")
    suspend fun getArchiveWithDwebById(id: Long): ArchiveWithDweb?

    @Upsert
    suspend fun upsertArchiveMetadata(entity: ArchiveDwebEntity)

    // --- Evidence ---

    @Transaction
    @Query("SELECT * FROM evidence WHERE archiveId = :archiveId")
    fun observeEvidenceWithDweb(archiveId: Long): Flow<List<EvidenceWithDweb>>

    @Transaction
    @Query("SELECT * FROM evidence WHERE id = :id")
    suspend fun getEvidenceWithDwebById(id: Long): EvidenceWithDweb?

    @Upsert
    suspend fun upsertEvidenceMetadata(entity: EvidenceDwebEntity)
}
