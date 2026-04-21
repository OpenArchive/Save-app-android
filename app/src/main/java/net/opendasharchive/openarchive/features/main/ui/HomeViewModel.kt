package net.opendasharchive.openarchive.features.main.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.core.domain.Archive
import net.opendasharchive.openarchive.core.domain.Vault
import net.opendasharchive.openarchive.core.domain.VaultType
import net.opendasharchive.openarchive.core.repositories.ProjectRepository
import net.opendasharchive.openarchive.core.repositories.SpaceRepository
import net.opendasharchive.openarchive.features.main.ui.HomeEvent.LaunchPicker
import net.opendasharchive.openarchive.features.main.ui.components.HomeBottomTab
import net.opendasharchive.openarchive.features.media.AddMediaType
import net.opendasharchive.openarchive.features.media.MediaPicker
import net.opendasharchive.openarchive.features.media.camera.CameraConfig
import net.opendasharchive.openarchive.upload.UploadJobScheduler
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.util.Prefs

/**
 * HomeViewModel handles logic for the home screen including spaces and projects.
 */
class HomeViewModel(
    private val route: AppRoute.HomeRoute,
    private val navigator: Navigator,
    private val spaceRepository: SpaceRepository,
    private val projectRepository: ProjectRepository,
    private val uploadJobScheduler: UploadJobScheduler,
    private val sharedImportState: SharedImportState
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeState())
    val uiState: StateFlow<HomeState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<HomeEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        observeData()
        observeSharedImport()
    }

    private fun observeData() {

        combine(
            spaceRepository.observeSpaces(),
            spaceRepository.observeCurrentSpace(),
            spaceRepository.observeHasDwebSpace(),
        ) { spaces, current, hasDwebEntry ->
            Triple(spaces, current, hasDwebEntry)
        }.flatMapLatest { (spaces, current, hasDwebEntry) ->
            val actualCurrent = current ?: spaces.firstOrNull()
            if (current == null && actualCurrent != null) {
                viewModelScope.launch {
                    spaceRepository.setCurrentSpace(actualCurrent.id)
                }
            }

            val projectsFlow = actualCurrent?.let { projectRepository.observeProjects(it.id) }
                ?: flowOf(emptyList())

            projectsFlow.map { projects ->
                ObservedData(spaces, actualCurrent, projects, hasDwebEntry)
            }
        }.onEach { data ->
            _uiState.update { state ->
                val selectedProjectId =
                    state.selectedProjectId?.takeIf { id -> data.projects.any { it.id == id } }
                        ?: data.projects.firstOrNull()?.id

                val currentSettingsIndex = settingsIndex(state.projects.size)
                val wasOnSettings = state.pagerIndex == currentSettingsIndex

                val newPagerIndex = if (wasOnSettings) {
                    settingsIndex(data.projects.size)
                } else {
                    resolvePagerIndexForProject(selectedProjectId, data.projects)
                }

                state.copy(
                    spaces = data.spaces,
                    hasDwebEntry = data.hasDwebEntry,
                    currentSpace = data.currentSpace,
                    projects = data.projects,
                    selectedProjectId = selectedProjectId,
                    pagerIndex = newPagerIndex,
                    lastMediaIndex = if (newPagerIndex < settingsIndex(data.projects.size)) newPagerIndex else state.lastMediaIndex
                )
            }
        }.launchIn(viewModelScope)
    }

    private fun observeSharedImport() {
        sharedImportState.pendingUris
            .onEach { uris ->
                AppLogger.d("SHARE_DEBUG: HomeViewModel observeSharedImport uris=$uris")
                if (uris != null) {
                    AppLogger.d("SHARE_DEBUG: setting showProjectPickerForImport=true")
                    _uiState.update { it.copy(pendingSharedUris = uris, showProjectPickerForImport = true) }
                    sharedImportState.clear()
                }
            }
            .launchIn(viewModelScope)
    }

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.Load -> Unit // Already observing
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

            is HomeAction.Navigate -> navigator.navigateTo(action.route)
            
            HomeAction.ShowUploadManager -> {
                _uiState.update { it.copy(showUploadManager = true) }
                uploadJobScheduler.cancel()
            }
            HomeAction.HideUploadManager -> {
                _uiState.update { it.copy(showUploadManager = false) }
                // In legacy, it resumes if there are pending uploads.
                // UploadJobScheduler.schedule() usually checks internally, but we can also check here if needed.
                uploadJobScheduler.schedule()
            }

            HomeAction.NavigateToAddNewFolder -> {
                val spaceId = uiState.value.currentSpace?.id ?: return
                navigateToAddFolder(spaceId)
            }

            HomeAction.NavigateToArchivedFolders -> {
                val spaceId = uiState.value.currentSpace?.id
                navigator.navigateTo(AppRoute.FolderListRoute(spaceId = spaceId, showArchived = true))
            }

            HomeAction.NavigateToPreviewMedia -> {
                val projectId = uiState.value.selectedProjectId ?: return
                navigator.navigateTo(AppRoute.PreviewMediaRoute(projectId = projectId))
            }

            is HomeAction.MediaImported -> viewModelScope.launch {
                val spaceId = uiState.value.currentSpace?.id ?: return@launch
                val projectId = uiState.value.selectedProjectId ?: return@launch

                _uiState.update { state ->
                    state.copy(
                        mediaRefreshProjectId = action.projectId,
                        mediaRefreshToken = state.mediaRefreshToken + 1L
                    )
                }

                navigator.navigateTo(AppRoute.PreviewMediaRoute(projectId))
            }

            HomeAction.NavigateToCamera -> viewModelScope.launch {
                val projectId = uiState.value.selectedProjectId ?: return@launch
                val config = CameraConfig(
                    allowVideoCapture = true,
                    allowPhotoCapture = true,
                    allowMultipleCapture = false,
                    enablePreview = true,
                    showFlashToggle = true,
                    showGridToggle = true,
                    showCameraSwitch = true
                )
                val route = AppRoute.CameraRoute(projectId, config)
                navigator.navigateTo(route)
            }

            is HomeAction.StartSharedImport -> {
                _uiState.update { it.copy(
                    pendingSharedUris = action.uris,
                    showProjectPickerForImport = true
                )}
            }

            HomeAction.DismissSharedImportPicker -> {
                _uiState.update { it.copy(
                    pendingSharedUris = null,
                    showProjectPickerForImport = false
                )}
            }

            // NEW: Handle project mutations
            is HomeAction.RenameProject -> renameProject(action.projectId, action.newName)
            is HomeAction.ArchiveProject -> archiveProject(action.projectId)
            is HomeAction.DeleteProject -> deleteProject(action.projectId)
        }
    }

    private fun selectSpace(spaceId: Long) {
        viewModelScope.launch {
            spaceRepository.setCurrentSpace(spaceId)
            // Projects will be reloaded automatically via observeData
            _uiState.update {
                it.copy(
                    pagerIndex = 0,
                    lastMediaIndex = 0
                )
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

    private fun resolvePagerIndexForProject(projectId: Long?, projects: List<Archive>): Int {
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
            state.currentSpace == null -> navigator.navigateTo(AppRoute.SpaceSetupRoute)
            state.projects.isEmpty() || state.selectedProjectId == null -> {
                state.currentSpace.id.let {
                    navigateToAddFolder(it)
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
                    _uiEvent.emit(LaunchPicker(AddMediaType.GALLERY))
                }
            }
        }
    }

    private fun handleAddLongClick() {
        val state = _uiState.value
        val settingsIndex = settingsIndex(state.projects.size)
        val isSettings = state.pagerIndex == settingsIndex

        when {
            state.currentSpace == null -> navigator.navigateTo(AppRoute.SpaceSetupRoute)
            state.projects.isEmpty() || state.selectedProjectId == null -> {
                state.currentSpace.id.let {
                    navigateToAddFolder(it)
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
        viewModelScope.launch {
            projectRepository.renameProject(projectId, newName)
            emitEvent(HomeEvent.ShowMessage("Folder renamed"))
        }
    }

    private fun archiveProject(projectId: Long) {
        viewModelScope.launch {
            projectRepository.getProject(projectId)?.let { project ->
                projectRepository.archiveProject(projectId, !project.isArchived)
            }
            emitEvent(HomeEvent.ShowMessage("Folder archived"))
        }
    }

    private fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            projectRepository.deleteProject(projectId)
            emitEvent(HomeEvent.ShowMessage("Folder removed"))
        }
    }


    private fun emitEvent(event: HomeEvent) {
        viewModelScope.launch { _uiEvent.emit(event) }
    }

    private fun settingsIndex(projectCount: Int): Int = maxOf(1, projectCount)

    private fun navigateToAddFolder(spaceId: Long) {
        if (uiState.value.currentSpace?.type == VaultType.INTERNET_ARCHIVE) {
            navigator.navigateTo(AppRoute.CreateNewFolderRoute)
        } else {
            navigator.navigateTo(AppRoute.AddFolderRoute(spaceId))
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

private data class ObservedData(
    val spaces: List<Vault>,
    val currentSpace: Vault?,
    val projects: List<Archive>,
    val hasDwebEntry: Boolean,
)
