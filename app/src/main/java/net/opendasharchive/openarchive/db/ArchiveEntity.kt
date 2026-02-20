package net.opendasharchive.openarchive.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDateTime

@Entity(
    tableName = "archives",
    foreignKeys = [
        ForeignKey(
            entity = VaultEntity::class,
            parentColumns = ["id"],
            childColumns = ["vaultId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("vaultId")]
)
data class ArchiveEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val description: String?,
    val createdAt: LocalDateTime?,
    val vaultId: Long,
    val archived: Boolean,
    val openSubmissionId: Long,
    val licenseUrl: String?,
    val isRemote: Boolean
)
