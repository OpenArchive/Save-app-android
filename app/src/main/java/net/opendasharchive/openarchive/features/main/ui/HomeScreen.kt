package net.opendasharchive.openarchive.features.main.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.main.ui.components.HomeAppBar
import net.opendasharchive.openarchive.features.main.ui.components.MainBottomBar
import net.opendasharchive.openarchive.features.main.ui.components.MainDrawerContent
import net.opendasharchive.openarchive.features.media.AddMediaType
import net.opendasharchive.openarchive.features.settings.SettingsScreen
import net.opendasharchive.openarchive.util.Prefs
import org.koin.androidx.compose.koinViewModel
import kotlin.math.max

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel(),
    onExit: () -> Unit,
    onNewFolder: () -> Unit,
    onFolderSelected: (Long) -> Unit,
    onAddMedia: (AddMediaType) -> Unit,
    onNavigateToCache: () -> Unit,
    onNavigateToProofModeSettings: () -> Unit
) {

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collectLatest { event ->
            when(event) {
                is HomeScreenEvent.NavigateToProofModeSettings -> {
                    onNavigateToProofModeSettings()
                }
            }
        }
    }

    HomeScreenContent(
        onExit = onExit,
        state = state,
        onAction = viewModel::onAction,
        onNavigateToCache = onNavigateToCache,
        onNewFolder = onNewFolder,
        onAddMedia = onAddMedia
    )


}

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeScreenState())
    val uiState: StateFlow<HomeScreenState> = _uiState.asStateFlow()

    private val _uiEvents = Channel<HomeScreenEvent>()
    val uiEvents: Flow<HomeScreenEvent> = _uiEvents.receiveAsFlow()

    init {
        loadSpacesAndFolders()
    }

    fun onAction(action: HomeScreenAction) {
        when (action) {
            is HomeScreenAction.UpdateSelectedProject -> {
                _uiState.update { it.copy(selectedProject = action.project) }
            }

            is HomeScreenAction.AddMediaClicked -> TODO()

            is HomeScreenAction.NavigateToProofModeSettings -> viewModelScope.launch{
                _uiEvents.send(HomeScreenEvent.NavigateToProofModeSettings)
            }
        }
    }

    private fun loadSpacesAndFolders() {
        viewModelScope.launch {
            val allSpaces = Space.getAll().asSequence().toList()
            val selectedSpace = Space.current
            val projectsForSelectedSpace = selectedSpace?.projects ?: emptyList()

            _uiState.update {
                it.copy(
                    allSpaces = allSpaces,
                    projectsForSelectedSpace = projectsForSelectedSpace,
                    selectedSpace = selectedSpace,
                    selectedProject = projectsForSelectedSpace.firstOrNull()
                )
            }
        }
    }

}

sealed class HomeScreenAction {
    data class UpdateSelectedProject(val project: Project? = null) : HomeScreenAction()
    data class AddMediaClicked(val mediaType: AddMediaType) : HomeScreenAction()
    data object NavigateToProofModeSettings : HomeScreenAction()
}

sealed class HomeScreenEvent {
    data object NavigateToProofModeSettings: HomeScreenEvent()
}

data class HomeScreenState(
    val selectedSpace: Space? = null,
    val selectedProject: Project? = null,
    val allSpaces: List<Space> = emptyList(),
    val projectsForSelectedSpace: List<Project> = emptyList()
)

