package net.opendasharchive.openarchive.features.main.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.db.Collection
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.features.main.data.CollectionRepository
import net.opendasharchive.openarchive.features.main.data.MediaRepository

/**
 * Represents one collection section with its media items.
 */
data class CollectionSection(
    val collection: Collection,
    val media: List<Media>
)

/**
 * Folder bar modes matching MainActivity.
 */
enum class FolderBarMode {
    INFO,
    SELECTION,
    EDIT
}

/**
 * UI State for a single project's media screen.
 * This ONLY contains media-specific state.
 * Project info comes from HomeViewModel.
 */
data class MainMediaState(
    val projectId: Long? = null,
    val sections: List<CollectionSection> = emptyList(),
    val isInSelectionMode: Boolean = false,
    val selectedMediaIds: Set<Long> = emptySet(),
    val isLoading: Boolean = false,
    val folderBarMode: FolderBarMode = FolderBarMode.INFO,
    val totalMediaCount: Int = 0
)

/**
 * User actions scoped to MainMediaScreen.
 */
sealed class MainMediaAction {
    data class LoadProject(val projectId: Long) : MainMediaAction()
    data class Refresh(val projectId: Long) : MainMediaAction()
    data class MediaClicked(val media: Media) : MainMediaAction()
    data class MediaLongPressed(val media: Media) : MainMediaAction()
    data class UpdateMediaItem(
        val collectionId: Long,
        val mediaId: Long,
        val progress: Int,
        val isUploaded: Boolean
    ) : MainMediaAction()

    data object ToggleSelectAll : MainMediaAction()
    data object DeleteSelected : MainMediaAction()
    data object CancelSelection : MainMediaAction()
    data object EditFolderClicked : MainMediaAction()
    data object CancelEditMode : MainMediaAction()
    data class SaveFolderName(val newName: String) : MainMediaAction()
    data object EnterSelectionMode : MainMediaAction()
}

/**
 * Events emitted from ViewModel to UI.
 * IMPROVED: Project-level mutations are emitted as events,
 * not handled directly. HomeViewModel will handle them.
 */
sealed class MainMediaEvent {
    data class NavigateToPreview(val projectId: Long) : MainMediaEvent()
    data object ShowUploadManager : MainMediaEvent()
    data class ShowErrorDialog(val media: Media, val position: Int) : MainMediaEvent()
    data class SelectionModeChanged(val isSelecting: Boolean, val count: Int) : MainMediaEvent()
    data object FocusFolderNameInput : MainMediaEvent()

    // NEW: Project-level mutation requests (handled by HomeViewModel)
    data class RequestProjectRename(val projectId: Long, val newName: String) : MainMediaEvent()
    data class RequestProjectArchive(val projectId: Long) : MainMediaEvent()
    data class RequestProjectDelete(val projectId: Long) : MainMediaEvent()
}

/**
 * IMPROVED MainMediaViewModel:
 * - Only manages media/collection state for its project
 * - Does NOT load project info (gets it from HomeViewModel)
 * - Emits events for project-level mutations instead of handling them directly
 * - Smaller, more focused responsibility
 */
