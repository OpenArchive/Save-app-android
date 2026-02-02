package net.opendasharchive.openarchive.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDateTime
import net.opendasharchive.openarchive.core.domain.EvidenceStatus

@Entity(
    tableName = "evidence",
    foreignKeys = [
        ForeignKey(
            entity = ArchiveEntity::class,
            parentColumns = ["id"],
            childColumns = ["archiveId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SubmissionEntity::class,
            parentColumns = ["id"],
            childColumns = ["submissionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("archiveId"),
        Index("submissionId"),
        Index("status"),
        Index("priority"),
        Index("createdAt")
    ]
)
data class EvidenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val originalFilePath: String,
    val mimeType: String,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
    val uploadedAt: LocalDateTime?,
    val serverUrl: String,
    val title: String,
    val description: String,
    val author: String,
    val location: String,
    val tags: String,
    val licenseUrl: String?,
    val mediaHashString: String,
    val status: EvidenceStatus,
    val statusMessage: String,
    val archiveId: Long,
    val submissionId: Long,
    val contentLength: Long,
    val progress: Long,
    val flag: Boolean,
    val priority: Int,
    val thumbnail: ByteArray? = null
)
