package net.opendasharchive.openarchive.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime

@Dao
interface DwebDao {

    companion object {
        // VaultType.DWEB_STORAGE converter value
        private const val DWEB_VAULT_TYPE = 5
    }

    // --- Vaults ---

    @Transaction
    @Query(
        """
        SELECT v.* FROM vaults v
        INNER JOIN vault_dweb_metadata m ON v.id = m.vaultId
        WHERE v.type = $DWEB_VAULT_TYPE
        """
    )
    fun observeVaultsWithDweb(): Flow<List<VaultWithDweb>>

    @Transaction
    @Query(
        """
        SELECT v.* FROM vaults v
        INNER JOIN vault_dweb_metadata m ON v.id = m.vaultId
        WHERE v.id = :id AND v.type = $DWEB_VAULT_TYPE
        """
    )
    fun observeVaultWithDwebById(id: Long): Flow<VaultWithDweb?>

    @Transaction
    @Query(
        """
        SELECT v.* FROM vaults v
        INNER JOIN vault_dweb_metadata m ON v.id = m.vaultId
        WHERE m.vaultKey = :key AND v.type = $DWEB_VAULT_TYPE
        """
    )
    suspend fun getVaultWithDwebByKey(key: String): VaultWithDweb?

    @Transaction
    @Query(
        """
        SELECT v.* FROM vaults v
        INNER JOIN vault_dweb_metadata m ON v.id = m.vaultId
        WHERE v.id = :id AND v.type = $DWEB_VAULT_TYPE
        """
    )
    suspend fun getVaultWithDwebById(id: Long): VaultWithDweb?

    @Query("SELECT vaultId FROM vault_dweb_metadata WHERE vaultKey = :key LIMIT 1")
    suspend fun getVaultIdByKey(key: String): Long?

    @Upsert
    suspend fun upsertVaultMetadata(entity: VaultDwebEntity)

    // --- Archives ---

    @Transaction
    @Query(
        """
        SELECT a.* FROM archives a
        INNER JOIN vaults v ON v.id = a.vaultId
        INNER JOIN archive_dweb_metadata m ON m.archiveId = a.id
        WHERE a.vaultId = :vaultId
          AND a.archived = :archived
          AND v.type = $DWEB_VAULT_TYPE
        ORDER BY a.id DESC
        """
    )
    fun observeArchivesWithDweb(vaultId: Long, archived: Boolean): Flow<List<ArchiveWithDweb>>

    @Transaction
    @Query(
        """
        SELECT a.* FROM archives a
        INNER JOIN vaults v ON v.id = a.vaultId
        INNER JOIN archive_dweb_metadata m ON m.archiveId = a.id
        WHERE a.id = :id AND v.type = $DWEB_VAULT_TYPE
        """
    )
    fun observeArchiveWithDwebById(id: Long): Flow<ArchiveWithDweb?>

    @Transaction
    @Query(
        """
        SELECT a.* FROM archives a
        INNER JOIN vaults v ON v.id = a.vaultId
        INNER JOIN archive_dweb_metadata m ON m.archiveId = a.id
        WHERE a.id = :id AND v.type = $DWEB_VAULT_TYPE
        """
    )
    suspend fun getArchiveWithDwebById(id: Long): ArchiveWithDweb?

    @Query("SELECT archiveId FROM archive_dweb_metadata WHERE archiveKey = :key LIMIT 1")
    suspend fun getArchiveIdByKey(key: String): Long?

    @Upsert
    suspend fun upsertArchiveMetadata(entity: ArchiveDwebEntity)

    // --- Evidence ---

    @Transaction
    @Query(
        """
        SELECT e.* FROM evidence e
        INNER JOIN archives a ON a.id = e.archiveId
        INNER JOIN vaults v ON v.id = a.vaultId
        INNER JOIN evidence_dweb_metadata m ON m.evidenceId = e.id
        WHERE e.archiveId = :archiveId
          AND v.type = $DWEB_VAULT_TYPE
        """
    )
    fun observeEvidenceWithDweb(archiveId: Long): Flow<List<EvidenceWithDweb>>

    @Transaction
    @Query(
        """
        SELECT e.* FROM evidence e
        INNER JOIN archives a ON a.id = e.archiveId
        INNER JOIN vaults v ON v.id = a.vaultId
        INNER JOIN evidence_dweb_metadata m ON m.evidenceId = e.id
        WHERE e.id = :id
          AND v.type = $DWEB_VAULT_TYPE
        """
    )
    suspend fun getEvidenceWithDwebById(id: Long): EvidenceWithDweb?

    // --- Submissions ---

    @Query(
        """
        SELECT s.* FROM submissions s
        INNER JOIN archives a ON a.id = s.archiveId
        INNER JOIN vaults v ON v.id = a.vaultId
        WHERE s.archiveId = :archiveId
          AND v.type = $DWEB_VAULT_TYPE
        ORDER BY s.id DESC
        """
    )
    fun observeSubmissionsForDwebArchive(archiveId: Long): Flow<List<SubmissionEntity>>

    @Upsert
    suspend fun upsertEvidenceMetadata(entity: EvidenceDwebEntity)

    @Query(
        """
        UPDATE evidence
        SET originalFilePath = :localFilePath,
            mimeType = :mimeType,
            updatedAt = :updatedAt
        WHERE id = :evidenceId
        """
    )
    suspend fun updateEvidenceLocalFilePath(
        evidenceId: Long,
        localFilePath: String,
        mimeType: String,
        updatedAt: LocalDateTime
    )

    @Transaction
    suspend fun markEvidenceDownloaded(
        evidenceId: Long,
        localFilePath: String,
        mimeType: String,
        updatedAt: LocalDateTime
    ) {
        updateEvidenceLocalFilePath(evidenceId, localFilePath, mimeType, updatedAt)
        upsertEvidenceMetadata(
            EvidenceDwebEntity(
                evidenceId = evidenceId,
                isDownloaded = true
            )
        )
    }
}
