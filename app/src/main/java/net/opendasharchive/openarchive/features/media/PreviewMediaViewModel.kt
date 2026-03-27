package net.opendasharchive.openarchive.features.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.domain.Archive
import net.opendasharchive.openarchive.core.domain.Evidence
import net.opendasharchive.openarchive.features.core.UiColor
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.asUiImage
import net.opendasharchive.openarchive.features.core.asUiText
import net.opendasharchive.openarchive.features.core.dialog.DialogStateManager
import net.opendasharchive.openarchive.features.core.dialog.DialogType
import net.opendasharchive.openarchive.features.core.dialog.showDialog
import net.opendasharchive.openarchive.core.repositories.MediaRepository
import net.opendasharchive.openarchive.core.repositories.ProjectRepository
import net.opendasharchive.openarchive.features.main.ui.AppRoute
import net.opendasharchive.openarchive.features.main.ui.Navigator
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.upload.UploadJobScheduler

data class PreviewMediaState(
    val mediaList: List<Evidence> = emptyList(),
    val isLoading: Boolean = true,
    val selectionCount: Int = 0,
    val showAddMore: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val showContentPicker: Boolean = false,
    val selectedProjectId: Long? = null,
    val selectedProject: Archive? = null
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
    data object ContentPickerDismissed : PreviewMediaAction()
    data class ContentPickerPicked(val type: AddMediaType) : PreviewMediaAction()
}

sealed class PreviewMediaEvent {
    data class LaunchPicker(val type: AddMediaType) : PreviewMediaEvent()
}

