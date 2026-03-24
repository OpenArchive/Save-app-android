package net.opendasharchive.openarchive.db

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import kotlinx.datetime.LocalDateTime

@Entity(
    tableName = "submissions",
    foreignKeys = [
        ForeignKey(
            entity = ArchiveEntity::class,
            parentColumns = ["id"],
            childColumns = ["archiveId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("archiveId")]
)
data class SubmissionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val archiveId: Long,
    val uploadedAt: LocalDateTime?,
    val serverUrl: String?
)
