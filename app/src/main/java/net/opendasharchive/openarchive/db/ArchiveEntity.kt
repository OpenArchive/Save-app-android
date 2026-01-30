package net.opendasharchive.openarchive.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "archives",
    foreignKeys = [
        ForeignKey(
            entity = VaultEntity::class,
            parentColumns = ["id"],
            childColumns = ["spaceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("spaceId")]
)
data class ArchiveEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val description: String?,
    val created: Long?, // epoch ms
    val spaceId: Long,
    val archived: Boolean,
    val openCollectionId: Long,
    val licenseUrl: String?
)
