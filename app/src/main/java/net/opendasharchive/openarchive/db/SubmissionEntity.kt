package net.opendasharchive.openarchive.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "submissions",
    foreignKeys = [
        ForeignKey(
            entity = ArchiveEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class SubmissionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val uploadDate: Long?, // epoch ms
    val serverUrl: String?
)
