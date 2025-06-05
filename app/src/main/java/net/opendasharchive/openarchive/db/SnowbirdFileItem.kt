package net.opendasharchive.openarchive.db

import android.database.sqlite.SQLiteException
import com.orm.SugarRecord
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class SnowbirdFileList(
    var files: List<SnowbirdFileItem>
) : SerializableMarker

@Serializable
data class SnowbirdFileItem(
    var hash: String = "",
    var name: String = "",
    @Transient var groupKey: String = "",
    @Transient var repoKey: String = "",
    @SerialName("is_downloaded") var isDownloaded: Boolean = false
): SugarRecord(), SerializableMarker {
    companion object {
        fun clear() {
            try {
                deleteAll(SnowbirdFileItem::class.java)
            } catch (e: SQLiteException) {
                // Probably because table doesn't exist. Ignore.
            }
        }

        fun findBy(groupKey: String, repoKey: String): List<SnowbirdFileItem> {
            val whereClause = "GROUP_KEY = ? AND REPO_KEY = ?"
            val whereArgs = mutableListOf(groupKey, repoKey)

            val items = find(
                SnowbirdFileItem::class.java,
                whereClause,
                whereArgs.toTypedArray(),
                null,
                null,
                null)

            return items
        }
    }

    fun saveWith(groupKey: String, repoKey: String) {
        this.groupKey = groupKey
        this.repoKey = repoKey
        save()
    }
}