@Composable
fun HomeScreenContent(
    onExit: () -> Unit,
    state: HomeScreenState,
    onAction: (HomeScreenAction) -> Unit,
    onNavigateToCache: () -> Unit = {},
    onNewFolder: () -> Unit,
    onAddMedia: (AddMediaType) -> Unit
) {

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val projects = state.projectsForSelectedSpace
    val totalPages = max(1, projects.size) + 1

    // Always start at last media page (never settings) for fresh starts
    // For configuration changes, the activity handles restoration
    val initialPage = Prefs.currentHomePage.coerceIn(0, (totalPages - 2).coerceAtLeast(0))
    val pagerState = rememberPagerState(initialPage = initialPage) { totalPages }

    val currentProjectIndex = state.selectedProject?.let { selected ->
        projects.indexOfFirst { it.id == selected.id }.takeIf { it >= 0 } ?: 0
    } ?: 0

    // Save current page ONLY if it's a media page (not settings)
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage < totalPages - 1) {
            Prefs.currentHomePage = pagerState.currentPage
        }
    }

    // Whenever the pager's current page changes and it represents a project page,
    // update the view model's selected project.
    LaunchedEffect(pagerState.currentPage, projects) {
        if (projects.isNotEmpty() && pagerState.currentPage < projects.size) {
            val newlySelectedProject = projects[pagerState.currentPage]
            onAction(HomeScreenAction.UpdateSelectedProject(newlySelectedProject))
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {

        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = true,
            drawerContent = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    MainDrawerContent(
                        selectedSpace = state.selectedSpace,
                        spaceList = state.allSpaces,
                        projects = state.projectsForSelectedSpace,
                        selectedProject = state.selectedProject,
                        onProjectSelected = { project ->
                            // Update selected project and close drawer
                            onAction(HomeScreenAction.UpdateSelectedProject(project))
                            scope.launch {
                                drawerState.close()
                                // Navigate to the project's page
                                val projectIndex = state.projectsForSelectedSpace.indexOf(project)
                                if (projectIndex >= 0) {
                                    pagerState.scrollToPage(projectIndex)
                                }
                            }
                        },
                        onNewFolderClick = {
                            // TODO: Wire up to onNewFolder callback from parent
                            // onNewFolder()
                            scope.launch { drawerState.close() }
                        }
                    )
                }
            }
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {

                Scaffold(
                    topBar = {
                        HomeAppBar(
                            onExit = onExit,
                            isSettings = pagerState.currentPage == (totalPages - 1),
                            openDrawer = {
                                scope.launch {
                                    drawerState.open()
                                }
                            }
                        )
                    },

                    bottomBar = {
                        MainBottomBar(
                            isSettings = pagerState.currentPage == (totalPages - 1),
                            onAddMediaClick = { mediaType ->
                                // TODO: Wire up to onAddMedia callback from parent
                                // onAddMedia(mediaType)
                            },
                            onMyMediaClick = {
                                // When "My Media" is tapped, scroll to the page of the currently selected project.
                                // If no project is selected, default to the first page.
                                val targetPage = if (projects.isEmpty()) 0 else currentProjectIndex
                                if (pagerState.currentPage != targetPage) {
                                    scope.launch { pagerState.scrollToPage(targetPage) }
                                }
                            },
                            onSettingsClick = {
                                // Scroll to the last page if not already there.
                                if (pagerState.currentPage != totalPages - 1) {
                                    scope.launch { pagerState.scrollToPage(totalPages - 1) }
                                }
                            }
                        )
                    },
                    contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)

                ) { paddingValues ->

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize().padding(paddingValues),
                        ) { page ->

                            when (page) {
                                0 -> {
                                    // First page: If no projects, show -1, else show first project's ID
                                    val firstProject = projects.firstOrNull()
                                    MainMediaScreen(
                                        projectId = firstProject?.id ?: -1,
                                        project = firstProject,
                                        space = state.selectedSpace,
                                        onNavigateToPreview = { projectId ->
                                            // TODO: Navigate to PreviewActivity - needs to be passed from parent
                                        },
                                        onShowUploadManager = {
                                            // TODO: Show upload manager - needs to be passed from parent
                                        },
                                        onShowErrorDialog = { media, position ->
                                            // TODO: Show error dialog - needs to be implemented
                                        },
                                        onSelectionModeChanged = { isSelecting, count ->
                                            // TODO: Update selection mode - needs to be implemented
                                        },
                                        onAddServerClick = {
                                            // TODO: Navigate to add server screen
                                        },
                                        onAddFolderClick = onNewFolder,
                                        onAddMediaClick = {
                                            onAddMedia(AddMediaType.GALLERY)
                                        }
                                    )
                                }

                                in 1 until projects.size -> {
                                    // Next project IDs (page - 1)
                                    val currentProject = projects[page]
                                    MainMediaScreen(
                                        projectId = currentProject.id,
                                        project = currentProject,
                                        space = state.selectedSpace,
                                        onNavigateToPreview = { projectId ->
                                            // TODO: Navigate to PreviewActivity - needs to be passed from parent
                                        },
                                        onShowUploadManager = {
                                            // TODO: Show upload manager - needs to be passed from parent
                                        },
                                        onShowErrorDialog = { media, position ->
                                            // TODO: Show error dialog - needs to be implemented
                                        },
                                        onSelectionModeChanged = { isSelecting, count ->
                                            // TODO: Update selection mode - needs to be implemented
                                        },
                                        onAddServerClick = {
                                            // TODO: Navigate to add server screen
                                        },
                                        onAddFolderClick = onNewFolder,
                                        onAddMediaClick = {
                                            onAddMedia(AddMediaType.GALLERY)
                                        }
                                    )
                                }

                                totalPages - 1 -> {
                                    // Always settings screen as the last page
                                    SettingsScreen(
                                        onNavigateToCache = onNavigateToCache,
                                        onNavigateToProofMode = {
                                            onAction(HomeScreenAction.NavigateToProofModeSettings)
                                        }
                                    )
                                }

                                else -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Unexpected page index")
                                    }
                                } // This should never be reached
                            }

                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun MainContentPreview() {
    SaveAppTheme {

        HomeScreenContent(
            onExit = {},
            state = HomeScreenState(),
            onAction = {},
            onNewFolder = {},
            onAddMedia = {}
        )
    }
}


//@Composable
//fun MainMediaScreen(projectId: Long) {
//
//    val fragmentState = rememberFragmentState()
//
//    AndroidFragment<MainMediaFragment>(
//        modifier = Modifier.fillMaxSize(),
//        fragmentState = fragmentState,
//        arguments = bundleOf("project_id" to projectId),
//        onUpdate = {
//            //
//        }
//    )
//}