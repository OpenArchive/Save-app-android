package net.opendasharchive.openarchive.features.main.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.db.Collection
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.db.Space

interface SpaceRepository {
    suspend fun getSpaces(): List<Space>
    suspend fun getCurrentSpace(): Space?
    suspend fun setCurrentSpace(id: Long)

    suspend fun getSpaceById(id: Long): Space?
    suspend fun updateSpace(spaceId: Long, space: Space): Boolean
    suspend fun deleteSpace(id: Long): Boolean
}

interface ProjectRepository {
    suspend fun getProjects(spaceId: Long): List<Project>
    suspend fun getProject(id: Long): Project?
    suspend fun renameProject(id: Long, newName: String)
}

interface CollectionRepository {
    suspend fun getCollections(projectId: Long): List<Collection>
    suspend fun deleteCollection(id: Long)
}

interface MediaRepository {
    suspend fun getMediaForCollection(collectionId: Long): List<Media>
    suspend fun setSelected(mediaId: Long, selected: Boolean)
    suspend fun deleteMedia(mediaId: Long)
}

/**
 * Sugar-backed implementations; keep all ORM calls off the main thread.
 */
class SugarSpaceRepository : SpaceRepository {
    override suspend fun getSpaces(): List<Space> = withContext(Dispatchers.IO) {
        Space.getAll().asSequence().toList()
    }

    override suspend fun getCurrentSpace(): Space? = withContext(Dispatchers.IO) {
        Space.current
    }

    override suspend fun setCurrentSpace(id: Long) {
        withContext(Dispatchers.IO) {
            val space = Space.get(id)
            if (space != null) {
                Space.current = space
            }
        }
    }

    override suspend fun updateSpace(spaceId: Long,space: Space): Boolean = withContext(Dispatchers.IO) {
        space.id = spaceId
        val savedId = space.save()
        return@withContext savedId > 0
    }

    override suspend fun getSpaceById(id: Long): Space? = withContext(Dispatchers.IO){
        Space.get(id)
    }

    override suspend fun deleteSpace(id: Long): Boolean {
        return Space.get(id)?.delete() ?: false
    }
}

class SugarProjectRepository : ProjectRepository {
    override suspend fun getProjects(spaceId: Long): List<Project> = withContext(Dispatchers.IO) {
        Space.get(spaceId)?.projects ?: emptyList()
    }

    override suspend fun getProject(id: Long): Project? = withContext(Dispatchers.IO) {
        Project.getById(id)
    }

    override suspend fun renameProject(id: Long, newName: String) {
        withContext(Dispatchers.IO) {
            Project.getById(id)?.let {
                it.description = newName
                it.save()
            }
        }
    }
}

class SugarCollectionRepository : CollectionRepository {
    override suspend fun getCollections(projectId: Long): List<Collection> = withContext(Dispatchers.IO) {
        Collection.getByProject(projectId)
    }

    override suspend fun deleteCollection(id: Long) {
        withContext(Dispatchers.IO) {
            Collection.get(id)?.delete()
        }
    }
}

class SugarMediaRepository : MediaRepository {
    override suspend fun getMediaForCollection(collectionId: Long): List<Media> = withContext(Dispatchers.IO) {
        Collection.get(collectionId)?.media ?: emptyList()
    }

    override suspend fun setSelected(mediaId: Long, selected: Boolean) {
        withContext(Dispatchers.IO) {
            Media.get(mediaId)?.let {
                it.selected = selected
                it.save()
            }
        }
    }

    override suspend fun deleteMedia(mediaId: Long) {
        withContext(Dispatchers.IO) {
            Media.get(mediaId)?.let { media ->
                val collection = media.collection
                if ((collection?.size ?: 0) < 2) {
                    collection?.delete()
                } else {
                    media.delete()
                }
            }
        }
    }
}
