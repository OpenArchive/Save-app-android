package net.opendasharchive.openarchive.features.main.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.features.main.ui.HomeAction
import net.opendasharchive.openarchive.features.main.ui.HomeState
import net.opendasharchive.openarchive.features.main.ui.components.HomeAppBar
import net.opendasharchive.openarchive.features.main.ui.components.HomeBottomTab
import net.opendasharchive.openarchive.features.main.ui.components.MainBottomBar
import net.opendasharchive.openarchive.features.main.ui.components.MainDrawerContent
import net.opendasharchive.openarchive.features.media.AddMediaType
import net.opendasharchive.openarchive.features.media.ContentPickerSheet
import net.opendasharchive.openarchive.features.media.rememberContentPickerLaunchers
import net.opendasharchive.openarchive.features.settings.SettingsScreen
import net.opendasharchive.openarchive.upload.BroadcastManager
import net.opendasharchive.openarchive.util.Prefs
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.max

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = koinViewModel(),
    onExit: () -> Unit,
    onFolderSelected: (Long) -> Unit,
    onAddMedia: (AddMediaType) -> Unit,
    onNavigateToCache: () -> Unit,
    onNavigateToProofModeSettings: () -> Unit,
    onNavigateToPreview: (Long) -> Unit,
    onNavigateToSpaceSetup: () -> Unit,
    onNavigateToAddNewFolder: (Long) -> Unit,
    onNavigateToSpaceList: () -> Unit = {},
    onNavigateToArchivedFolders: (Long?) -> Unit = {}
) {

    val context = LocalContext.current
    val homeState by homeViewModel.state.collectAsStateWithLifecycle()

    HomeScreenContent(
        onExit = onExit,
        homeState = homeState,
        onHomeAction = homeViewModel::onAction,
        onNavigateToCache = onNavigateToCache,
        onAddMedia = onAddMedia,
        onNavigateToPreview = onNavigateToPreview,
        onNavigateToProofModeSettings = onNavigateToProofModeSettings,
        onNavigateToSpaceSetup = onNavigateToSpaceSetup,
        onNavigateToAddNewFolder = onNavigateToAddNewFolder,
        onNavigateToSpaceList = onNavigateToSpaceList,
        onNavigateToArchivedFolders = onNavigateToArchivedFolders,
    )


}


