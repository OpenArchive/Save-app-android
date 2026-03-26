package net.opendasharchive.openarchive.db

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.PrimaryKey

@Entity(
    tableName = "evidence_dweb_metadata",
    foreignKeys = [
        ForeignKey(
            entity = EvidenceEntity::class,
            parentColumns = ["id"],
            childColumns = ["evidenceId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class EvidenceDwebEntity(
    @PrimaryKey val evidenceId: Long,
    val isDownloaded: Boolean
)
