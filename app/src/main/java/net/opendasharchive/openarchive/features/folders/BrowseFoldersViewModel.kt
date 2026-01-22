package net.opendasharchive.openarchive.features.folders

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import net.opendasharchive.openarchive.core.domain.Archive
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.core.repositories.ProjectRepository
import net.opendasharchive.openarchive.core.repositories.SpaceRepository
import net.opendasharchive.openarchive.services.webdav.WebDavRepository
import net.opendasharchive.openarchive.util.DateUtils
import timber.log.Timber

data class Folder(val name: String, val modified: LocalDateTime)

data class BrowseFoldersState(
    val folders: List<Folder> = emptyList(),
    val selectedFolder: Folder? = null,
    val isLoading: Boolean = false,
    val error: UiText? = null
)

sealed interface BrowseFoldersAction {
    data class SelectFolder(val folder: Folder) : BrowseFoldersAction
    data object AddFolder : BrowseFoldersAction
    data object LoadFolders : BrowseFoldersAction
}

sealed interface BrowseFoldersEvent {
    data class NavigateBackWithResult(val projectId: Long) : BrowseFoldersEvent
    data class ShowSuccessDialog(val projectId: Long) : BrowseFoldersEvent
}

class BrowseFoldersViewModel(
    private val webDavRepository: WebDavRepository,
    private val spaceRepository: SpaceRepository,
    private val projectRepository: ProjectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BrowseFoldersState())
    val uiState: StateFlow<BrowseFoldersState> = _uiState.asStateFlow()

    private val _events = Channel<BrowseFoldersEvent>()
    val events = _events.receiveAsFlow()

    // Legacy LiveData for Fragment support
    private val mFolders = MutableLiveData<List<Folder>>()
    val folders: LiveData<List<Folder>> get() = mFolders

    val progressBarFlag = MutableLiveData(false)

    init {
        loadFolders()
    }

    fun onAction(action: BrowseFoldersAction) {
        when (action) {
            is BrowseFoldersAction.SelectFolder -> {
                _uiState.update { it.copy(selectedFolder = action.folder) }
            }

            is BrowseFoldersAction.AddFolder -> {
                addFolder()
            }

            is BrowseFoldersAction.LoadFolders -> {
                loadFolders()
            }
        }
    }

    private fun loadFolders() {
        viewModelScope.launch {
            val space = spaceRepository.getCurrentSpace() ?: return@launch

            _uiState.update { it.copy(isLoading = true, error = null) }
            progressBarFlag.value = true

            try {
                val folderList = webDavRepository.getFolders(space)

                // Filter out folders that are already tracked as projects
                val filteredFolders = folderList.filter { folder ->
                    projectRepository.getProjectByName(space.id, folder.name) == null
                }

                _uiState.update {
                    it.copy(
                        folders = filteredFolders,
                        isLoading = false,
                        error = null
                    )
                }
                mFolders.value = filteredFolders
                progressBarFlag.value = false
            } catch (e: Throwable) {
                Timber.e(e)
                _uiState.update {
                    it.copy(
                        folders = emptyList(),
                        isLoading = false,
                        error = if (e.message != null) {
                            UiText.Dynamic(e.message!!)
                        } else {
                            UiText.Resource(net.opendasharchive.openarchive.R.string.error)
                        }
                    )
                }
                mFolders.value = emptyList()
                progressBarFlag.value = false
            }
        }
    }

    private fun addFolder() {
        viewModelScope.launch {
            val folder = _uiState.value.selectedFolder ?: return@launch
            val space = spaceRepository.getCurrentSpace() ?: return@launch

            if (projectRepository.getProjectByName(space.id, folder.name) != null) return@launch

            val archive = Archive(
                description = folder.name,
                created = DateUtils.nowDateTime,
                vaultId = space.id,
                licenseUrl = space.licenseUrl
            )
            val projectId = projectRepository.addProject(archive)

            _events.send(BrowseFoldersEvent.ShowSuccessDialog(projectId))
        }
    }

    fun navigateBackWithResult(projectId: Long) {
        viewModelScope.launch {
            _events.send(BrowseFoldersEvent.NavigateBackWithResult(projectId))
        }
    }

}
