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
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.db.dummyProjectList
import net.opendasharchive.openarchive.db.dummySpaceList
import net.opendasharchive.openarchive.features.media.AddMediaType

/**
 * Represents one collection section with its media items.
 */
data class CollectionSection(
    val collection: Collection,
    val media: List<Media>
)

/**
 * Folder bar modes matching MainActivity
 */
enum class FolderBarMode {
    INFO,       // Normal mode: shows space icon, folder name, edit button, count
    SELECTION,  // Selection mode: shows close button, "Select Media" text, remove button
    EDIT        // Rename mode: shows close button, text input field
}

/**
 * UI State for MainMediaScreen
 */
data class MainMediaState(
    val spaces: List<Space> = emptyList(),
    val space: Space? = null,
    val projects: List<Project> = emptyList(),
    val project: Project? = null,
    val sections: List<CollectionSection> = emptyList(),
    val isInSelectionMode: Boolean = false,
    val selectedMediaIds: Set<Long> = emptySet(),
    val isLoading: Boolean = false,
    val folderBarMode: FolderBarMode = FolderBarMode.INFO,
    val folderName: String = "",
    val totalMediaCount: Int = 0
)

/**
 * User actions in MainMediaScreen
 */
sealed class MainMediaAction {
    data class UpdateSelectedSpace(val space: Space) : MainMediaAction()
    data class UpdateSelectedProject(val project: Project? = null) : MainMediaAction()
    data class AddMediaClicked(val mediaType: AddMediaType) : MainMediaAction()
    data object NavigateToProofModeSettings : MainMediaAction()
    data object NavigateToSpaceList : MainMediaAction()
    data object NavigateToArchivedFolders : MainMediaAction()

    data class Refresh(val projectId: Long) : MainMediaAction()
    data class MediaClicked(val media: Media) : MainMediaAction()
    data class MediaLongPressed(val media: Media) : MainMediaAction()
    data class UpdateMediaItem(val collectionId: Long, val mediaId: Long, val progress: Int, val isUploaded: Boolean) : MainMediaAction()
    data object ToggleSelectAll : MainMediaAction()
    data object DeleteSelected : MainMediaAction()
    data object CancelSelection : MainMediaAction()
    data object EditFolderClicked : MainMediaAction()
    data object CancelEditMode : MainMediaAction()
    data class SaveFolderName(val newName: String) : MainMediaAction()
    data class ShowFolderOptions(val x: Int, val y: Int) : MainMediaAction()
    data object EnterSelectionMode : MainMediaAction()
}

/**
 * Events emitted from ViewModel to UI
 */
sealed class MainMediaEvent {
    data class NavigateToPreview(val projectId: Long) : MainMediaEvent()
    data object ShowUploadManager : MainMediaEvent()
    data class ShowErrorDialog(val media: Media, val position: Int) : MainMediaEvent()
    data class SelectionModeChanged(val isSelecting: Boolean, val count: Int) : MainMediaEvent()
    data class ShowFolderOptionsPopup(val x: Int, val y: Int) : MainMediaEvent()
    data object ShowDeleteConfirmation : MainMediaEvent()
    data object FocusFolderNameInput : MainMediaEvent()

    data object NavigateToProofModeSettings : MainMediaEvent()
    data object NavigateToSpaceList : MainMediaEvent()
    data object NavigateToArchivedFolders : MainMediaEvent()
}

/**
 * ViewModel for MainMediaScreen
 */
class MainMediaViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MainMediaState(
        spaces = dummySpaceList,
        space = dummySpaceList.first(),
        projects = dummyProjectList,
        project = dummyProjectList.firstOrNull()
    ))
    val uiState: StateFlow<MainMediaState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<MainMediaEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        loadSpacesAndFolders()
    }

    fun onAction(action: MainMediaAction) {
        when (action) {
            is MainMediaAction.Refresh -> refreshSections()
            is MainMediaAction.MediaClicked -> handleMediaClick(action.media)
            is MainMediaAction.MediaLongPressed -> handleMediaLongPress(action.media)
            is MainMediaAction.UpdateMediaItem -> updateMediaItem(
                action.collectionId,
                action.mediaId,
                action.progress,
                action.isUploaded
            )
            is MainMediaAction.ToggleSelectAll -> toggleSelectAll()
            is MainMediaAction.DeleteSelected -> deleteSelected()
            is MainMediaAction.CancelSelection -> cancelSelection()
            is MainMediaAction.EnterSelectionMode -> enableSelectionMode()
            is MainMediaAction.EditFolderClicked -> enterEditMode()
            is MainMediaAction.CancelEditMode -> exitEditMode()
            is MainMediaAction.SaveFolderName -> saveFolderName(action.newName)
            is MainMediaAction.ShowFolderOptions -> showFolderOptions(action.x, action.y)

            is MainMediaAction.AddMediaClicked -> {

            }

            MainMediaAction.NavigateToProofModeSettings -> viewModelScope.launch {
                _uiEvent.emit(MainMediaEvent.NavigateToProofModeSettings)
            }
            MainMediaAction.NavigateToSpaceList -> viewModelScope.launch {
                _uiEvent.emit(MainMediaEvent.NavigateToSpaceList)
            }
            MainMediaAction.NavigateToArchivedFolders -> viewModelScope.launch {
                _uiEvent.emit(MainMediaEvent.NavigateToArchivedFolders)
            }

            is MainMediaAction.UpdateSelectedProject -> {
                _uiState.update { it.copy(project = action.project) }
                refreshSections()
            }

            is MainMediaAction.UpdateSelectedSpace -> {
                val newSpace = action.space
                Space.current = newSpace
                _uiState.update {
                    it.copy(
                        space = newSpace,
                        projects = newSpace.projects,
                        project = newSpace.projects.firstOrNull()
                    )
                }
            }
        }
    }

    private fun loadSpacesAndFolders() = viewModelScope.launch{
        val allSpaces = Space.getAll().asSequence().toList()
        val selectedSpace = Space.current
        val projects = selectedSpace?.projects ?: emptyList()
        val project = projects.firstOrNull()

        _uiState.update {
            it.copy(
                spaces = allSpaces,
                space = selectedSpace,
                projects = projects,
                project = project
            )
        }
    }

    private fun refreshSections() {
        viewModelScope.launch(Dispatchers.IO) {
            val project = uiState.value.project

            val collections = if (project != null) {
                Collection.getByProject(project.id)
            } else {
                emptyList()
            }
            val newSections = collections
                .filter { it.media.isNotEmpty() }
                .map { CollectionSection(it, it.media) }

            val totalCount = newSections.sumOf { it.media.size }

            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(sections = newSections, totalMediaCount = totalCount) }
            }
        }
    }

    private fun handleMediaClick(media: Media) {
        viewModelScope.launch {
            if (_uiState.value.isInSelectionMode) {
                toggleMediaSelection(media)
            } else {
                when (media.sStatus) {
                    Media.Status.Local -> {
                        // Navigate to preview
                        _uiEvent.emit(MainMediaEvent.NavigateToPreview(media.projectId))
                    }
                    Media.Status.Queued, Media.Status.Uploading -> {
                        // Show upload manager
                        _uiEvent.emit(MainMediaEvent.ShowUploadManager)
                    }
                    Media.Status.Error -> {
                        // Show error dialog
                        val position = findMediaPosition(media)
                        _uiEvent.emit(MainMediaEvent.ShowErrorDialog(media, position))
                    }
                    else -> {
                        // No action for other statuses
                    }
                }
            }
        }
    }

    private fun handleMediaLongPress(media: Media) {
        viewModelScope.launch {
            if (!_uiState.value.isInSelectionMode) {
                // Enter selection mode and change folder bar mode
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

        // Update media.selected in database
        viewModelScope.launch(Dispatchers.IO) {
            media.selected = newSelected.contains(media.id)
            media.save()

            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(selectedMediaIds = newSelected) }

                // If no items selected, exit selection mode and revert folder bar to INFO
                if (newSelected.isEmpty() && _uiState.value.isInSelectionMode) {
                    _uiState.update { it.copy(isInSelectionMode = false, folderBarMode = FolderBarMode.INFO) }
                }

                // Emit selection mode change event
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

            val newSelected = if (currentSelected.size == allMediaIds.size) {
                // Deselect all
                emptySet()
            } else {
                // Select all
                allMediaIds
            }

            // Update all media items
            _uiState.value.sections.flatMap { it.media }.forEach { media ->
                media.selected = newSelected.contains(media.id)
                media.save()
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
            val updatedSections = _uiState.value.sections.map { section ->
                val remainingMedia = section.media.filter { media ->
                    if (selectedIds.contains(media.id)) {
                        // Delete media
                        val collection = media.collection
                        if (collection?.size ?: 0 < 2) {
                            collection?.delete()
                        } else {
                            media.delete()
                        }
                        false
                    } else {
                        true
                    }
                }
                section.copy(media = remainingMedia)
            }.filter { it.media.isNotEmpty() }

            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        sections = updatedSections,
                        selectedMediaIds = emptySet(),
                        isInSelectionMode = false
                    )
                }
                _uiEvent.emit(MainMediaEvent.SelectionModeChanged(false, 0))
            }
        }
    }

    fun cancelSelection() {
        viewModelScope.launch(Dispatchers.IO) {
            // Clear selections in database
            _uiState.value.sections.flatMap { it.media }.forEach { media ->
                if (media.selected) {
                    media.selected = false
                    media.save()
                }
            }

            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        selectedMediaIds = emptySet(),
                        isInSelectionMode = false,
                        folderBarMode = FolderBarMode.INFO  // Revert to INFO mode
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

    // Folder bar actions
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

    private fun saveFolderName(newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Get current project from the first collection's projectId
            val currentSections = _uiState.value.sections
            if (currentSections.isNotEmpty()) {
                val projectId = currentSections.first().collection.projectId
                val project = Project.getById(projectId)
                project?.let {
                    it.description = newName
                    it.save()
                }
            }

            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(folderName = newName, folderBarMode = FolderBarMode.INFO) }
            }
        }
    }

    private fun showFolderOptions(x: Int, y: Int) {
        viewModelScope.launch {
            _uiEvent.emit(MainMediaEvent.ShowFolderOptionsPopup(x, y))
        }
    }

    fun setFolderName(name: String) {
        _uiState.update { it.copy(folderName = name) }
    }
}
