package net.opendasharchive.openarchive.db

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "migration_state")
data class MigrationStateEntity(
    @PrimaryKey val id: Int = 1,
    val stage: String, // IDLE, SPACES, PROJECTS, COLLECTIONS, MEDIA, DONE
    val processedCount: Int,
    val totalCount: Int,
    val completedAt: Long? = null
)
