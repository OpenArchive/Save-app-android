package net.opendasharchive.openarchive.db

import androidx.room3.Embedded
import androidx.room3.Relation

data class VaultWithDweb(
    @Embedded val vault: VaultEntity,
    @Relation(
        parentColumns = ["id"],
        entityColumns = ["vaultId"]
    )
    val dwebMetadata: VaultDwebEntity?
)

data class ArchiveWithDweb(
    @Embedded val archive: ArchiveEntity,
    @Relation(
        parentColumns = ["id"],
        entityColumns = ["archiveId"]
    )
    val dwebMetadata: ArchiveDwebEntity?
)

data class EvidenceWithDweb(
    @Embedded val evidence: EvidenceEntity,
    @Relation(
        parentColumns = ["id"],
        entityColumns = ["evidenceId"]
    )
    val dwebMetadata: EvidenceDwebEntity?
)
