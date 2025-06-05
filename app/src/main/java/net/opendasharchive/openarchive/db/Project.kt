package net.opendasharchive.openarchive.db

import com.orm.SugarRecord
import java.util.Date

/**
 * Project - aka Folder
 * Each Space can have many folders created by the user.
 *
 * @property description
 * @property created
 * @property spaceId
 * @property archived
 * @property openCollectionId
 * @property licenseUrl
 * @constructor Create empty Project
 */
data class Project(
    var description: String? = null,
    var created: Date? = null,
    var spaceId: Long? = null,
    private var archived: Boolean = false,
    private var openCollectionId: Long = -1,
    var licenseUrl: String? = null
) : SugarRecord() {

    companion object {

        fun getById(projectId: Long?): Project? {
            @Suppress("NAME_SHADOWING")
            val projectId = projectId ?: return null

            return findById(Project::class.java, projectId)
        }
    }

    var isArchived: Boolean
        get() = archived
        set(value) {
              archived = value

            // When the space has a license, that needs to be applied when de-archived.
            // Otherwise the wrong license setting might get transmitted to the server.
            if (!archived) {
                val sl = space?.license

                if (!sl.isNullOrBlank()) licenseUrl = sl
            }
        }

    val isUploading
        get() = collections.any { it.isUploading }

    val collections: List<Collection>
        get() = find(Collection::class.java, "project_id = ?", id.toString())

    val openCollection: Collection
        get() {
            var collection = findById(Collection::class.java, openCollectionId)

            if (collection == null || collection.uploadDate != null) {
                collection = Collection(projectId = id)
                collection.save()

                openCollectionId = collection.id
                save()
            }

            return collection
        }

    override fun delete(): Boolean {
        collections.forEach {
            it.delete()
        }

        return super.delete()
    }

    val space: Space?
        get() = findById(Space::class.java, spaceId)

}