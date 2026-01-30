package net.opendasharchive.openarchive.features.main.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.core.navigation.ResultEffect
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.features.main.ui.components.HomeAppBar
import net.opendasharchive.openarchive.features.main.ui.components.HomeBottomTab
import net.opendasharchive.openarchive.features.main.ui.components.MainBottomBar
import net.opendasharchive.openarchive.features.main.ui.components.MainDrawerContent
import net.opendasharchive.openarchive.features.media.AddMediaType
import net.opendasharchive.openarchive.features.media.ContentPickerSheet
import net.opendasharchive.openarchive.features.media.rememberContentPickerLaunchers
import net.opendasharchive.openarchive.features.settings.SettingsScreen
import net.opendasharchive.openarchive.util.rememberComposePermissionManager
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.max
import net.opendasharchive.openarchive.core.repositories.ProjectRepository
import net.opendasharchive.openarchive.core.repositories.MediaRepository
import org.koin.compose.koinInject

import net.opendasharchive.openarchive.features.main.CheckForInAppUpdates
import net.opendasharchive.openarchive.features.main.CheckForInAppReview
import net.opendasharchive.openarchive.features.media.MediaPicker
import net.opendasharchive.openarchive.upload.UploadManagerScreen

/**
 * IMPROVED HomeScreen:
 * - Single HomeViewModel as source of truth
 * - Bridges MainMediaViewModel events → HomeViewModel actions
 * - No unnecessary HomeState copying
 * - Proper reactive data flow
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel(),
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle In-App Updates
    CheckForInAppUpdates(snackbarHostState)

    // Handle In-App Review
    CheckForInAppReview()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val projectRepository: ProjectRepository = koinInject()
    val mediaRepository: MediaRepository = koinInject()

    // Content Picker Launchers for Gallery/Files
    // Camera is handled via navigation
    val pickerLaunchers = rememberContentPickerLaunchers(
        projectProvider = { uiState.projects.firstOrNull { it.id == uiState.selectedProjectId } },
        onError = { message ->
            scope.launch {
                snackbarHostState.showSnackbar(message)
            }
        },
        onMediaImported = { evidenceList ->
            // Handle imported media (refresh, show toast, etc.)
            // For now, we'll just reload the projects to refresh media counts
            viewModel.onAction(HomeAction.Reload)
            uiState.selectedProjectId?.let { projectId ->
                viewModel.onAction(HomeAction.MediaImported(projectId))
            }
        }
    )
    val permissionManager = rememberComposePermissionManager()

    // Receive camera capture results from CameraScreen via ResultEventBus
    ResultEffect<CameraCaptureResult>(resultKey = "camera_capture_result") { result ->
        scope.launch(Dispatchers.IO) {
            try {
                val archive = projectRepository.getProject(result.projectId)
                if (archive != null && result.capturedUris.isNotEmpty()) {
                    val submission = projectRepository.getActiveSubmission(archive.id)
                    val evidenceList = MediaPicker.import(
                        context,
                        archive,
                        submission.id,
                        result.capturedUris,
                        generateProof = Prefs.useProofMode
                    )
                    evidenceList.forEach { evidence ->
                        mediaRepository.addEvidence(evidence)
                    }
                    withContext(Dispatchers.Main) {
                        snackbarHostState.showSnackbar("${evidenceList.size} item(s) imported")
                        viewModel.onAction(HomeAction.Reload)
                        viewModel.onAction(HomeAction.MediaImported(result.projectId))
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    snackbarHostState.showSnackbar("Failed to import: ${e.localizedMessage}")
                }
            }
        }
    }

    // Receive space added result to refresh space list after coming from space setup complete screen
    ResultEffect<Boolean>(resultKey = "refresh_spaces") { success ->
        if (success) {
            viewModel.onAction(HomeAction.Load)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is HomeEvent.LaunchPicker -> {
                    when (event.type) {
                        AddMediaType.CAMERA -> {
                            // Navigate to camera screen
                            viewModel.onAction(HomeAction.NavigateToCamera)
                        }

                        AddMediaType.GALLERY -> {
                            permissionManager.checkMediaPermissions {
                                pickerLaunchers.launch(AddMediaType.GALLERY)
                            }
                        }

                        AddMediaType.FILES -> {
                            pickerLaunchers.launch(AddMediaType.FILES)
                        }
                    }
                }

                is HomeEvent.NavigateToProject -> Unit
                is HomeEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    HomeScreenContent(
        state = uiState,
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

    // Sync pager → HomeViewModel ONLY when settled
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { settledPage ->
                onAction(HomeAction.UpdatePager(settledPage))
            }
    }

    // HomeViewModel → pager (when state changes)
    LaunchedEffect(state.pagerIndex) {
        if (!pagerState.isScrollInProgress && pagerState.currentPage != state.pagerIndex) {
            val distance = kotlin.math.abs(pagerState.currentPage - state.pagerIndex)
            if (distance > 2) {
                pagerState.scrollToPage(state.pagerIndex)
            } else {
                pagerState.animateScrollToPage(state.pagerIndex)
            }
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
                        onSpaceSelected = { spaceId ->
                            scope.launch { drawerState.close() }
                            onAction(HomeAction.SelectSpace(spaceId))
                        },
                        onAddNewSpaceClicked = {
                            scope.launch { drawerState.close() }
                            onAction(HomeAction.Navigate(route = AppRoute.SpaceSetupRoute))
                        },
                        onAddNewFolderClicked = {
                            scope.launch { drawerState.close() }
                            onAction(HomeAction.NavigateToAddNewFolder)
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
                        beyondViewportPageCount = 0,
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
                                        onAction(HomeAction.Navigate(AppRoute.SpaceListRoute))
                                    },
                                    onNavigateToArchivedFolders = {
                                        onAction(HomeAction.NavigateToArchivedFolders)
                                    },
                                    onNavigateToCache = {
                                        onAction(HomeAction.Navigate(AppRoute.MediaCacheRoute))
                                    },
                                    onNavigateToProofMode = {
                                        onAction(HomeAction.Navigate(AppRoute.ProofModeSettings))
                                    }
                                )
                            }

                            projectForPage != null -> {
                                val projectId = projectForPage.id
                                val viewModel = koinViewModel<MainMediaViewModel>(
                                    key = "media_$projectId",
                                    parameters = { parametersOf(projectId) }
                                )

                                // Bridge MainMediaViewModel events → HomeViewModel actions
                                LaunchedEffect(projectId) {
                                    viewModel.projectEvent.collectLatest { event ->
                                        when (event) {
                                            is MainMediaProjectEvent.RequestProjectRename -> {
                                                onAction(HomeAction.RenameProject(event.projectId, event.newName))
                                            }

                                            is MainMediaProjectEvent.RequestProjectArchive -> {
                                                onAction(HomeAction.ArchiveProject(event.projectId))
                                            }

                                            is MainMediaProjectEvent.RequestProjectDelete -> {
                                                onAction(HomeAction.DeleteProject(event.projectId))
                                            }
                                        }
                                    }
                                }

                                MainMediaScreen(
                                    viewModel = viewModel,
                                    refreshProjectId = state.mediaRefreshProjectId,
                                    refreshToken = state.mediaRefreshToken,
                                    onNavigateToPreview = {
                                        onAction(HomeAction.NavigateToPreviewMedia)
                                    },
                                    onShowUploadManager = {
                                        onAction(HomeAction.ShowUploadManager)
                                    }
                                )
                            }

                            else -> {
                                // No projects yet: show empty media state with current space from HomeViewModel
                                MainMediaContent(
                                    state = MainMediaState(currentSpace = state.currentSpace),
                                    onAction = {}
                                )
                            }
                        }

                    }
                }

            }
        }
    }

    // Content Picker Bottom Sheet
    if (state.showContentPicker) {
        ContentPickerSheet(
            onDismiss = {
                onAction(HomeAction.ContentPickerDismissed)
            },
            onMediaPicked = { type ->
                onAction(HomeAction.ContentPickerPicked(type))
            }
        )
    }

    // Upload Manager Bottom Sheet
    if (state.showUploadManager) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = {
                onAction(HomeAction.HideUploadManager)
            },
            sheetState = sheetState
        ) {
            UploadManagerScreen(
                viewModel = koinViewModel(),
                onClose = {
                    onAction(HomeAction.HideUploadManager)
                }
            )
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
