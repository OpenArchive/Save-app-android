package net.opendasharchive.openarchive.db

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.PrimaryKey

@Entity(
    tableName = "vault_dweb_metadata",
    foreignKeys = [
        ForeignKey(
            entity = VaultEntity::class,
            parentColumns = ["id"],
            childColumns = ["vaultId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class VaultDwebEntity(
    @PrimaryKey val vaultId: Long,
    val vaultKey: String
)
