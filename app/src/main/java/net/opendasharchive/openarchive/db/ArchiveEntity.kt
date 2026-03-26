package net.opendasharchive.openarchive.db

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
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
