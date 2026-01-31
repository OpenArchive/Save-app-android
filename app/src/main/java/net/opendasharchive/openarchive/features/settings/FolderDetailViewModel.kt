package net.opendasharchive.openarchive.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.domain.Archive
import net.opendasharchive.openarchive.core.repositories.ProjectRepository
import net.opendasharchive.openarchive.features.main.ui.AppRoute
import net.opendasharchive.openarchive.features.core.dialog.DialogStateManager
import net.opendasharchive.openarchive.features.core.dialog.DialogType
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.UiImage
import net.opendasharchive.openarchive.features.core.dialog.showDialog
import net.opendasharchive.openarchive.features.main.ui.Navigator

data class FolderDetailState(
    val projectId: Long = -1L,
    val folderName: String = "",
    val isArchived: Boolean = false
)

sealed interface FolderDetailAction {
    data class UpdateFolderName(val name: String) : FolderDetailAction
    data object ArchiveProject : FolderDetailAction
    data object UnarchiveProject : FolderDetailAction
    data object ShowRemoveDialog : FolderDetailAction
}

class FolderDetailViewModel(
    private val route: AppRoute.FolderDetailRoute,
    private val navigator: Navigator,
    private val projectRepository: ProjectRepository,
    private val dialogManager: DialogStateManager
) : ViewModel() {

    private val projectId: Long = route.currentProjectId
    private var archive: Archive? = null

    private val _uiState = MutableStateFlow(FolderDetailState(projectId = projectId))
    val uiState: StateFlow<FolderDetailState> = _uiState.asStateFlow()

    init {
        if (projectId != -1L) {
            loadProject()
        }
    }

    private fun loadProject() {
        viewModelScope.launch {
            archive = projectRepository.getProject(projectId)
            archive?.let { arc ->
                _uiState.update {
                    it.copy(
                        folderName = arc.description ?: "",
                        isArchived = arc.isArchived
                    )
                }
            }
        }
    }

    fun onAction(action: FolderDetailAction) {
        when (action) {
            is FolderDetailAction.UpdateFolderName -> {
                val name = action.name.trim()
                if (name.isNotBlank()) {
                    archive?.let {
                        val updated = it.copy(description = name)
                        viewModelScope.launch {
                            projectRepository.addProject(updated)
                            archive = updated
                            _uiState.update { state -> state.copy(folderName = name) }
                        }
                    }
                }
            }

            is FolderDetailAction.ArchiveProject -> {
                archive?.let {
                    viewModelScope.launch {
                        projectRepository.archiveProject(it.id, isArchived = true)
                        navigator.navigateBack()
                    }
                }
            }

            is FolderDetailAction.UnarchiveProject -> {
                archive?.let {
                    viewModelScope.launch {
                        projectRepository.archiveProject(it.id, isArchived = false)
                        navigator.navigateBack()
                    }
                }
            }

            is FolderDetailAction.ShowRemoveDialog -> {
                showRemoveConfirmDialog()
            }

        }
    }

    private fun removeProject() {
        archive?.let {
            viewModelScope.launch {
                projectRepository.deleteProject(it.id)
                navigator.navigateBack()
            }
        }
    }

    private fun showRemoveConfirmDialog() {
        dialogManager.showDialog {
            type = DialogType.Error
            title = UiText.Resource(R.string.remove_from_app)
            message = UiText.Resource(R.string.action_remove_project)
            icon = UiImage.DrawableResource(R.drawable.ic_trash)
            destructiveButton {
                text = UiText.Resource(R.string.lbl_remove)
                action = { removeProject() }
            }
            neutralButton {
                text = UiText.Resource(R.string.lbl_Cancel)
            }
        }
    }
}
