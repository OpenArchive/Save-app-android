package net.opendasharchive.openarchive.db

import com.orm.SugarRecord
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SnowbirdRepoList(
    var repos: List<FetchRepoResponse>
) : SerializableMarker

@Serializable
data class FetchRepoResponse(
    val name: String,
    val key: String,
    @SerialName("can_write")
    val canWrite: Boolean,
)

@Serializable
data class CreateRepoResponse(
    val key: String,
    val name: String,
    @SerialName("can_write")
    val canWrite: Boolean? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
) : SerializableMarker

fun FetchRepoResponse.toRepo(groupKey: String): SnowbirdRepo {
    return SnowbirdRepo(
        key = key,
        name = name,
        groupKey = groupKey,
        permissions = if (canWrite) "READ_WRITE" else "READ_ONLY",
    )
}

fun CreateRepoResponse.toRepo(groupKey: String): SnowbirdRepo {
    return SnowbirdRepo(
        key = key,
        name = name,
        groupKey = groupKey,
        permissions = if (canWrite == true) "READ_WRITE" else "READ_ONLY",
        createdAt = createdAt
    )
}

@Serializable
data class SnowbirdRepo(
    var key: String = "",
    var name: String? = null,
    var hash: String? = null,
    var groupKey: String = "",
    var permissions: String = "READ_ONLY",
    var createdAt: String? = null
) : SugarRecord(), SerializableMarker {

    companion object {

        fun clear(groupKey: String) {
            val whereClause = "GROUP_KEY = ?"

            deleteAll(SnowbirdRepo::class.java, whereClause, groupKey)
        }

        fun getAll(): List<SnowbirdRepo> {
            return findAll(SnowbirdRepo::class.java).asSequence().toList()
        }

        fun getAllFor(group: SnowbirdGroup?): List<SnowbirdRepo> {
            if (group == null) return emptyList()

            val whereClause = "GROUP_KEY = ?"
            val whereArgs = mutableListOf(group.key)

            return find(
                SnowbirdRepo::class.java, whereClause, whereArgs.toTypedArray(),
                null,
                null,
                null
            )
        }

        fun findByKey(key: String): SnowbirdRepo? {
            val whereClause = "KEY = ?"
            val whereArgs = arrayOf(key)
            return find(
                SnowbirdRepo::class.java,
                whereClause,
                whereArgs,
                null,
                null,
                null
            ).firstOrNull()
        }

        fun getAllForGroupKey(groupKey: String): List<SnowbirdRepo> {
            val whereClause = "GROUP_KEY = ?"
            val whereArgs = arrayOf(groupKey)
            return find(SnowbirdRepo::class.java, whereClause, whereArgs, null, null, null)
        }
    }
}

fun SnowbirdRepo.shortHash(): String {
    return key.take(10)
}