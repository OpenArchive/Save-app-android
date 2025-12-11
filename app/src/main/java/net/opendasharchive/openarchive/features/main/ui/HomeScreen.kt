package net.opendasharchive.openarchive.features.main.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.features.main.ui.components.HomeAppBar
import net.opendasharchive.openarchive.features.main.ui.components.HomeBottomTab
import net.opendasharchive.openarchive.features.main.ui.components.MainBottomBar
import net.opendasharchive.openarchive.features.main.ui.components.MainDrawerContent
import net.opendasharchive.openarchive.features.settings.SettingsScreen
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.max

/**
 * IMPROVED HomeScreen:
 * - Single HomeViewModel as source of truth
 * - Bridges MainMediaViewModel events → HomeViewModel actions
 * - No unnecessary HomeState copying
 * - Proper reactive data flow
 */
@Composable
fun HomeScreen(
    invokeNavEvent: (HomeNavigation) -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {

    val homeState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is HomeEvent.LaunchPicker -> {
                    // TODO: Launch picker
                }
                is HomeEvent.Navigate -> invokeNavEvent(event.destination)
                is HomeEvent.NavigateToProject -> Unit
                HomeEvent.ShowContentPickerSheet -> {
                    // TODO: Show content picker
                }
                is HomeEvent.ShowMessage -> {
                    // Show message via snackbar instead of Toast
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    HomeScreenContent(
        state = homeState,
        onAction = viewModel::onAction,
        snackbarHostState = snackbarHostState
    )
}


/**
 * IMPROVED HomeScreenContent:
 * - Takes getProject function instead of copying state
 * - Properly bridges MainMediaViewModel events to HomeViewModel
 * - Cleaner data flow
 */
@Composable
fun HomeScreenContent(
    state: HomeState,
    onAction: (HomeAction) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Calculate pager configuration
    val totalPages = max(1, state.projects.size) + 1
    val settingsIndex = totalPages - 1

    val pagerState = rememberPagerState(
        initialPage = state.pagerIndex.coerceIn(0, totalPages - 1)
    ) { totalPages }

    val selectedTab: HomeBottomTab =
        if (state.pagerIndex == settingsIndex) HomeBottomTab.SETTINGS else HomeBottomTab.MEDIA
    val isSettings = selectedTab == HomeBottomTab.SETTINGS

    val showDrawer = isSettings.not() && state.spaces.isNotEmpty()

    // Sync pager → HomeViewModel
    LaunchedEffect(pagerState.currentPage) {
        onAction(HomeAction.UpdatePager(pagerState.currentPage))
    }

    // HomeViewModel → pager (when state changes)
    LaunchedEffect(state.pagerIndex) {
        if (pagerState.currentPage != state.pagerIndex) {
            pagerState.animateScrollToPage(state.pagerIndex)
        }
    }

    // React to project list changes - update pager page count
    LaunchedEffect(state.projects.size) {
        // Pager will automatically rebuild with new page count via rememberPagerState
        // If current page is out of bounds, coerce it
        if (pagerState.currentPage >= totalPages) {
            pagerState.scrollToPage(settingsIndex)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {

        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = showDrawer,
            drawerContent = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {

                    MainDrawerContent(
                        selectedSpace = state.currentSpace,
                        spaceList = state.spaces,
                        projects = state.projects,
                        selectedProject = state.projects.firstOrNull { it.id == state.selectedProjectId },
                        onProjectSelected = { project ->
                            // Update selected project and close drawer
                            onAction(HomeAction.SelectProject(project.id))
                            scope.launch {
                                drawerState.close()
                                // Navigate to the project's page
                                val projectIndex = state.projects.indexOf(project)
                                if (projectIndex >= 0) {
                                    pagerState.scrollToPage(projectIndex)
                                }
                            }
                        },
                        onNewFolderClick = {
                            onAction(HomeAction.NavigateToAddNewFolder)
                        },
                        onSpaceSelected = { space ->
                            scope.launch { drawerState.close() }
                            onAction(HomeAction.SelectSpace(space.id))
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
                                onAction(HomeAction.TabSelected(tab))
                            },
                            onAddClick = {
                                onAction(HomeAction.AddClick)
                            },

                            onAddLongClick = {
                                onAction(HomeAction.AddLongClick)
                            }
                        )
                    },

                    snackbarHost = {
                        SnackbarHost(snackbarHostState)
                    }

                ) { paddingValues ->

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        // IMPORTANT: Set a key so pager items are properly keyed by content
                        key = { page ->
                            if (page == settingsIndex) {
                                "settings"
                            } else {
                                state.projects.getOrNull(page)?.id ?: "empty_$page"
                            }
                        }
                    ) { page ->

                        val isSettingsPage = page == settingsIndex
                        val projectForPage = state.projects.getOrNull(page)

                        when {
                            isSettingsPage -> {
                                SettingsScreen(
                                    onNavigateToSpaceList = {
                                        onAction(HomeAction.Navigate(HomeNavigation.SpaceList))
                                    },
                                    onNavigateToArchivedFolders = {
                                        onAction(HomeAction.NavigateToArchivedFolders)
                                    },
                                    onNavigateToCache = {
                                        onAction(HomeAction.Navigate(HomeNavigation.Cache))
                                    },
                                    onNavigateToProofMode = {
                                        onAction(HomeAction.Navigate(HomeNavigation.ProofMode))
                                    }
                                )
                            }

                            projectForPage != null -> {
                                val projectId = projectForPage.id
                                val viewModel = koinViewModel<MainMediaViewModel>(
                                    key = "media_$projectId",
                                    parameters = { parametersOf(projectId) }
                                )

                                // IMPROVED: Bridge MainMediaViewModel events → HomeViewModel actions
                                LaunchedEffect(projectId) {
                                    viewModel.uiEvent.collectLatest { event ->
                                        when (event) {
                                            is MainMediaEvent.NavigateToPreview -> {
                                                onAction(HomeAction.NavigateToPreviewMedia)
                                            }
                                            is MainMediaEvent.RequestProjectRename -> {
                                                onAction(HomeAction.RenameProject(event.projectId, event.newName))
                                            }
                                            is MainMediaEvent.RequestProjectArchive -> {
                                                onAction(HomeAction.ArchiveProject(event.projectId))
                                            }
                                            is MainMediaEvent.RequestProjectDelete -> {
                                                onAction(HomeAction.DeleteProject(event.projectId))
                                            }
                                            // Other events handled internally by MainMediaScreen
                                            else -> Unit
                                        }
                                    }
                                }

                                MainMediaScreen(
                                    viewModel = viewModel,
                                    currentSpace = state.currentSpace,
                                    currentProject = projectForPage,
                                    onNavigateToPreview = {}
                                )
                            }

                            else -> {
                                // No projects yet: show empty media state without spinning up a VM.
                                MainMediaContent(
                                    state = MainMediaState(),
                                    currentSpace = state.currentSpace,
                                    currentProject = null,
                                    onAction = {}
                                )
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
            state = HomeState(),
            onAction = {},
        )
    }
}
