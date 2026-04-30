package net.opendasharchive.openarchive.upload

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed class UploadEvent {
    data class Changed(
        val projectId: Long,
        val collectionId: Long,
        val mediaId: Long,
        val progress: Int = -1,
        val isUploaded: Boolean = false
    ) : UploadEvent()

    data class Deleted(
        val projectId: Long,
        val collectionId: Long,
        val mediaId: Long
    ) : UploadEvent()
}

object UploadEventBus {
    private val _events = MutableSharedFlow<UploadEvent>(
        replay = 1,
        extraBufferCapacity = 64
    )
    val events: SharedFlow<UploadEvent> = _events.asSharedFlow()

    fun tryEmit(event: UploadEvent): Boolean = _events.tryEmit(event)

    fun emitChanged(
        projectId: Long,
        collectionId: Long,
        mediaId: Long,
        progress: Int = -1,
        isUploaded: Boolean = false
    ): Boolean = tryEmit(
        UploadEvent.Changed(
            projectId = projectId,
            collectionId = collectionId,
            mediaId = mediaId,
            progress = progress,
            isUploaded = isUploaded
        )
    )

    fun emitDeleted(
        projectId: Long,
        collectionId: Long,
        mediaId: Long
    ): Boolean = tryEmit(
        UploadEvent.Deleted(
            projectId = projectId,
            collectionId = collectionId,
            mediaId = mediaId
        )
    )
}
