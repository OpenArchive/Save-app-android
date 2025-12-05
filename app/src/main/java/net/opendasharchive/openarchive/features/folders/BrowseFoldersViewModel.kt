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
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.services.webdav.WebDavRepository
import timber.log.Timber
import java.util.Date

data class Folder(val name: String, val modified: Date)

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
    private val webDavRepository: WebDavRepository
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
        val space = Space.current ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            progressBarFlag.value = true

            try {
                val folderList = webDavRepository.getFolders(space)
                val filteredFolders = folderList.filter { !space.hasProject(it.name) }

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
                        error = UiText.DynamicString(e.message ?: "Unknown error")
                    )
                }
                mFolders.value = emptyList()
                progressBarFlag.value = false
            }
        }
    }

    private fun addFolder() {
        val folder = _uiState.value.selectedFolder ?: return
        val space = Space.current ?: return

        // This should not happen. These should have been filtered on display.
        if (space.hasProject(folder.name)) return

        val license = space.license
        val project = Project(folder.name, Date(), space.id, licenseUrl = license)
        project.save()

        viewModelScope.launch {
            _events.send(BrowseFoldersEvent.ShowSuccessDialog(project.id))
        }
    }

    fun navigateBackWithResult(projectId: Long) {
        viewModelScope.launch {
            _events.send(BrowseFoldersEvent.NavigateBackWithResult(projectId))
        }
    }

    // Legacy method for Fragment support
    fun getFiles(space: Space) {
        viewModelScope.launch {
            progressBarFlag.value = true

            try {
                val folderList = webDavRepository.getFolders(space)
                val filteredFolders = folderList.filter { !space.hasProject(it.name) }
                mFolders.value = filteredFolders
                progressBarFlag.value = false
            } catch (e: Throwable) {
                progressBarFlag.value = false
                mFolders.value = arrayListOf()
                Timber.e(e)
            }
        }
    }
}
