package net.opendasharchive.openarchive.services.snowbird.presentation.file

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.features.main.ui.AppRoute
import net.opendasharchive.openarchive.features.main.ui.Navigator
import net.opendasharchive.openarchive.core.domain.DomainResult
import net.opendasharchive.openarchive.core.domain.Evidence
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.dialog.DialogStateManager
import net.opendasharchive.openarchive.features.core.dialog.showErrorDialog
import net.opendasharchive.openarchive.features.media.AddMediaType
import net.opendasharchive.openarchive.services.snowbird.service.repository.ISnowbirdFileRepository
import net.opendasharchive.openarchive.services.snowbird.util.SnowbirdFileStorage
import net.opendasharchive.openarchive.util.ProcessingTracker
import net.opendasharchive.openarchive.util.trackProcessing

data class SnowbirdFileState(
    val files: List<Evidence> = emptyList(),
    val isLoading: Boolean = false,
    val archiveId: Long = 0,
    val groupKey: String = "",
    val repoKey: String = "",
    val showContentPicker: Boolean = false
)

sealed interface SnowbirdFileAction {
    data class DownloadFile(val evidence: Evidence) : SnowbirdFileAction
    data class UploadFile(val uri: Uri) : SnowbirdFileAction
    data object RefreshFiles : SnowbirdFileAction
    data object ShowContentPicker : SnowbirdFileAction
    data object ContentPickerDismissed : SnowbirdFileAction
    data class OnMediaTypeSelected(val type: AddMediaType) : SnowbirdFileAction
    data object NavigateBack : SnowbirdFileAction
}

sealed interface SnowbirdFileEvent {
    data class FileDownloaded(val uri: Uri) : SnowbirdFileEvent
    data class LaunchPicker(val type: AddMediaType) : SnowbirdFileEvent
}

class SnowbirdFileViewModel(
    private val navigator: Navigator,
    private val route: AppRoute.SnowbirdFileListRoute,
    private val repository: ISnowbirdFileRepository,
    private val fileStorage: SnowbirdFileStorage,
    private val dialogManager: DialogStateManager,
    private val processingTracker: ProcessingTracker = ProcessingTracker()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SnowbirdFileState(
            archiveId = route.archiveId,
            groupKey = route.groupKey,
            repoKey = route.repoKey
        )
    )
    val uiState: StateFlow<SnowbirdFileState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SnowbirdFileEvent>()
    val events = _events.asSharedFlow()

    init {
        observeFiles(route.archiveId)
        fetchFiles()
    }

    fun onAction(action: SnowbirdFileAction) {
        when (action) {
            is SnowbirdFileAction.DownloadFile -> downloadFile(action.evidence)
            is SnowbirdFileAction.UploadFile -> uploadFile(action.uri)
            is SnowbirdFileAction.RefreshFiles -> fetchFiles(forceRefresh = true)
            is SnowbirdFileAction.ShowContentPicker -> {
                _uiState.update { it.copy(showContentPicker = true) }
            }
            is SnowbirdFileAction.ContentPickerDismissed -> {
                _uiState.update { it.copy(showContentPicker = false) }
            }
            is SnowbirdFileAction.OnMediaTypeSelected -> {
                _uiState.update { it.copy(showContentPicker = false) }
                viewModelScope.launch {
                    _events.emit(SnowbirdFileEvent.LaunchPicker(action.type))
                }
            }
            is SnowbirdFileAction.NavigateBack -> {
                navigator.navigateBack()
            }
        }
    }

    private fun observeFiles(archiveId: Long) {
        repository.observeFiles(archiveId)
            .onEach { files ->
                _uiState.update { it.copy(files = files) }
            }
            .launchIn(viewModelScope)
    }

    private fun fetchFiles(forceRefresh: Boolean = false) {
        val state = _uiState.value
        if (state.archiveId == 0L || state.groupKey.isBlank() || state.repoKey.isBlank()) return

        viewModelScope.launch {
            processingTracker.trackProcessing("fetch_files") {
                _uiState.update { it.copy(isLoading = true) }
                val result = repository.fetchFiles(state.archiveId, state.groupKey, state.repoKey, forceRefresh)
                _uiState.update { it.copy(isLoading = false) }

                if (result is DomainResult.Error) {
                    dialogManager.showErrorDialog(message = UiText.Dynamic(result.error.friendlyMessage))
                }
            }
        }
    }

    private fun downloadFile(evidence: Evidence) {
        val state = _uiState.value
        val filename = evidence.title
        
        viewModelScope.launch {
            processingTracker.trackProcessing("download_file") {
                _uiState.update { it.copy(isLoading = true) }
                val result = repository.downloadFile(state.groupKey, state.repoKey, filename)
                _uiState.update { it.copy(isLoading = false) }

                when (result) {
                    is DomainResult.Success -> {
                        val internalUri = fileStorage.saveByteArrayToFile(result.data, filename).getOrNull()
                        if (internalUri != null) {
                            fileStorage.saveImageToGallery(result.data, filename)
                            _events.emit(SnowbirdFileEvent.FileDownloaded(internalUri))
                        } else {
                            dialogManager.showErrorDialog(message = UiText.Dynamic("Failed to save file locally"))
                        }
                    }
                    is DomainResult.Error -> {
                        dialogManager.showErrorDialog(message = UiText.Dynamic(result.error.friendlyMessage))
                    }
                }
            }
        }
    }

    private fun uploadFile(uri: Uri) {
        val state = _uiState.value
        viewModelScope.launch {
            processingTracker.trackProcessing("upload_file") {
                _uiState.update { it.copy(isLoading = true) }
                val result = repository.uploadFile(state.groupKey, state.repoKey, uri)
                _uiState.update { it.copy(isLoading = false) }

                if (result is DomainResult.Error) {
                    dialogManager.showErrorDialog(message = UiText.Dynamic(result.error.friendlyMessage))
                } else {
                    fetchFiles(forceRefresh = true)
                }
            }
        }
    }
}