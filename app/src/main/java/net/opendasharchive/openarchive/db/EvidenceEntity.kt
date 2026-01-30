package net.opendasharchive.openarchive.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "evidence",
    foreignKeys = [
        ForeignKey(
            entity = ArchiveEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SubmissionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("projectId"),
        Index("collectionId"),
        Index("status"),
        Index("priority"),
        Index("createDate")
    ]
)
data class EvidenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val originalFilePath: String,
    val mimeType: String,
    val createDate: Long?, // epoch ms
    val updateDate: Long?,
    val uploadDate: Long?,
    val serverUrl: String,
    val title: String,
    val description: String,
    val author: String,
    val location: String,
    val tags: String,
    val licenseUrl: String?,
    val mediaHash: ByteArray,
    val mediaHashString: String,
    val status: Int,
    val statusMessage: String,
    val projectId: Long,
    val collectionId: Long,
    val contentLength: Long,
    val progress: Long,
    val flag: Boolean,
    val priority: Int
)
