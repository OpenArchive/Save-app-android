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
import net.opendasharchive.openarchive.features.main.ui.HomeEvent.LaunchPicker
import net.opendasharchive.openarchive.features.main.ui.HomeEvent.Navigate
import net.opendasharchive.openarchive.features.main.ui.HomeNavigation.AddNewFolder
import net.opendasharchive.openarchive.features.main.ui.HomeNavigation.ArchivedFolders
import net.opendasharchive.openarchive.features.main.ui.components.HomeBottomTab
import net.opendasharchive.openarchive.features.media.AddMediaType
import net.opendasharchive.openarchive.features.media.camera.CameraConfig
import net.opendasharchive.openarchive.util.Prefs

sealed class HomeNavigation {
    data class PreviewMedia(val spaceId: Long, val projectId: Long) : HomeNavigation()
    data object ProofMode : HomeNavigation()
    data object SpaceList : HomeNavigation()
    data class ArchivedFolders(val spaceId: Long) : HomeNavigation()
    data object Cache : HomeNavigation()
    data object SpaceSetup : HomeNavigation()
    data class AddNewFolder(val spaceId: Long) : HomeNavigation()
    data class Camera(
        val projectId: Long,
        val config: CameraConfig = CameraConfig(
            allowVideoCapture = true,
            allowPhotoCapture = true,
            allowMultipleCapture = false,
            enablePreview = true,
            showFlashToggle = true,
            showGridToggle = true,
            showCameraSwitch = true
        )
    ) : HomeNavigation()
}

/**
 * Activity-scoped state for the Home shell.
 * This is the SINGLE SOURCE OF TRUTH for:
 * - All spaces
 * - Current selected space
 * - All projects in the current space
 * - Currently selected project ID
 * - Pager state
 *
 * MainMediaViewModel should NOT duplicate this data.
 */
data class HomeState(
    val spaces: List<Space> = emptyList(),
    val currentSpace: Space? = null,
    val projects: List<Project> = emptyList(),
    val selectedProjectId: Long? = null,
    val pagerIndex: Int = 0,
    val lastMediaIndex: Int = 0,
    val showContentPicker: Boolean = false,
    val mediaRefreshProjectId: Long? = null,
    val mediaRefreshToken: Long = 0L
)

sealed class HomeAction {
    data object Load : HomeAction()
    data object Reload : HomeAction() // Force reload projects
    data class SelectSpace(val spaceId: Long) : HomeAction()
    data class SelectProject(val projectId: Long?) : HomeAction()
    data class UpdatePager(val page: Int) : HomeAction()
    data object AddClick : HomeAction()
    data object AddLongClick : HomeAction()
    data class TabSelected(val tab: HomeBottomTab) : HomeAction()
    data object ContentPickerDismissed : HomeAction()
    data class ContentPickerPicked(val type: AddMediaType) : HomeAction()
    data class Navigate(val destination: HomeNavigation) : HomeAction()
    data object NavigateToAddNewFolder : HomeAction()
    data object NavigateToArchivedFolders : HomeAction()
    data object NavigateToPreviewMedia : HomeAction()
    data class MediaImported(val projectId: Long) : HomeAction()

    // NEW: Project-level actions that modify the projects list
    data class RenameProject(val projectId: Long, val newName: String) : HomeAction()
    data class ArchiveProject(val projectId: Long) : HomeAction()
    data class DeleteProject(val projectId: Long) : HomeAction()
}

sealed class HomeEvent {
    data class NavigateToProject(val projectId: Long) : HomeEvent()
    data class LaunchPicker(val type: AddMediaType) : HomeEvent() // Launch native picker
    data class Navigate(val destination: HomeNavigation) : HomeEvent()
    data class ShowMessage(val message: String) : HomeEvent()
}

/**
 * IMPROVED HomeViewModel:
 * - Single source of truth for spaces, projects, selected project
 * - Handles ALL project-level mutations (rename, archive, delete)
 * - Exposes helpers to get current project details
 * - Reloads projects list after mutations
 */
