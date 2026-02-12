package net.opendasharchive.openarchive.core.repositories

import com.orm.SugarRecord
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.core.domain.Evidence
import net.opendasharchive.openarchive.core.domain.mappers.toDomain
import net.opendasharchive.openarchive.core.domain.mappers.toEntity
import net.opendasharchive.openarchive.db.sugar.Collection
import net.opendasharchive.openarchive.db.sugar.Media

class SugarMediaRepository(private val io: CoroutineDispatcher = Dispatchers.IO) : MediaRepository {

    override suspend fun getMediaForCollection(collectionId: Long): List<Evidence> =
        withContext(io) {
            Collection.get(collectionId)?.media?.map { it.toDomain() } ?: emptyList()
        }

    override fun observeMediaForCollection(collectionId: Long): Flow<List<Evidence>> = InvalidationBus.media
        .map { getMediaForCollection(collectionId) }
        .distinctUntilChanged()

    override suspend fun getMediaForProject(projectId: Long): List<Evidence> = withContext(io) {
        SugarRecord.find(
            Media::class.java, "project_id = ?",
            arrayOf(projectId.toString()), null, "id DESC", null
        )
            .map { it.toDomain() }
    }

    override fun observeMediaForProject(projectId: Long): Flow<List<Evidence>> = InvalidationBus.media
        .map { getMediaForProject(projectId) }
        .distinctUntilChanged()

    override suspend fun getLocalMedia(): List<Evidence> = withContext(io) {
        Media.getByStatus(listOf(Media.Status.Local), Media.ORDER_CREATED)
            .map { it.toDomain() }
    }

    override fun observeLocalMedia(): Flow<List<Evidence>> = InvalidationBus.media
        .map { getLocalMedia() }
        .distinctUntilChanged()

    override suspend fun getEvidence(id: Long): Evidence? = withContext(io) {
        Media.get(id)?.toDomain()
    }

    override suspend fun setSelected(mediaId: Long, selected: Boolean) {
        withContext(io) {
            Media.get(mediaId)?.let {
                it.selected = selected
                it.save()
                InvalidationBus.invalidateMedia()
            }
        }
    }

    override suspend fun deleteMedia(mediaId: Long) {
        withContext(io) {
            Media.get(mediaId)?.let { media ->
                val collection = media.collection
                if ((collection?.size ?: 0) < 2) {
                    collection?.delete()
                } else {
                    media.delete()
                }
                InvalidationBus.invalidateMedia()
            }
        }
    }

    override suspend fun addEvidence(evidence: Evidence): Long = withContext(io) {
        val id = evidence.toEntity().save()
        if (id > 0) {
            InvalidationBus.invalidateMedia()
            InvalidationBus.invalidateCollections()
        }
        id
    }

    override suspend fun updateEvidence(evidence: Evidence) {
        withContext(io) {
            // Check if still exists before saving to avoid re-inserting deleted items
            if (Media.get(evidence.id) != null) {
                evidence.toEntity().save()
                InvalidationBus.invalidateMedia()
            }
        }
    }

    override suspend fun queueAllForUpload(mediaIds: List<Long>) {
        withContext(io) {
            mediaIds.forEach { id ->
                Media.get(id)?.let {
                    it.sStatus = Media.Status.Queued
                    it.selected = false
                    it.save()
                }
            }
            InvalidationBus.invalidateMedia()
        }
    }

    override suspend fun getQueue(): List<Evidence> = withContext(io) {
        val statuses = listOf(Media.Status.Uploading, Media.Status.Queued, Media.Status.Error)
        Media.getByStatus(statuses, Media.ORDER_PRIORITY).map { it.toDomain() }
    }

    override suspend fun updatePriority(mediaId: Long, priority: Int) {
        withContext(io) {
            Media.get(mediaId)?.let {
                it.priority = priority
                it.save()
                InvalidationBus.invalidateMedia()
            }
        }
    }

    override suspend fun retryMedia(mediaId: Long) {
        withContext(io) {
            Media.get(mediaId)?.let {
                it.sStatus = Media.Status.Queued
                it.uploadPercentage = 0
                it.statusMessage = ""
                it.save()
                InvalidationBus.invalidateMedia()
            }
        }
    }
}