@Composable
fun HomeScreenContent(
    onExit: () -> Unit,
    homeState: HomeState,
    onHomeAction: (HomeAction) -> Unit,
    onNavigateToCache: () -> Unit = {},
    onAddMedia: (AddMediaType) -> Unit,
    onNavigateToPreview: (Long) -> Unit,
    onNavigateToProofModeSettings: () -> Unit,
    onNavigateToSpaceSetup: () -> Unit,
    onNavigateToAddNewFolder: (Long) -> Unit,
    onNavigateToSpaceList: () -> Unit = {},
    onNavigateToArchivedFolders: (Long?) -> Unit = {}
) {
    val contentPicker = rememberContentPickerLaunchers(
        projectProvider = {
            val selectedId = homeState.selectedProjectId
            homeState.projects.firstOrNull { it.id == selectedId }
        },
        onMediaImported = { mediaList ->
            // refresh project collection here
            // The main media content should be refreshed
            // and then navigate to preview screen
            val project =
                homeState.projects.firstOrNull { it.id == homeState.selectedProjectId }
                    ?: return@rememberContentPickerLaunchers
            onNavigateToPreview(project.id)
        }
    )

    var showContentPicker by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()


    val totalPages = max(1, homeState.projects.size) + 1

    // Always start at last media page (never settings) for fresh starts
    // For configuration changes, the activity handles restoration
    val initialPage = Prefs.currentHomePage.coerceIn(0, (totalPages - 2).coerceAtLeast(0))
    val pagerState = rememberPagerState(initialPage = initialPage) { totalPages }

    val currentProjectIndex = homeState.selectedProjectId?.let { selectedId ->
        homeState.projects.indexOfFirst { it.id == selectedId }.takeIf { it >= 0 } ?: 0
    } ?: 0

    var selectedTab: HomeBottomTab by remember {
        val tab =
            if (totalPages == currentProjectIndex) HomeBottomTab.SETTINGS else HomeBottomTab.MEDIA
        mutableStateOf(tab)
    }

    val isSettings = pagerState.currentPage == (totalPages - 1)

    val showDrawer = isSettings.not() && homeState.spaces.isNotEmpty()

    // Save current page ONLY if it's a media page (not settings)
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage < totalPages - 1) {
            Prefs.currentHomePage = pagerState.currentPage
        }
        onHomeAction(HomeAction.UpdatePager(pagerState.currentPage))
    }

    // Whenever the pager's current page changes and it represents a project page,
    // update the view model's selected project.
    LaunchedEffect(pagerState.currentPage, homeState.projects) {
        if (homeState.projects.isNotEmpty() && pagerState.currentPage < homeState.projects.size) {
            val newlySelectedProject = homeState.projects[pagerState.currentPage]
            onHomeAction(HomeAction.SelectProject(newlySelectedProject.id))
        }
    }

    // Update selectedTab when pager is swiped
    LaunchedEffect(pagerState.currentPage, totalPages) {
        selectedTab = if (pagerState.currentPage == totalPages - 1) {
            HomeBottomTab.SETTINGS
        } else {
            HomeBottomTab.MEDIA
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {

        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = showDrawer,
            drawerContent = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {

                    MainDrawerContent(
                        selectedSpace = homeState.currentSpace,
                        spaceList = homeState.spaces,
                        projects = homeState.projects,
                        selectedProject = homeState.projects.firstOrNull { it.id == homeState.selectedProjectId },
                        onProjectSelected = { project ->
                            // Update selected project and close drawer
                            onHomeAction(HomeAction.SelectProject(project.id))
                            scope.launch {
                                drawerState.close()
                                // Navigate to the project's page
                                val projectIndex = homeState.projects.indexOf(project)
                                if (projectIndex >= 0) {
                                    pagerState.scrollToPage(projectIndex)
                                }
                            }
                        },
                        onNewFolderClick = onNewFolderClick@{
                            val spaceId = homeState.currentSpace?.id ?: return@onNewFolderClick
                            onNavigateToAddNewFolder(spaceId)
                        },
                        onSpaceSelected = { space ->
                            scope.launch { drawerState.close() }
                            onHomeAction(HomeAction.SelectSpace(space.id))
                        },
                        onAddAnotherAccountClicked = {
                            scope.launch { drawerState.close() }
                        },
                    )

                }
            }
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {

                Scaffold(
                    topBar = {
                        HomeAppBar(
                            onExit = onExit,
                            showDrawer = showDrawer,
                            openDrawer = {
                                scope.launch {
                                    drawerState.open()
                                }
                            }
                        )
                    },

                    bottomBar = {
                        MainBottomBar(
                            selectedTab = selectedTab,
                            onTabSelected = { tab ->
                                when (tab) {
                                    HomeBottomTab.MEDIA -> {
                                        val targetPage =
                                            if (homeState.projects.isEmpty()) 0 else currentProjectIndex
                                        if (pagerState.currentPage != targetPage) {
                                            scope.launch { pagerState.animateScrollToPage(targetPage) }
                                        }
                                    }

                                    HomeBottomTab.SETTINGS -> {
                                        if (pagerState.currentPage != totalPages - 1) {
                                            Prefs.putInt("settings_scroll_position", 0)
                                            scope.launch { pagerState.animateScrollToPage(totalPages - 1) }
                                        }
                                    }
                                }

                            selectedTab = tab
                        },
                        onAddClick = onAddClick@{
                            when {
                                homeState.currentSpace == null -> {
                                    scope.launch {
                                        onNavigateToSpaceSetup()
                                    }
                                }

                                homeState.projects.isEmpty() || homeState.selectedProjectId == null -> {
                                    val space = homeState.currentSpace
                                    onNavigateToAddNewFolder(space.id)
                                }

                                    else -> {
                                        // If we are in SettingsScreen, pager scroll back to previous MainMediaScreen
                                        if (pagerState.currentPage == totalPages - 1) {
                                            scope.launch {
                                                pagerState.animateScrollToPage(currentProjectIndex)
                                                contentPicker.launch(AddMediaType.GALLERY)
                                            }
                                        } else {
                                            contentPicker.launch(AddMediaType.GALLERY)
                                        }
                                    }
                                }
                            },

                        onAddLongClick = {
                            when {
                                homeState.currentSpace == null -> {
                                    scope.launch { onNavigateToSpaceSetup() }
                                }

                                homeState.projects.isEmpty() || homeState.selectedProjectId == null -> {
                                    val space = homeState.currentSpace
                                    onNavigateToAddNewFolder(space.id)
                                }

                                    pagerState.currentPage == totalPages - 1 -> {
                                        scope.launch {
                                            pagerState.animateScrollToPage(currentProjectIndex)
                                            showContentPicker = true
                                        }
                                    }

                                    else -> {
                                        showContentPicker = true
                                    }
                                }
                            }
                        )
                    },
                    contentWindowInsets = WindowInsets(
                        0,
                        0,
                        0,
                        0
                    )

                ) { paddingValues ->

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                    ) { page ->

                        when (page) {
                            0 -> {
                                // First page: If no projects, show -1, else show first project's ID
                                val firstProject = homeState.projects.firstOrNull()
                                val projectId = firstProject?.id ?: -1
                                val viewModel = koinViewModel<MainMediaViewModel>(
                                    key = "media_$projectId",
                                    parameters = { parametersOf(projectId) }
                                )

                                MainMediaScreen(
                                    viewModel = viewModel,
                                    homeState = homeState.copy(selectedProjectId = firstProject?.id),
                                    projectId = firstProject?.id ?: -1,
                                    onNavigateToPreview = onNavigateToPreview,
                                )
                            }

                            in 1 until homeState.projects.size -> {
                                // Next project IDs (page - 1)
                                val currentProject = homeState.projects[page]
                                val projectId = currentProject.id
                                val viewModel = koinViewModel<MainMediaViewModel>(
                                    key = "media_$projectId",
                                    parameters = { parametersOf(projectId) }
                                )

                                MainMediaScreen(
                                    viewModel = viewModel,
                                    homeState = homeState.copy(selectedProjectId = currentProject.id),
                                    projectId = currentProject.id,
                                    onNavigateToPreview = onNavigateToPreview,
                                )
                            }

                            totalPages - 1 -> {
                                // Always settings screen as the last page
                                SettingsScreen(
                                    onNavigateToSpaceList = onNavigateToSpaceList,
                                    onNavigateToArchivedFolders = {
                                        onNavigateToArchivedFolders(homeState.currentSpace?.id)
                                    },
                                    onNavigateToCache = onNavigateToCache,
                                    onNavigateToProofMode = onNavigateToProofModeSettings
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

                if (showContentPicker) {
                    ContentPickerSheet(
                        onDismiss = { showContentPicker = false },
                        onMediaPicked = { mediaType ->
                            showContentPicker = false
                            onAddMedia(mediaType)
                        }
                    )
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
            homeState = HomeState(),
            onHomeAction = {},
            onNavigateToAddNewFolder = {},
            onNavigateToCache = {},
            onAddMedia = {},
            onNavigateToPreview = {},
            onNavigateToSpaceSetup = {},
            onNavigateToProofModeSettings = {},
        )
    }
}
