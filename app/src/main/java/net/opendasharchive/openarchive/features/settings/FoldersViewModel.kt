package net.opendasharchive.openarchive.features.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.opendasharchive.openarchive.core.domain.Archive
import net.opendasharchive.openarchive.features.main.ui.Navigator
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import net.opendasharchive.openarchive.core.repositories.ProjectRepository
import net.opendasharchive.openarchive.core.repositories.SpaceRepository
import net.opendasharchive.openarchive.features.main.ui.AppRoute

data class FoldersState(
    val folders: List<Archive> = emptyList(),
    val isArchived: Boolean = false,
    val showArchivedMenuItem: Boolean = false,
    val archivedCount: Int = 0
)

sealed interface FoldersAction {
    data class FolderClicked(val archive: Archive) : FoldersAction
    data object ViewArchivedClicked : FoldersAction
}

sealed interface FoldersEvent {
    data class NavigateToFolderDetail(val projectId: Long) : FoldersEvent
    data class NavigateToArchivedFolders(val spaceId: Long) : FoldersEvent
}

class FoldersViewModel(
    private val route: AppRoute.FolderListRoute,
    private val navigator: Navigator,
    private val projectRepository: ProjectRepository,
    private val spaceRepository: SpaceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FoldersState(isArchived = route.showArchived))
    val uiState: StateFlow<FoldersState> = _uiState.asStateFlow()

    private val _events = Channel<FoldersEvent>()
    val events = _events.receiveAsFlow()

    init {
        observeData()
    }

    private fun observeData() {
        val spaceFlow = route.spaceId?.let { spaceRepository.observeSpace(it) }
            ?: spaceRepository.observeCurrentSpace()

        combine(
            spaceFlow,
            _uiState.map { it.isArchived }.distinctUntilChanged()
        ) { space, isArchived ->
            space to isArchived
        }.flatMapLatest { (space, isArchived) ->
            if (space == null) {
                kotlinx.coroutines.flow.flowOf(emptyList<net.opendasharchive.openarchive.core.domain.Archive>() to 0)
            } else {
                combine(
                    projectRepository.observeProjects(space.id, isArchived),
                    projectRepository.observeProjects(space.id, true) // For archived count
                ) { folders, archivedProjects ->
                    folders to archivedProjects.size
                }
            }
        }.onEach { (folders, archivedCount) ->
            _uiState.update {
                it.copy(
                    folders = folders,
                    archivedCount = archivedCount,
                    showArchivedMenuItem = !_uiState.value.isArchived && archivedCount > 0
                )
            }
        }.launchIn(viewModelScope)
    }

    fun onAction(action: FoldersAction) {
        when (action) {
            is FoldersAction.FolderClicked -> navigator.navigateTo(AppRoute.FolderDetailRoute(action.archive.id))

            is FoldersAction.ViewArchivedClicked -> navigator.navigateTo(AppRoute.FolderListRoute(showArchived = true, spaceId = route.spaceId))
        }
    }

}