class MainMediaViewModel(
    private val projectId: Long,
    private val collectionRepository: CollectionRepository,
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainMediaState())
    val uiState: StateFlow<MainMediaState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<MainMediaEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        AppLogger.i("MainMediaViewModel initialized for project $projectId")
        if (projectId >= 0) {
            setProject(projectId)
        }
    }

    fun onAction(action: MainMediaAction) {
        when (action) {
            is MainMediaAction.LoadProject -> setProject(action.projectId)
            is MainMediaAction.Refresh -> refreshSections()
            is MainMediaAction.MediaClicked -> handleMediaClick(action.media)
            is MainMediaAction.MediaLongPressed -> handleMediaLongPress(action.media)
            is MainMediaAction.UpdateMediaItem -> updateMediaItem(
                action.collectionId,
                action.mediaId,
                action.progress,
                action.isUploaded
            )

            MainMediaAction.ToggleSelectAll -> toggleSelectAll()
            MainMediaAction.DeleteSelected -> deleteSelected()
            MainMediaAction.CancelSelection -> cancelSelection()
            MainMediaAction.EnterSelectionMode -> enableSelectionMode()
            MainMediaAction.EditFolderClicked -> enterEditMode()
            MainMediaAction.CancelEditMode -> exitEditMode()
            is MainMediaAction.SaveFolderName -> requestSaveFolderName(action.newName)
        }
    }

    private fun setProject(projectId: Long) {
        _uiState.update {
            it.copy(
                projectId = projectId,
                isInSelectionMode = false,
                selectedMediaIds = emptySet()
            )
        }
        refreshSections()
    }

    fun refreshSections() {
        viewModelScope.launch(Dispatchers.IO) {
            val projectId = uiState.value.projectId ?: return@launch

            val collections = collectionRepository.getCollections(projectId)
            val newSections = collections
                .map { collection ->
                    val media = mediaRepository.getMediaForCollection(collection.id)
                    collection to media
                }
                .filter { (_, media) -> media.isNotEmpty() }
                .map { (collection, media) -> CollectionSection(collection, media) }

            val totalCount = newSections.sumOf { it.media.size }

            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        sections = newSections,
                        totalMediaCount = totalCount
                    )
                }
            }
        }
    }

    private fun handleMediaClick(media: Media) {
        viewModelScope.launch {
            if (_uiState.value.isInSelectionMode) {
                toggleMediaSelection(media)
            } else {
                when (media.sStatus) {
                    Media.Status.New,
                    Media.Status.Local -> _uiEvent.emit(MainMediaEvent.NavigateToPreview(media.projectId))
                    Media.Status.Queued,
                    Media.Status.Uploading -> _uiEvent.emit(MainMediaEvent.ShowUploadManager)
                    Media.Status.Error -> {
                        val position = findMediaPosition(media)
                        _uiEvent.emit(MainMediaEvent.ShowErrorDialog(media, position))
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun handleMediaLongPress(media: Media) {
        viewModelScope.launch {
            if (!_uiState.value.isInSelectionMode) {
                _uiState.update { it.copy(isInSelectionMode = true, folderBarMode = FolderBarMode.SELECTION) }
            }
            toggleMediaSelection(media)
        }
    }

    private fun toggleMediaSelection(media: Media) {
        val currentSelected = _uiState.value.selectedMediaIds
        val newSelected = if (currentSelected.contains(media.id)) {
            currentSelected - media.id
        } else {
            currentSelected + media.id
        }

        viewModelScope.launch(Dispatchers.IO) {
            mediaRepository.setSelected(media.id, newSelected.contains(media.id))

            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(selectedMediaIds = newSelected) }

                if (newSelected.isEmpty() && _uiState.value.isInSelectionMode) {
                    _uiState.update { it.copy(isInSelectionMode = false, folderBarMode = FolderBarMode.INFO) }
                }

                _uiEvent.emit(
                    MainMediaEvent.SelectionModeChanged(
                        _uiState.value.isInSelectionMode,
                        newSelected.size
                    )
                )
            }
        }
    }

    private fun toggleSelectAll() {
        viewModelScope.launch(Dispatchers.IO) {
            val allMediaIds = _uiState.value.sections.flatMap { it.media }.map { it.id }.toSet()
            val currentSelected = _uiState.value.selectedMediaIds

            val newSelected = if (currentSelected.size == allMediaIds.size) emptySet() else allMediaIds

            _uiState.value.sections.flatMap { it.media }.forEach { media ->
                mediaRepository.setSelected(media.id, newSelected.contains(media.id))
            }

            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(selectedMediaIds = newSelected) }
                _uiEvent.emit(
                    MainMediaEvent.SelectionModeChanged(
                        _uiState.value.isInSelectionMode,
                        newSelected.size
                    )
                )
            }
        }
    }

    private fun deleteSelected() {
        viewModelScope.launch(Dispatchers.IO) {
            val selectedIds = _uiState.value.selectedMediaIds
            selectedIds.forEach { mediaId -> mediaRepository.deleteMedia(mediaId) }
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        selectedMediaIds = emptySet(),
                        isInSelectionMode = false
                    )
                }
            }
            refreshSections()
            _uiEvent.emit(MainMediaEvent.SelectionModeChanged(false, 0))
        }
    }

    fun cancelSelection() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value.sections.flatMap { it.media }.forEach { media ->
                if (media.selected) {
                    mediaRepository.setSelected(media.id, false)
                }
            }

            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        selectedMediaIds = emptySet(),
                        isInSelectionMode = false,
                        folderBarMode = FolderBarMode.INFO
                    )
                }
                _uiEvent.emit(MainMediaEvent.SelectionModeChanged(false, 0))
            }
        }
    }

    private fun updateMediaItem(
        collectionId: Long,
        mediaId: Long,
        progress: Int,
        isUploaded: Boolean
    ) {
        viewModelScope.launch {
            AppLogger.i("updateMediaItem: collectionId=$collectionId, mediaId=$mediaId, progress=$progress, isUploaded=$isUploaded")

            _uiState.update { state ->
                val updatedSections = state.sections.map { section ->
                    if (section.collection.id == collectionId) {
                        val updatedMedia = section.media.map { media ->
                            if (media.id == mediaId) {
                                when {
                                    isUploaded -> {
                                        media.status = Media.Status.Uploaded.id
                                        media
                                    }

                                    progress >= 0 -> {
                                        media.uploadPercentage = progress
                                        media.status = Media.Status.Uploading.id
                                        media
                                    }

                                    else -> {
                                        media.status = Media.Status.Queued.id
                                        media
                                    }
                                }
                            } else {
                                media
                            }
                        }
                        section.copy(media = updatedMedia)
                    } else {
                        section
                    }
                }
                state.copy(sections = updatedSections)
            }
        }
    }

    private fun findMediaPosition(media: Media): Int {
        var position = 0
        for (section in _uiState.value.sections) {
            val index = section.media.indexOfFirst { it.id == media.id }
            if (index != -1) {
                return position + index
            }
            position += section.media.size
        }
        return -1
    }

    fun enableSelectionMode() {
        viewModelScope.launch {
            _uiState.update { it.copy(isInSelectionMode = true, folderBarMode = FolderBarMode.SELECTION) }
            _uiEvent.emit(MainMediaEvent.SelectionModeChanged(true, _uiState.value.selectedMediaIds.size))
        }
    }

    fun getSelectedCount(): Int = _uiState.value.selectedMediaIds.size

    private fun enterEditMode() {
        viewModelScope.launch {
            _uiState.update { it.copy(folderBarMode = FolderBarMode.EDIT) }
            _uiEvent.emit(MainMediaEvent.FocusFolderNameInput)
        }
    }

    private fun exitEditMode() {
        viewModelScope.launch {
            _uiState.update { it.copy(folderBarMode = FolderBarMode.INFO) }
        }
    }

    /**
     * IMPROVED: Instead of calling repository directly,
     * emit an event for HomeViewModel to handle.
     */
    private fun requestSaveFolderName(newName: String) {
        viewModelScope.launch {
            val projectId = _uiState.value.projectId ?: return@launch

            // Exit edit mode immediately for responsive UI
            _uiState.update { it.copy(folderBarMode = FolderBarMode.INFO) }

            // Emit event to HomeViewModel to handle the actual rename
            _uiEvent.emit(MainMediaEvent.RequestProjectRename(projectId, newName))
        }
    }

    /**
     * Request project archive.
     * Called from MainMediaScreen when user selects "Archive" from menu.
     */
    fun requestArchiveProject() {
        viewModelScope.launch {
            val projectId = _uiState.value.projectId ?: return@launch
            _uiEvent.emit(MainMediaEvent.RequestProjectArchive(projectId))
        }
    }

    /**
     * Request project deletion.
     * Called from MainMediaScreen when user confirms "Remove" from menu.
     */
    fun requestDeleteProject() {
        viewModelScope.launch {
            val projectId = _uiState.value.projectId ?: return@launch
            _uiEvent.emit(MainMediaEvent.RequestProjectDelete(projectId))
        }
    }
}
