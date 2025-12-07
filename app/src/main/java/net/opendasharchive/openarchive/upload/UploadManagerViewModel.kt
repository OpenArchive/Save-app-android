package net.opendasharchive.openarchive.upload

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.db.Media

data class UploadManagerState(
    val mediaList: List<Media> = emptyList(),
    val isLoading: Boolean = false
)

sealed class UploadManagerAction {
    data object Refresh : UploadManagerAction()
    data class UpdateItem(val mediaId: Long, val progress: Int = -1, val isUploaded: Boolean = false) : UploadManagerAction()
    data class RemoveItem(val mediaId: Long) : UploadManagerAction()
    data class DeleteItem(val position: Int) : UploadManagerAction()
    data class RetryItem(val media: Media) : UploadManagerAction()
    data class MoveItem(val fromPosition: Int, val toPosition: Int) : UploadManagerAction()
    data object Close : UploadManagerAction()
}

sealed class UploadManagerEvent {
    data object Close : UploadManagerEvent()
    data class ShowRetryDialog(val media: Media, val position: Int) : UploadManagerEvent()
}

class UploadManagerViewModel(
    application: Application
) : AndroidViewModel(application) {

    companion object {
        private val STATUSES = listOf(Media.Status.Uploading, Media.Status.Queued, Media.Status.Error)
    }

    private val _uiState = MutableStateFlow(UploadManagerState())
    val uiState: StateFlow<UploadManagerState> = _uiState.asStateFlow()

    private val _events = Channel<UploadManagerEvent>()
    val events = _events.receiveAsFlow()

    init {
        refresh()
    }

    fun onAction(action: UploadManagerAction) {
        when (action) {
            is UploadManagerAction.Refresh -> refresh()
            is UploadManagerAction.UpdateItem -> updateItem(action.mediaId, action.progress, action.isUploaded)
            is UploadManagerAction.RemoveItem -> removeItem(action.mediaId)
            is UploadManagerAction.DeleteItem -> deleteItem(action.position)
            is UploadManagerAction.RetryItem -> retryItem(action.media)
            is UploadManagerAction.MoveItem -> moveItem(action.fromPosition, action.toPosition)
            is UploadManagerAction.Close -> close()
        }
    }

    private fun refresh() {
        _uiState.update { currentState ->
            currentState.copy(
                mediaList = Media.getByStatus(STATUSES, Media.ORDER_PRIORITY)
            )
        }
    }

    private fun updateItem(mediaId: Long, progress: Int, isUploaded: Boolean) {
        _uiState.update { currentState ->
            val updatedList = currentState.mediaList.toMutableList()
            val index = updatedList.indexOfFirst { it.id == mediaId }

            if (index >= 0) {
                val item = updatedList[index]
                when {
                    isUploaded -> {
                        item.status = Media.Status.Uploaded.id
                    }
                    progress >= 0 -> {
                        item.uploadPercentage = progress
                        item.status = Media.Status.Uploading.id
                    }
                    else -> {
                        item.status = Media.Status.Queued.id
                    }
                }
                updatedList[index] = item
            }

            currentState.copy(mediaList = updatedList)
        }
    }

    private fun removeItem(mediaId: Long) {
        _uiState.update { currentState ->
            currentState.copy(
                mediaList = currentState.mediaList.filter { it.id != mediaId }
            )
        }
    }

    private fun deleteItem(position: Int) {
        val currentList = _uiState.value.mediaList
        if (position < 0 || position >= currentList.size) return

        val item = currentList[position]
        val collection = item.collection

        // Delete collection along with the item, if the collection would become empty.
        if ((collection?.size ?: 0) < 2) {
            collection?.delete()
        } else {
            item.delete()
        }

        removeItem(item.id)

        // Broadcast the delete action to notify MainMediaFragment
        BroadcastManager.postDelete(getApplication(), item.id)
    }

    private fun retryItem(media: Media) {
        media.apply {
            sStatus = Media.Status.Queued
            uploadPercentage = 0
            statusMessage = ""
            save()
        }

        updateItem(media.id, progress = -1, isUploaded = false)

        // Broadcast the change to notify MainMediaFragment
        BroadcastManager.postChange(getApplication(), media.collectionId, media.id)
    }

    private fun moveItem(fromPosition: Int, toPosition: Int) {
        _uiState.update { currentState ->
            val updatedList = currentState.mediaList.toMutableList()

            if (fromPosition < 0 || fromPosition >= updatedList.size ||
                toPosition < 0 || toPosition >= updatedList.size) {
                return@update currentState
            }

            val movedItem = updatedList.removeAt(fromPosition)
            updatedList.add(toPosition, movedItem)

            // Update priorities
            var priority = updatedList.size
            for (item in updatedList) {
                item.priority = priority--
                item.save()
            }

            currentState.copy(mediaList = updatedList)
        }
    }

    private fun close() {
        viewModelScope.launch {
            _events.send(UploadManagerEvent.Close)
        }
    }

    fun getUploadingCounter(): Int {
        return _uiState.value.mediaList.size
    }
}
