package net.opendasharchive.openarchive.features.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.db.Project

data class FolderDetailState(
    val projectId: Long = -1L,
    val folderName: String = "",
    val isArchived: Boolean = false
)

sealed interface FolderDetailAction {
    data class UpdateFolderName(val name: String) : FolderDetailAction
    data object ArchiveProject : FolderDetailAction
    data object UnarchiveProject : FolderDetailAction
    data object RemoveProject : FolderDetailAction
    data object ShowRemoveDialog : FolderDetailAction
}

sealed interface FolderDetailEvent {
    data object NavigateBack : FolderDetailEvent
    data object ShowRemoveConfirmDialog : FolderDetailEvent
}

class FolderDetailViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private var projectId: Long = savedStateHandle.get<Long>("currentProjectId") ?: -1L
    private var project: Project? = null

    private val _uiState = MutableStateFlow(FolderDetailState(projectId = projectId))
    val uiState: StateFlow<FolderDetailState> = _uiState.asStateFlow()

    private val _events = Channel<FolderDetailEvent>()
    val events = _events.receiveAsFlow()

    init {
        if (projectId != -1L) {
            loadProject()
        }
    }

    fun setProjectId(id: Long) {
        projectId = id
        loadProject()
    }

    private fun loadProject() {
        project = Project.getById(projectId)
        project?.let { proj ->
            _uiState.update {
                it.copy(
                    folderName = proj.description ?: "",
                    isArchived = proj.isArchived
                )
            }
        }
    }

    fun onAction(action: FolderDetailAction) {
        when (action) {
            is FolderDetailAction.UpdateFolderName -> {
                val name = action.name.trim()
                if (name.isNotBlank()) {
                    project?.let {
                        it.description = name
                        it.save()
                        _uiState.update { state -> state.copy(folderName = name) }
                    }
                }
            }

            is FolderDetailAction.ArchiveProject -> {
                project?.let {
                    it.isArchived = true
                    it.save()
                    viewModelScope.launch {
                        _events.send(FolderDetailEvent.NavigateBack)
                    }
                }
            }

            is FolderDetailAction.UnarchiveProject -> {
                project?.let {
                    it.isArchived = false
                    it.save()
                    viewModelScope.launch {
                        _events.send(FolderDetailEvent.NavigateBack)
                    }
                }
            }

            is FolderDetailAction.ShowRemoveDialog -> {
                viewModelScope.launch {
                    _events.send(FolderDetailEvent.ShowRemoveConfirmDialog)
                }
            }

            is FolderDetailAction.RemoveProject -> {
                project?.delete()
                viewModelScope.launch {
                    _events.send(FolderDetailEvent.NavigateBack)
                }
            }
        }
    }
}
