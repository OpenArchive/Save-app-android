package net.opendasharchive.openarchive.features.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import net.opendasharchive.openarchive.db.sugar.Project
import net.opendasharchive.openarchive.db.sugar.Space
import net.opendasharchive.openarchive.features.main.ui.AppRoute
import net.opendasharchive.openarchive.features.main.ui.Navigator

data class FoldersState(
    val folders: List<Project> = emptyList(),
    val isArchived: Boolean = false,
    val showArchivedMenuItem: Boolean = false,
    val archivedCount: Int = 0
)

sealed interface FoldersAction {
    data class FolderClicked(val project: Project) : FoldersAction
    data object ViewArchivedClicked : FoldersAction
    data object RefreshFolders : FoldersAction
}

sealed interface FoldersEvent {
    data class NavigateToFolderDetail(val projectId: Long) : FoldersEvent
    data class NavigateToArchivedFolders(val spaceId: Long) : FoldersEvent
}

class FoldersViewModel(
    private val route: AppRoute.FolderListRoute,
    private val navigator: Navigator,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FoldersState(isArchived = route.showArchived))
    val uiState: StateFlow<FoldersState> = _uiState.asStateFlow()

    private val _events = Channel<FoldersEvent>()
    val events = _events.receiveAsFlow()

    init {
        refreshFolders()
    }

    fun setArchived(archived: Boolean) {

        _uiState.update { it.copy(isArchived = archived) }
        refreshFolders()
    }

    fun onAction(action: FoldersAction) {
        when (action) {
            is FoldersAction.FolderClicked -> navigator.navigateTo(AppRoute.FolderDetailRoute(action.project.id))

            is FoldersAction.ViewArchivedClicked -> navigator.navigateTo(AppRoute.FolderListRoute(showArchived = true, spaceId = route.spaceId))

            is FoldersAction.RefreshFolders -> {
                refreshFolders()
            }
        }
    }

    private fun refreshFolders() {
        val space = Space.current

        val folders = if (_uiState.value.isArchived) {
            space?.archivedProjects ?: emptyList()
        } else {
            space?.projects?.filter { !it.isArchived } ?: emptyList()
        }

        val archivedCount = space?.archivedProjects?.size ?: 0

        _uiState.update {
            it.copy(
                folders = folders,
                archivedCount = archivedCount,
                showArchivedMenuItem = !_uiState.value.isArchived && archivedCount > 0
            )
        }
    }
}