class PreviewMediaViewModel(
    private val route: AppRoute.PreviewMediaRoute,
    private val navigator: Navigator,
    private val dialogManager: DialogStateManager,
    private val projectRepository: ProjectRepository,
    private val mediaRepository: MediaRepository,
    private val uploadJobScheduler: UploadJobScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(PreviewMediaState())
    val uiState: StateFlow<PreviewMediaState> = _uiState.asStateFlow()

    // Expose navigator for content picker launchers
    internal fun getNavigator(): Navigator = navigator

    private val _uiEvent = Channel<PreviewMediaEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()
    
    init {
        observeData()
    }

    fun onAction(action: PreviewMediaAction) {
        when (action) {
            is PreviewMediaAction.MediaClicked -> handleMediaClicked(action.mediaId)
            is PreviewMediaAction.MediaLongPressed -> toggleSelection(action.mediaId)
            PreviewMediaAction.ToggleSelectAll -> handleToggleSelectAll()
            PreviewMediaAction.RemoveSelected -> handleRemoveSelected()
            PreviewMediaAction.UploadAll -> invokeUpload()
            PreviewMediaAction.BatchEdit -> handleBatchEdit()
            PreviewMediaAction.AddMore -> emitEvent(PreviewMediaEvent.LaunchPicker(AddMediaType.GALLERY))
            PreviewMediaAction.ShowAddMenu -> _uiState.update { it.copy(showContentPicker = true) }
            PreviewMediaAction.Refresh -> { /* Flow handles this automatically */ }
            PreviewMediaAction.ContentPickerDismissed -> _uiState.update { it.copy(showContentPicker = false) }
            is PreviewMediaAction.ContentPickerPicked -> {
                _uiState.update { it.copy(showContentPicker = false) }
                emitEvent(PreviewMediaEvent.LaunchPicker(action.type))
            }
        }
    }

    private fun observeData() {
        val projectId = route.projectId

        combine(
            projectRepository.observeProject(projectId),
            mediaRepository.observeLocalMedia(),
            _uiState.map { it.selectedIds }.distinctUntilChanged()
        ) { project, localMedia, selectedIds ->
            
            _uiState.update { state ->
                state.copy(
                    mediaList = localMedia.map { it.copy(isSelected = selectedIds.contains(it.id)) },
                    isLoading = false,
                    selectionCount = selectedIds.size,
                    showAddMore = project != null,
                    selectedProjectId = projectId,
                    selectedProject = project
                )
            }

            // Legacy-like behavior: auto-finish if we previously had media but now it's empty
            if (localMedia.isEmpty() && !_uiState.value.isLoading) {
                 navigator.navigateBack()
            }
            
            // Initial cleanup of lingering selections in the DB (kept for safety)
            if (localMedia.any { it.isSelected }) {
                viewModelScope.launch {
                    localMedia.filter { it.isSelected }.forEach {
                        mediaRepository.setSelected(it.id, false)
                    }
                }
            }
        }.launchIn(viewModelScope)

        showFirstTimeBatch()
    }

    private fun handleMediaClicked(mediaId: Long) {
        val currentState = _uiState.value
        val media = currentState.mediaList.firstOrNull { it.id == mediaId } ?: return

        if (currentState.isInSelectionMode) {
            toggleSelection(media.id)
        } else {

            viewModelScope.launch {
                navigator.navigateTo(
                    AppRoute.ReviewMediaRoute(
                        mediaIds = currentState.mediaList.mapNotNull { it.id }.toLongArray(),
                        selectedIdx = currentState.mediaList.indexOf(media),
                        batchMode = false
                    )
                )
            }
        }
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

            navigator.navigateTo(
                AppRoute.ReviewMediaRoute(
                    mediaIds = mediaForReview.mapNotNull { it.id }.toLongArray(),
                    selectedIdx = mediaForReview.indexOf(selectedMedia),
                    batchMode = batchMode
                )
            )
        }
    }

    private fun handleRemoveSelected() {
        viewModelScope.launch {
            val selectedIds = _uiState.value.selectedIds.toList()
            _uiState.update { it.copy(selectedIds = emptySet()) } // Clear selection immediately
            selectedIds.forEach { id ->
                mediaRepository.deleteMedia(id)
            }
        }
    }

    private fun invokeUpload() {

        if (Prefs.dontShowUploadHint) {
            handleUploadAll()
        } else {
            var doNotShowAgain = false
            dialogManager.showDialog(dialogManager.requireResourceProvider()) {
                type = DialogType.Warning
                iconColor = UiColor.Resource(R.color.colorTertiary)
                message = R.string.once_uploaded_you_will_not_be_able_to_edit_media.asUiText()
                showCheckbox = true
                checkboxText = UiText.Resource(R.string.do_not_show_me_this_again)
                onCheckboxChanged = { isChecked ->
                    doNotShowAgain = isChecked
                }
                positiveButton {
                    text = UiText.Resource(R.string.proceed_to_upload)
                    action = {
                        Prefs.dontShowUploadHint = doNotShowAgain
                        handleUploadAll()
                    }
                }
                neutralButton {
                    text = UiText.Resource(R.string.actually_let_me_edit)
                }
            }
        }
    }

    private fun handleUploadAll() {
        viewModelScope.launch {
            val mediaIds = _uiState.value.mediaList.map { it.id }
            mediaRepository.queueAllForUpload(mediaIds)
            uploadJobScheduler.schedule()
            navigator.navigateAndClear(AppRoute.HomeRoute)
        }
    }

    private fun emitEvent(event: PreviewMediaEvent) {
        viewModelScope.launch { _uiEvent.send(event) }
    }

    private fun showFirstTimeBatch() {
        if (Prefs.batchHintShown) return

        dialogManager.showDialog {
            icon = R.drawable.ic_media_new.asUiImage()
            iconColor = UiColor.Resource(R.color.colorTertiary)
            title = R.string.edit_multiple.asUiText()
            message = R.string.press_and_hold_to_select_and_edit_multiple_media.asUiText()
            onDismissAction {
                Prefs.batchHintShown = true
            }
            positiveButton {
                text = UiText.Resource(R.string.lbl_got_it)
                action = {
                    Prefs.batchHintShown = true
                }
            }

        }

    }
}
