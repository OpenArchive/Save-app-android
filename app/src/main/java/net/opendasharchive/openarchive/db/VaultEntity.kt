package net.opendasharchive.openarchive.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vaults")
data class VaultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: Int,
    val name: String,
    val username: String,
    val displayname: String,
    val password: String,
    val host: String,
    val metaData: String,
    val licenseUrl: String?
)
