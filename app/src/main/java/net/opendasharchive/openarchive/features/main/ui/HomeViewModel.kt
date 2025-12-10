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
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.main.data.ProjectRepository
import net.opendasharchive.openarchive.features.main.data.SpaceRepository
import net.opendasharchive.openarchive.util.Prefs

/**
 * Activity-scoped state for the Home shell (spaces, projects, pager indices).
 * Media grid/selection state lives in [MainMediaViewModel].
 */
data class HomeState(
    val spaces: List<Space> = emptyList(),
    val currentSpace: Space? = null,
    val projects: List<Project> = emptyList(),
    val selectedProjectId: Long? = null,
    val pagerIndex: Int = 0,
    val lastMediaIndex: Int = 0
)

sealed class HomeAction {
    data object Load : HomeAction()
    data class SelectSpace(val spaceId: Long) : HomeAction()
    data class SelectProject(val projectId: Long?) : HomeAction()
    data class UpdatePager(val page: Int) : HomeAction()
}

sealed class HomeEvent {
    data class NavigateToProject(val projectId: Long) : HomeEvent()
}

class HomeViewModel(
    private val spaceRepository: SpaceRepository,
    private val projectRepository: ProjectRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<HomeEvent>()
    val events = _events.asSharedFlow()

    init {
        onAction(HomeAction.Load)
    }

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.Load -> loadSpacesAndProjects()
            is HomeAction.SelectSpace -> selectSpace(action.spaceId)
            is HomeAction.SelectProject -> selectProject(action.projectId)
            is HomeAction.UpdatePager -> updatePager(action.page)
        }
    }

    private fun loadSpacesAndProjects() {
        viewModelScope.launch(Dispatchers.IO) {
            val spaces = spaceRepository.getSpaces()
            val currentSpace = spaceRepository.getCurrentSpace() ?: spaces.firstOrNull()
            val projects = currentSpace?.let { projectRepository.getProjects(it.id) } ?: emptyList()
            val selectedProjectId = projects.firstOrNull()?.id

            withContext(Dispatchers.Main) {
                _state.update {
                    it.copy(
                        spaces = spaces,
                        currentSpace = currentSpace,
                        projects = projects,
                        selectedProjectId = selectedProjectId,
                        pagerIndex = it.pagerIndex.coerceIn(0, maxOf(projects.size, 1))
                    )
                }
                selectedProjectId?.let { _events.emit(HomeEvent.NavigateToProject(it)) }
            }
        }
    }

    private fun selectSpace(spaceId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val space = spaceRepository.getSpaces().firstOrNull { it.id == spaceId } ?: return@launch
            spaceRepository.setCurrentSpace(space.id)
            val projects = projectRepository.getProjects(space.id)
            val selectedProjectId = projects.firstOrNull()?.id

            withContext(Dispatchers.Main) {
                _state.update {
                    it.copy(
                        currentSpace = space,
                        projects = projects,
                        selectedProjectId = selectedProjectId,
                        pagerIndex = 0,
                        lastMediaIndex = 0
                    )
                }
            }
        }
    }

    private fun selectProject(projectId: Long?) {
        _state.update {
            it.copy(
                selectedProjectId = projectId,
                pagerIndex = resolvePagerIndexForProject(projectId, it.projects),
                lastMediaIndex = resolvePagerIndexForProject(projectId, it.projects)
            )
        }
    }

    private fun resolvePagerIndexForProject(projectId: Long?, projects: List<Project>): Int {
        val idx = projects.indexOfFirst { it.id == projectId }
        return if (idx >= 0) idx else 0
    }

    private fun updatePager(page: Int) {
        _state.update {
            val lastMediaIndex = if (page < (it.projects.size)) page else it.lastMediaIndex
            if (page < it.projects.size) {
                Prefs.currentHomePage = page
            }
            it.copy(pagerIndex = page, lastMediaIndex = lastMediaIndex)
        }
    }
}
