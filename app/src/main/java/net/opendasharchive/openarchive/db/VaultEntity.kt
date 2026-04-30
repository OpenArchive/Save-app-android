package net.opendasharchive.openarchive.db

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import kotlinx.datetime.LocalDateTime
import net.opendasharchive.openarchive.core.domain.VaultType

@Entity(tableName = "vaults")
data class VaultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: VaultType,
    val name: String,
    val username: String,
    val displayName: String,
    val host: String,
    val metaData: String,
    val licenseUrl: String?,
    val createdAt: LocalDateTime
)