class HomeViewModel(
    private val route: AppRoute.HomeRoute,
    private val navigator: Navigator,
    private val spaceRepository: SpaceRepository,
    private val projectRepository: ProjectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeState())
    val uiState: StateFlow<HomeState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<HomeEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        onAction(HomeAction.Load)
    }

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.Load -> loadSpacesAndProjects()
            HomeAction.Reload -> reloadProjects()
            is HomeAction.SelectSpace -> selectSpace(action.spaceId)
            is HomeAction.SelectProject -> selectProject(action.projectId)
            is HomeAction.UpdatePager -> updatePager(action.page)
            HomeAction.AddClick -> handleAddClick()
            HomeAction.AddLongClick -> handleAddLongClick()
            is HomeAction.TabSelected -> handleTabSelected(action.tab)
            HomeAction.ContentPickerDismissed -> {
                _uiState.update { it.copy(showContentPicker = false) }
            }

            is HomeAction.ContentPickerPicked -> {
                _uiState.update { it.copy(showContentPicker = false) }
                emitEvent(LaunchPicker(action.type))
            }

            is HomeAction.Navigate -> {
                emitEvent(Navigate(action.destination))
            }

            HomeAction.NavigateToAddNewFolder -> {
                val spaceId = uiState.value.currentSpace?.id ?: return
                emitEvent(Navigate(AddNewFolder(spaceId)))
            }

            HomeAction.NavigateToArchivedFolders -> {
                val spaceId = uiState.value.currentSpace?.id ?: return
                emitEvent(Navigate(ArchivedFolders(spaceId)))
            }

            HomeAction.NavigateToPreviewMedia -> {
                val projectId = uiState.value.selectedProjectId ?: return
                navigator.navigateTo(AppRoute.PreviewMediaRoute(projectId = projectId))
                //emitEvent(Navigate(HomeNavigation.PreviewMedia(spaceId, projectId)))

                navigator
            }
            is HomeAction.MediaImported -> viewModelScope.launch{
                val spaceId = uiState.value.currentSpace?.id ?: return@launch
                val projectId = uiState.value.selectedProjectId ?: return@launch

                _uiState.update { state ->
                    state.copy(
                        mediaRefreshProjectId = action.projectId,
                        mediaRefreshToken = state.mediaRefreshToken + 1L
                    )
                }

                _uiEvent.emit(HomeEvent.Navigate(destination = HomeNavigation.PreviewMedia(spaceId = spaceId, projectId = projectId)))
            }

            // NEW: Handle project mutations
            is HomeAction.RenameProject -> renameProject(action.projectId, action.newName)
            is HomeAction.ArchiveProject -> archiveProject(action.projectId)
            is HomeAction.DeleteProject -> deleteProject(action.projectId)
        }
    }

    /**
     * Get the currently selected project.
     * Used by UI to display project details without duplicating state.
     */
    fun getSelectedProject(): Project? {
        val selectedId = _uiState.value.selectedProjectId ?: return null
        return _uiState.value.projects.find { it.id == selectedId }
    }

    /**
     * Get project by ID from the current projects list.
     */
    fun getProject(projectId: Long): Project? {
        return _uiState.value.projects.find { it.id == projectId }
    }

    private fun loadSpacesAndProjects() {
        viewModelScope.launch(Dispatchers.IO) {
            val spaces = spaceRepository.getSpaces()
            val currentSpace = spaceRepository.getCurrentSpace() ?: spaces.firstOrNull()
            val projects = currentSpace?.let { projectRepository.getProjects(it.id) } ?: emptyList()
            val selectedProjectId = projects.firstOrNull()?.id

            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        spaces = spaces,
                        currentSpace = currentSpace,
                        projects = projects,
                        selectedProjectId = selectedProjectId,
                        pagerIndex = it.pagerIndex.coerceIn(0, maxOf(projects.size, 1))
                    )
                }
                selectedProjectId?.let { _uiEvent.emit(HomeEvent.NavigateToProject(it)) }
            }
        }
    }

    /**
     * NEW: Reload projects list without changing space.
     * Called after project mutations (rename/archive/delete).
     */
    private fun reloadProjects() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentSpace = _uiState.value.currentSpace ?: return@launch
            val projects = projectRepository.getProjects(currentSpace.id)

            withContext(Dispatchers.Main) {
                val currentSelectedId = _uiState.value.selectedProjectId
                val currentPagerIndex = _uiState.value.pagerIndex

                // If currently selected project was deleted, select first project
                val newSelectedId = if (projects.any { it.id == currentSelectedId }) {
                    currentSelectedId
                } else {
                    projects.firstOrNull()?.id
                }

                // Adjust pager index if needed
                val settingsIndex = maxOf(1, projects.size)
                val newPagerIndex = if (currentPagerIndex >= settingsIndex) {
                    // Was on settings, stay on settings
                    settingsIndex
                } else {
                    // Was on a project page, stay on same index if valid
                    currentPagerIndex.coerceIn(0, maxOf(0, projects.size - 1))
                }

                _uiState.update {
                    it.copy(
                        projects = projects,
                        selectedProjectId = newSelectedId,
                        pagerIndex = newPagerIndex,
                        lastMediaIndex = if (newPagerIndex < settingsIndex) newPagerIndex else it.lastMediaIndex
                    )
                }
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
                _uiState.update {
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
        _uiState.update {
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
        _uiState.update { state ->
            val settingsIndex = settingsIndex(state.projects.size)
            val isMediaPage = page < settingsIndex
            val newSelectedProjectId =
                if (isMediaPage) state.projects.getOrNull(page)?.id else state.selectedProjectId
            val lastMediaIndex = if (isMediaPage) page else state.lastMediaIndex
            if (isMediaPage) Prefs.currentHomePage = page

            val updated = state.copy(
                pagerIndex = page,
                selectedProjectId = newSelectedProjectId,
                lastMediaIndex = lastMediaIndex
            )
            updated
        }

    }

    private fun handleAddClick() {
        val state = _uiState.value
        val settingsIndex = settingsIndex(state.projects.size)
        val isSettings = state.pagerIndex == settingsIndex

        when {
            state.currentSpace == null -> emitEvent(HomeEvent.Navigate(HomeNavigation.SpaceSetup))
            state.projects.isEmpty() || state.selectedProjectId == null -> {
                state.currentSpace.id?.let {
                    emitEvent(HomeEvent.Navigate(HomeNavigation.AddNewFolder(it)))
                }
            }

            isSettings -> {
                // When on settings, navigate back to media page and show picker
                _uiState.update {
                    it.copy(
                        showContentPicker = true,
                        pagerIndex = state.lastMediaIndex.coerceAtMost(settingsIndex - 1)
                    )
                }
            }

            else -> {
                // launch gallery
                viewModelScope.launch {
                    _uiEvent.emit(HomeEvent.LaunchPicker(AddMediaType.GALLERY))
                }
            }
        }
    }

    private fun handleAddLongClick() {
        val state = _uiState.value
        val settingsIndex = settingsIndex(state.projects.size)
        val isSettings = state.pagerIndex == settingsIndex

        when {
            state.currentSpace == null -> emitEvent(HomeEvent.Navigate(HomeNavigation.SpaceSetup))
            state.projects.isEmpty() || state.selectedProjectId == null -> {
                state.currentSpace.id?.let {
                    emitEvent(HomeEvent.Navigate(HomeNavigation.AddNewFolder(it)))
                }
            }

            isSettings -> {
                // When on settings, navigate back to media page and show picker
                _uiState.update {
                    it.copy(
                        showContentPicker = true,
                        pagerIndex = state.lastMediaIndex.coerceAtMost(settingsIndex - 1)
                    )
                }
            }

            else -> {
                // Show content picker sheet
                _uiState.update { it.copy(showContentPicker = true) }
            }
        }
    }

    private fun handleTabSelected(tab: HomeBottomTab) {
        val state = _uiState.value
        val settingsIndex = settingsIndex(state.projects.size)
        when (tab) {
            HomeBottomTab.MEDIA -> _uiState.update {
                it.copy(
                    pagerIndex = state.lastMediaIndex.coerceAtMost(
                        settingsIndex - 1
                    )
                )
            }

            HomeBottomTab.SETTINGS -> _uiState.update { it.copy(pagerIndex = settingsIndex) }
        }
    }

    // NEW: Project mutation methods

    private fun renameProject(projectId: Long, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            projectRepository.renameProject(projectId, newName)

            withContext(Dispatchers.Main) {
                // Update the project in our local list immediately for instant feedback
                _uiState.update { state ->
                    val updatedProjects = state.projects.map { project ->
                        if (project.id == projectId) {
                            project.apply { description = newName }
                        } else {
                            project
                        }
                    }
                    state.copy(projects = updatedProjects)
                }
            }

            // Then reload to ensure consistency
            reloadProjects()
        }
    }

    private fun archiveProject(projectId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            // TODO: Add archiveProject to ProjectRepository interface
            // For now, use direct Sugar ORM call but this should be in repository
            val project = projectRepository.getProject(projectId)
            project?.let {
                it.isArchived = true
                it.save()
            }

            withContext(Dispatchers.Main) {
                emitEvent(HomeEvent.ShowMessage("Folder archived"))
            }

            // Reload projects list (archived projects should be filtered out)
            reloadProjects()
        }
    }

    private fun deleteProject(projectId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            // TODO: Add deleteProject to ProjectRepository interface
            // For now, use direct Sugar ORM call but this should be in repository
            val project = projectRepository.getProject(projectId)
            project?.delete()

            withContext(Dispatchers.Main) {
                emitEvent(HomeEvent.ShowMessage("Folder removed"))
            }

            // Reload projects list
            reloadProjects()
        }
    }

    private fun emitEvent(event: HomeEvent) {
        viewModelScope.launch { _uiEvent.emit(event) }
    }

    private fun settingsIndex(projectCount: Int): Int = maxOf(1, projectCount)
}
