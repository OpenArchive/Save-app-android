package net.opendasharchive.openarchive.features.main.ui

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.main.ui.components.HomeAppBar
import net.opendasharchive.openarchive.features.main.ui.components.MainBottomBar
import net.opendasharchive.openarchive.features.main.ui.components.MainDrawerContent
import net.opendasharchive.openarchive.features.main.ui.components.SpaceIcon
import net.opendasharchive.openarchive.features.media.AddMediaType
import net.opendasharchive.openarchive.features.settings.SettingsScreen
import org.koin.androidx.compose.koinViewModel
import kotlin.math.max


@Serializable
data object HomeRoute

@Serializable
data object MediaCacheRoute

@Composable
fun SaveNavGraph(
    context: Context,
    viewModel: HomeViewModel = koinViewModel(),
    onExit: () -> Unit,
    onNewFolder: () -> Unit,
    onFolderSelected: (Long) -> Unit,
    onAddMedia: (AddMediaType) -> Unit
) {
    val navController = rememberNavController()

    SaveAppTheme {

        NavHost(
            navController = navController,
            startDestination = HomeRoute
        ) {

            composable<HomeRoute> {
                HomeScreen(
                    viewModel = viewModel,
                    onExit = onExit,
                    onNewFolder = onNewFolder,
                    onFolderSelected = onFolderSelected,
                    onAddMedia = onAddMedia,
                    onNavigateToCache = {
                        navController.navigate(MediaCacheRoute)
                    }
                )
            }

            composable<MediaCacheRoute> {
                MediaCacheScreen(context) {
                    navController.popBackStack()
                }
            }

        }
    }
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel(),
    onExit: () -> Unit,
    onNewFolder: () -> Unit,
    onFolderSelected: (Long) -> Unit,
    onAddMedia: (AddMediaType) -> Unit,
    onNavigateToCache: () -> Unit
) {

    val state by viewModel.uiState.collectAsStateWithLifecycle()

        HomeScreenContent(
            onExit = onExit,
            state = state,
            onAction = viewModel::onAction,
            onNavigateToCache = onNavigateToCache
        )


}

class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HomeScreenState())
    val uiState: StateFlow<HomeScreenState> = _uiState.asStateFlow()

    init {
        loadSpacesAndFolders()
    }

    fun onAction(action: HomeScreenAction) {
        when (action) {
            is HomeScreenAction.UpdateSelectedProject -> {
                _uiState.update { it.copy(selectedProject = action.project) }
            }

            is HomeScreenAction.AddMediaClicked -> TODO()
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
    data class AddMediaClicked(val mediaType: AddMediaType): HomeScreenAction()
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
    onNavigateToCache: () -> Unit = {}
) {

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val projects = state.projectsForSelectedSpace
    val totalPages = max(1, projects.size) + 1
    val pagerState = rememberPagerState(initialPage = 0) { totalPages }

    val currentProjectIndex = state.selectedProject?.let { selected ->
        projects.indexOfFirst { it.id == selected.id }.takeIf { it >= 0 } ?: 0
    } ?: 0

    // Whenever the pager’s current page changes and it represents a project page,
    // update the view model’s selected project.
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
                        spaceList = state.allSpaces
                    )
                }
            }
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {

                Scaffold(
                    topBar = {
                        HomeAppBar(
                            onExit = onExit,
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
                            onAddMediaClick = {},
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
                    }

                ) { paddingValues ->

                    Column(
                        modifier = Modifier.padding(paddingValues)
                    ) {
                        AnimatedVisibility(
                            visible = pagerState.currentPage < totalPages - 1,
                            enter = slideInHorizontally(
                                animationSpec = tween()
                            ),
                            exit = slideOutHorizontally(
                                animationSpec = tween()
                            )
                        ) {
                            val selectedProject =
                                state.selectedProject ?: error("Project should not be empty")
                            val selectedSpace =
                                state.selectedSpace ?: error("Space should not be empty")

                            val folderName = selectedProject.description
                                ?: selectedProject.created.toString()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = dimensionResource(R.dimen.activity_horizontal_margin)),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row {
                                    SpaceIcon(
                                        type = selectedSpace.tType ?: Space.Type.WEBDAV,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Icon(
                                        painter = painterResource(R.drawable.keyboard_arrow_right),
                                        contentDescription = null
                                    )
                                    Text(folderName)
                                }


                                TextButton(
                                    onClick = {}
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_edit_folder),
                                        contentDescription = null
                                    )
                                    Text("Edit")
                                }
                            }
                        }



                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                        ) { page ->

                            when (page) {
                                0 -> {
                                    // First page: If no projects, show -1, else show first project's ID
                                    MainMediaScreen(projectId = if (projects.isEmpty()) -1 else projects[0].id)
                                }

                                in 1 until projects.size -> {
                                    // Next project IDs (page - 1)
                                    MainMediaScreen(projects[page].id)
                                }

                                totalPages - 1 -> {
                                    // Always settings screen as the last page
                                    SettingsScreen(
                                        onNavigateToCache = onNavigateToCache
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
}

@Preview
@Composable
private fun MainContentPreview() {
    SaveAppTheme {

        HomeScreenContent(
            onExit = {},
            state = HomeScreenState(),
            onAction = {}
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