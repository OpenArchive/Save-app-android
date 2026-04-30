package net.opendasharchive.openarchive.db

import androidx.room3.Embedded
import androidx.room3.Relation

data class VaultWithDweb(
    @Embedded val vault: VaultEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "vaultId"
    )
    val dwebMetadata: VaultDwebEntity?
)

data class ArchiveWithDweb(
    @Embedded val archive: ArchiveEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "archiveId"
    )
    val dwebMetadata: ArchiveDwebEntity?
)

data class EvidenceWithDweb(
    @Embedded val evidence: EvidenceEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "evidenceId"
    )
    val dwebMetadata: EvidenceDwebEntity?
)
