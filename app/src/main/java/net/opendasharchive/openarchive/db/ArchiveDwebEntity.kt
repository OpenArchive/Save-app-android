package net.opendasharchive.openarchive.db

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.PrimaryKey
import net.opendasharchive.openarchive.core.domain.ArchivePermission

@Entity(
    tableName = "archive_dweb_metadata",
    foreignKeys = [
        ForeignKey(
            entity = ArchiveEntity::class,
            parentColumns = ["id"],
            childColumns = ["archiveId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ArchiveDwebEntity(
    @PrimaryKey val archiveId: Long,
    val archiveKey: String,
    val archiveHash: String,
    val permissions: ArchivePermission
)
