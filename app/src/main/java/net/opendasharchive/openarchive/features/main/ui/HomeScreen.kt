package net.opendasharchive.openarchive.features.main.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.core.navigation.NavigationResultKeys
import net.opendasharchive.openarchive.core.navigation.ResultEffect
import net.opendasharchive.openarchive.core.presentation.theme.DefaultBoxPreview
import net.opendasharchive.openarchive.core.repositories.MediaRepository
import net.opendasharchive.openarchive.core.repositories.ProjectRepository
import net.opendasharchive.openarchive.features.main.CheckForInAppReview
import net.opendasharchive.openarchive.features.main.CheckForInAppUpdates
import net.opendasharchive.openarchive.features.main.ui.components.HomeAppBar
import net.opendasharchive.openarchive.features.main.ui.components.HomeBottomTab
import net.opendasharchive.openarchive.features.main.ui.components.MainBottomBar
import net.opendasharchive.openarchive.features.main.ui.components.MainDrawerContent
import net.opendasharchive.openarchive.features.media.AddMediaType
import net.opendasharchive.openarchive.features.media.ContentPickerSheet
import net.opendasharchive.openarchive.features.media.MediaPicker
import net.opendasharchive.openarchive.features.media.rememberContentPickerLaunchers
import net.opendasharchive.openarchive.features.settings.SettingsScreen
import net.opendasharchive.openarchive.upload.UploadManagerScreen
import net.opendasharchive.openarchive.util.rememberComposePermissionManager
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
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
    var manualImportInProgress by remember { mutableStateOf(false) }
    var importSnackbarJob by remember { mutableStateOf<Job?>(null) }
    var pendingImportedCount by remember { mutableIntStateOf(-1) }
    var pendingImportedProjectId by remember { mutableStateOf<Long?>(null) }

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
            pendingImportedCount = evidenceList.size
            pendingImportedProjectId = uiState.selectedProjectId
        }
    )
    val isImporting = pickerLaunchers.isProcessing || manualImportInProgress

    LaunchedEffect(isImporting) {
        if (isImporting) {
            if (importSnackbarJob == null) {
                importSnackbarJob = scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Importing media...",
                        duration = SnackbarDuration.Indefinite
                    )
                }
            }
        } else {
            importSnackbarJob?.cancel()
            importSnackbarJob = null
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }

    LaunchedEffect(pickerLaunchers.isProcessing, pendingImportedCount, pendingImportedProjectId) {
        if (!pickerLaunchers.isProcessing && pendingImportedCount >= 0) {
            if (pendingImportedCount > 0 && pendingImportedProjectId != null) {
                scope.launch {
                    snackbarHostState.showSnackbar("Media imported")
                }
                viewModel.onAction(HomeAction.MediaImported(pendingImportedProjectId!!))
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar("Import failed")
                }
            }
            pendingImportedCount = -1
            pendingImportedProjectId = null
        }
    }
    val permissionManager = rememberComposePermissionManager()

    // Receive camera capture results from CameraScreen via ResultEventBus
    ResultEffect<CameraCaptureResult>(resultKey = NavigationResultKeys.CAMERA_CAPTURE_RESULT) { result ->
        manualImportInProgress = true
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
                    )
                    evidenceList.forEach { evidence ->
                        mediaRepository.addEvidence(evidence)
                    }
                    withContext(Dispatchers.Main) {
                        if (evidenceList.isNotEmpty()) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Media imported")
                            }
                            viewModel.onAction(HomeAction.MediaImported(result.projectId))
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar("Import failed")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    snackbarHostState.showSnackbar("Failed to import: ${e.localizedMessage}")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    manualImportInProgress = false
                }
            }
        }
    }

    // Receive shared media imports from HomeActivity via ResultEventBus
    ResultEffect<List<android.net.Uri>>(resultKey = NavigationResultKeys.SHARED_MEDIA_IMPORT) { uris ->
        manualImportInProgress = true
        scope.launch(Dispatchers.IO) {
            try {
                // Use the currently selected project for the import
                val selectedProject = uiState.projects.getOrNull(uiState.pagerIndex)
                if (selectedProject != null && uris.isNotEmpty()) {
                    val submission = projectRepository.getActiveSubmission(selectedProject.id)
                    val evidenceList = MediaPicker.import(
                        context,
                        selectedProject,
                        submission.id,
                        uris,
                    )
                    evidenceList.forEach { evidence ->
                        mediaRepository.addEvidence(evidence)
                    }
                    withContext(Dispatchers.Main) {
                        if (evidenceList.isNotEmpty()) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Media imported")
                            }
                            viewModel.onAction(HomeAction.NavigateToPreviewMedia)
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar("Import failed")
                            }
                        }
                    }
                } else if (uris.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        snackbarHostState.showSnackbar("Please select a folder first")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    snackbarHostState.showSnackbar("Failed to import: ${e.localizedMessage}")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    manualImportInProgress = false
                }
            }
        }
    }

    // Receive space added result to refresh space list after coming from space setup complete screen
    ResultEffect<Boolean>(resultKey = NavigationResultKeys.REFRESH_SPACES) { success ->
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
        snackbarHostState = snackbarHostState,
        showImportLoading = isImporting
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
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    showImportLoading: Boolean = false
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

    val showDrawer = isSettings.not() && (state.spaces.isNotEmpty() || state.hasDwebEntry || state.hasStorachaEntry)

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
                        showDwebEntry = state.hasDwebEntry,
                        onDwebSelected = {
                            scope.launch { drawerState.close() }
                            onAction(HomeAction.Navigate(route = AppRoute.SnowbirdDashboardRoute))
                        },
                        showStorachaEntry = state.hasStorachaEntry,
                        onStorachaSelected = {
                            scope.launch { drawerState.close() }
                            onAction(HomeAction.Navigate(route = AppRoute.StorachaRoute))
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
                        SnackbarHost(hostState = snackbarHostState) { snackbarData ->
                            if (showImportLoading) {
                                Snackbar {
                                    Row {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(snackbarData.visuals.message)
                                    }
                                }
                            } else {
                                Snackbar(snackbarData = snackbarData)
                            }
                        }
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
                                    onNavigateToC2pa = {
                                        onAction(HomeAction.Navigate(AppRoute.C2paSettings))
                                    },
                                    onNavigateToStoracha = {
                                        onAction(HomeAction.Navigate(AppRoute.StorachaRoute))
                                    },
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
                                    state = MainMediaState(currentSpace = state.currentSpace, isLoading = false),
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
            onMediaTypeSelected = { type ->
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
    DefaultBoxPreview {

        HomeScreenContent(
            state = HomeState(),
            onAction = {},
        )
    }
}
