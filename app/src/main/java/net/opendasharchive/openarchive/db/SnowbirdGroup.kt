package net.opendasharchive.openarchive.db

import android.database.sqlite.SQLiteException
import com.orm.SugarRecord
import kotlinx.serialization.Serializable

@Serializable
data class SnowbirdGroupList(
    var groups: List<SnowbirdGroup>
) : SerializableMarker

@Serializable
data class SnowbirdGroup(
    var key: String = "",
    var name: String? = null,
    var uri: String? = null
) : SugarRecord(), SerializableMarker {
    companion object {
        fun clear() {
            try {
                deleteAll(SnowbirdGroup::class.java)
            } catch (e: SQLiteException) {
                // Probably because table doesn't exist. Ignore.
            }
        }

        fun getAll(): List<SnowbirdGroup> {
            return findAll(SnowbirdGroup::class.java).asSequence().toList()
        }

        fun exists(name: String): Boolean {
            val whereClause = "name = ?"
            val whereArgs = mutableListOf(name)

            return find(
                SnowbirdGroup::class.java, whereClause, whereArgs.toTypedArray(),
                null,
                null,
                null).isNotEmpty()
        }

        fun get(key: String): SnowbirdGroup? {
            val whereClause = "key = ?"
            val whereArgs = mutableListOf(key)

            return find(
                SnowbirdGroup::class.java, whereClause, whereArgs.toTypedArray(),
                null,
                null,
                null).firstOrNull()
        }
    }
}

fun SnowbirdGroup.shortHash(): String {
    return key.take(10)
}