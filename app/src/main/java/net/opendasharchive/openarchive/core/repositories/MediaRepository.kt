package net.opendasharchive.openarchive.core.repositories

import kotlinx.coroutines.flow.Flow
import net.opendasharchive.openarchive.core.domain.Evidence

interface MediaRepository {
    suspend fun getMediaForCollection(collectionId: Long): List<Evidence>
    fun observeMediaForCollection(collectionId: Long): Flow<List<Evidence>>
    suspend fun getMediaForProject(projectId: Long): List<Evidence>
    fun observeMediaForProject(projectId: Long): Flow<List<Evidence>>
    suspend fun getLocalMedia(): List<Evidence>
    fun observeLocalMedia(): Flow<List<Evidence>>
    suspend fun getEvidence(id: Long): Evidence?
    suspend fun setSelected(mediaId: Long, selected: Boolean)
    suspend fun deleteMedia(mediaId: Long)
    suspend fun addEvidence(evidence: Evidence): Long
    suspend fun updateEvidence(evidence: Evidence)
    suspend fun queueAllForUpload(mediaIds: List<Long>)
    suspend fun getQueue(): List<Evidence>
    suspend fun updatePriority(mediaId: Long, priority: Int)
    suspend fun retryMedia(mediaId: Long)
}