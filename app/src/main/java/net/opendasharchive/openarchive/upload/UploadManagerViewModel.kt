package net.opendasharchive.openarchive.upload

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.core.domain.Evidence
import net.opendasharchive.openarchive.core.domain.EvidenceStatus
import net.opendasharchive.openarchive.core.repositories.MediaRepository

data class UploadManagerState(
    val mediaList: List<Evidence> = emptyList(),
    val isLoading: Boolean = false
)

sealed class UploadManagerAction {
    data object Refresh : UploadManagerAction()
    data class UpdateItem(val mediaId: Long, val progress: Int = -1, val isUploaded: Boolean = false) :
        UploadManagerAction()

    data class RemoveItem(val mediaId: Long) : UploadManagerAction()
    data class DeleteItem(val position: Int) : UploadManagerAction()
    data class RetryItem(val evidence: Evidence) : UploadManagerAction()
    data class MoveItem(val fromPosition: Int, val toPosition: Int) : UploadManagerAction()
    data object Close : UploadManagerAction()
}

sealed class UploadManagerEvent {
    data object Close : UploadManagerEvent()
    data class ShowRetryDialog(val evidence: Evidence, val position: Int) : UploadManagerEvent()
}

class UploadManagerViewModel(
    private val application: Application,
    private val mediaRepository: MediaRepository
) : ViewModel() {

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
            is UploadManagerAction.RetryItem -> retryItem(action.evidence)
            is UploadManagerAction.MoveItem -> moveItem(action.fromPosition, action.toPosition)
            is UploadManagerAction.Close -> close()
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _uiState.update { currentState ->
                currentState.copy(
                    mediaList = mediaRepository.getQueue()
                )
            }
        }
    }

    private fun updateItem(mediaId: Long, progress: Int, isUploaded: Boolean) {
        _uiState.update { currentState ->
            val updatedList = currentState.mediaList.toMutableList()
            val index = updatedList.indexOfFirst { it.id == mediaId }

            if (index >= 0) {
                val item = updatedList[index]
                val updatedItem = when {
                    isUploaded -> {
                        item.copy(status = EvidenceStatus.UPLOADED)
                    }

                    progress >= 0 -> {
                        item.copy(uploadPercentage = progress, status = EvidenceStatus.UPLOADING)
                    }

                    else -> {
                        item.copy(status = EvidenceStatus.QUEUED)
                    }
                }
                updatedList[index] = updatedItem
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
        viewModelScope.launch {
            val currentList = _uiState.value.mediaList
            if (position < 0 || position >= currentList.size) return@launch

            val item = currentList[position]

            // Delete the media item (repository handles collection deletion if empty)
            mediaRepository.deleteMedia(item.id)

            removeItem(item.id)

            // Broadcast the delete action to notify MainMediaFragment
            BroadcastManager.postDelete(application, item.id)
            UploadEventBus.emitDeleted(
                projectId = item.archiveId,
                collectionId = item.submissionId,
                mediaId = item.id
            )
        }
    }

    private fun retryItem(evidence: Evidence) {
        viewModelScope.launch {
            mediaRepository.retryMedia(evidence.id)

            updateItem(evidence.id, progress = -1, isUploaded = false)

            // Broadcast the change to notify MainMediaFragment
            BroadcastManager.postChange(application, evidence.submissionId, evidence.id)
            UploadEventBus.emitChanged(
                projectId = evidence.archiveId,
                collectionId = evidence.submissionId,
                mediaId = evidence.id,
                progress = -1,
                isUploaded = false
            )
        }
    }

    private fun moveItem(fromPosition: Int, toPosition: Int) {
        viewModelScope.launch {
            val updatedList = _uiState.value.mediaList.toMutableList()

            if (fromPosition < 0 || fromPosition >= updatedList.size ||
                toPosition < 0 || toPosition >= updatedList.size
            ) {
                return@launch
            }

            val movedItem = updatedList.removeAt(fromPosition)
            updatedList.add(toPosition, movedItem)

            // Update priorities
            var priority = updatedList.size
            for (item in updatedList) {
                mediaRepository.updatePriority(item.id, priority--)
            }

            _uiState.update { it.copy(mediaList = updatedList) }
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
