package net.opendasharchive.openarchive.db

import android.database.sqlite.SQLiteException
import com.orm.SugarRecord
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@Serializable
data class SnowbirdFileList(
    var files: List<FetchFileResponse>
) : SerializableMarker

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys
data class FetchFileResponse(
    val name: String,
    val hash: String,
    @SerialName("is_downloaded")
    val isDownloaded: Boolean = false,
    val size: Long? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
)

fun FetchFileResponse.toFile(
    groupKey: String,
    repoKey: String,
): SnowbirdFileItem {
    val file = SnowbirdFileItem(
        name = name,
        hash = hash,
        isDownloaded = isDownloaded,
        size = size ?: 0L,
        groupKey = groupKey,
        repoKey = repoKey,
    )

    return file
}

@Serializable
data class SnowbirdFileItem(
    var hash: String = "",
    var name: String = "",
    var size: Long = 0L,
    @Transient var groupKey: String = "",
    @Transient var repoKey: String = "",
    @SerialName("is_downloaded") var isDownloaded: Boolean = false
) : SugarRecord(), SerializableMarker {
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
                null
            )

            return items
        }

        fun findBy(groupKey: String, repoKey: String, name: String): SnowbirdFileItem? {
            val whereClause = "GROUP_KEY = ? AND REPO_KEY = ? AND NAME = ?"
            val whereArgs = arrayOf(groupKey, repoKey, name)
            return find(
                SnowbirdFileItem::class.java,
                whereClause,
                whereArgs,
                null,
                null,
                null
            ).firstOrNull()
        }
    }

    fun saveWith(groupKey: String, repoKey: String) {
        this.groupKey = groupKey
        this.repoKey = repoKey
        save()
    }
}


