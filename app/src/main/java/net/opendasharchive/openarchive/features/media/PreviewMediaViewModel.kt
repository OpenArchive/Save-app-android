package net.opendasharchive.openarchive.features.media

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.util.Prefs

data class PreviewMediaState(
    val mediaList: List<Media> = emptyList(),
    val isLoading: Boolean = true,
    val selectionCount: Int = 0,
    val showAddMore: Boolean = false,
    val selectedIds: Set<Long> = emptySet()
) {
    val isInSelectionMode: Boolean
        get() = selectedIds.isNotEmpty()
}

sealed class PreviewMediaAction {
    data class MediaClicked(val mediaId: Long) : PreviewMediaAction()
    data class MediaLongPressed(val mediaId: Long) : PreviewMediaAction()
    data object ToggleSelectAll : PreviewMediaAction()
    data object RemoveSelected : PreviewMediaAction()
    data object UploadAll : PreviewMediaAction()
    data object BatchEdit : PreviewMediaAction()
    data object AddMore : PreviewMediaAction()
    data object ShowAddMenu : PreviewMediaAction()
    data object Refresh : PreviewMediaAction()
}

sealed class PreviewMediaEvent {
    data class NavigateToReview(
        val media: List<Media>,
        val selected: Media?,
        val batchMode: Boolean
    ) : PreviewMediaEvent()

    data object ShowBatchHint : PreviewMediaEvent()
    data object RequestAddMore : PreviewMediaEvent()
    data object RequestAddMenu : PreviewMediaEvent()
    data object CloseScreen : PreviewMediaEvent()
}

class PreviewMediaViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(PreviewMediaState())
    val uiState: StateFlow<PreviewMediaState> = _uiState.asStateFlow()

    private val _events = Channel<PreviewMediaEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadMedia()
    }

    fun onAction(action: PreviewMediaAction) {
        when (action) {
            is PreviewMediaAction.MediaClicked -> handleMediaClicked(action.mediaId)
            is PreviewMediaAction.MediaLongPressed -> handleToggleSelection(action.mediaId)
            PreviewMediaAction.ToggleSelectAll -> handleToggleSelectAll()
            PreviewMediaAction.RemoveSelected -> handleRemoveSelected()
            PreviewMediaAction.UploadAll -> handleUploadAll()
            PreviewMediaAction.BatchEdit -> handleBatchEdit()
            PreviewMediaAction.AddMore -> viewModelScope.launch { _events.send(PreviewMediaEvent.RequestAddMore) }
            PreviewMediaAction.ShowAddMenu -> viewModelScope.launch { _events.send(PreviewMediaEvent.RequestAddMenu) }
            PreviewMediaAction.Refresh -> loadMedia()
        }
    }

    private fun loadMedia() {
        viewModelScope.launch {
            val projectId = savedStateHandle.get<Long>("project_id") ?: -1L
            val media = withContext(Dispatchers.IO) {
                val items = Media.getByStatus(listOf(Media.Status.Local), Media.ORDER_CREATED)
                items.forEach {
                    if (it.selected) {
                        it.selected = false
                        it.save()
                    }
                }
                items
            }

            _uiState.update {
                it.copy(
                    mediaList = media,
                    isLoading = false,
                    selectionCount = 0,
                    showAddMore = Project.getById(projectId) != null,
                    selectedIds = emptySet()
                )
            }

            if (!Prefs.batchHintShown) {
                _events.send(PreviewMediaEvent.ShowBatchHint)
            }
        }
    }

    private fun handleMediaClicked(mediaId: Long) {
        val currentState = _uiState.value
        val media = currentState.mediaList.firstOrNull { it.id == mediaId } ?: return

        if (currentState.isInSelectionMode) {
            toggleSelection(media.id)
        } else {
            viewModelScope.launch {
                _events.send(
                    PreviewMediaEvent.NavigateToReview(
                        media = currentState.mediaList,
                        selected = media,
                        batchMode = false
                    )
                )
            }
        }
    }

    private fun handleToggleSelection(mediaId: Long) {
        toggleSelection(mediaId)
    }

    private fun toggleSelection(mediaId: Long?) {
        if (mediaId == null) return
        val updatedSelected = _uiState.value.selectedIds.toMutableSet().apply {
            if (contains(mediaId)) remove(mediaId) else add(mediaId)
        }
        val selectionCount = updatedSelected.size

        _uiState.update {
            it.copy(
                mediaList = it.mediaList.toList(),
                selectionCount = selectionCount,
                selectedIds = updatedSelected
            )
        }
    }

    private fun handleToggleSelectAll() {
        val current = _uiState.value
        val allIds = current.mediaList.mapNotNull { it.id }.toSet()
        val shouldSelectAll = current.selectedIds.size != allIds.size

        _uiState.update {
            val newSelection = if (shouldSelectAll) allIds else emptySet()
            it.copy(
                mediaList = it.mediaList.toList(),
                selectionCount = newSelection.size,
                selectedIds = newSelection
            )
        }
    }

    private fun handleBatchEdit() {
        val current = _uiState.value
        val selected = current.mediaList.filter { current.selectedIds.contains(it.id) }

        if (selected.isEmpty()) return

        viewModelScope.launch {
            val batchMode = selected.size > 1
            val mediaForReview = if (batchMode) selected else current.mediaList
            val selectedMedia = if (batchMode) null else selected.firstOrNull()

            _events.send(
                PreviewMediaEvent.NavigateToReview(
                    media = mediaForReview,
                    selected = selectedMedia,
                    batchMode = batchMode
                )
            )
        }
    }

    private fun handleRemoveSelected() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val selectedIds = _uiState.value.selectedIds
                _uiState.value.mediaList.filter { selectedIds.contains(it.id) }.forEach { it.delete() }
            }
            loadMedia()
        }
    }

    private fun handleUploadAll() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                _uiState.value.mediaList.forEach {
                    it.sStatus = Media.Status.Queued
                    it.selected = false
                    it.save()
                }
            }
            _events.send(PreviewMediaEvent.CloseScreen)
        }
    }
}
