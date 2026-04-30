package net.opendasharchive.openarchive.features.main.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import net.opendasharchive.openarchive.R
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
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
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.Alignment
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
import net.opendasharchive.openarchive.core.domain.Archive
import net.opendasharchive.openarchive.core.domain.Vault
import net.opendasharchive.openarchive.features.media.ContentPickerSheet
import net.opendasharchive.openarchive.features.media.MediaPicker
import net.opendasharchive.openarchive.features.media.rememberContentPickerLaunchers
import net.opendasharchive.openarchive.features.settings.SettingsScreen
import net.opendasharchive.openarchive.upload.UploadManagerScreen
import net.opendasharchive.openarchive.upload.UploadManagerViewModel
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

    LaunchedEffect(uiState.showProjectPickerForImport) {
        net.opendasharchive.openarchive.core.logger.AppLogger.d("SHARE_DEBUG: HomeScreen showProjectPickerForImport=${uiState.showProjectPickerForImport}")
    }

    // Handle In-App Updates
    CheckForInAppUpdates(snackbarHostState)

    // Handle In-App Review
    CheckForInAppReview()

    val context = LocalContext.current
    val noFolderMessage = stringResource(R.string.tap_to_add_folder)
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
                        fromCamera = true,
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
                    val hasFolder = uiState.selectedProjectId != null &&
                        uiState.projects.any { it.id == uiState.selectedProjectId }
                    if (!hasFolder) {
                        snackbarHostState.showSnackbar(noFolderMessage)
                    } else {
                        when (event.type) {
                            AddMediaType.CAMERA -> {
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

    // Two-step project picker bottom sheet for share-sheet imports
    // Step 1: pick a space; Step 2: pick a folder within that space
    if (uiState.showProjectPickerForImport) {
        var selectedSpace by remember { mutableStateOf<Vault?>(null) }
        var spaceFolders by remember { mutableStateOf<List<Archive>>(emptyList()) }
        var foldersLoading by remember { mutableStateOf(false) }

        LaunchedEffect(selectedSpace) {
            val space = selectedSpace ?: return@LaunchedEffect
            foldersLoading = true
            spaceFolders = try {
                projectRepository.getProjects(space.id)
            } catch (e: Exception) {
                emptyList()
            }
            foldersLoading = false
        }

        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = {
                selectedSpace = null
                viewModel.onAction(HomeAction.DismissSharedImportPicker)
            },
            sheetState = sheetState
        ) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                if (selectedSpace == null) {
                    // ── Step 1: space list ──
                    Text(
                        text = "Select server",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                    HorizontalDivider()
                    if (uiState.spaces.isEmpty()) {
                        Text(
                            text = "No servers configured. Add a server first.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        LazyColumn {
                            items(uiState.spaces) { space ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedSpace = space }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_folder),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = space.friendlyName,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            text = space.type.friendlyName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // ── Step 2: folder list for selected space ──
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, end = 16.dp, top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { selectedSpace = null }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back),
                                contentDescription = "Back"
                            )
                        }
                        Text(
                            text = selectedSpace!!.friendlyName,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    HorizontalDivider()
                    when {
                        foldersLoading -> {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Loading folders…", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        spaceFolders.isEmpty() -> {
                            Text(
                                text = "No folders found in this server.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                        else -> {
                            LazyColumn {
                                items(spaceFolders) { project ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val uris = uiState.pendingSharedUris ?: return@clickable
                                                selectedSpace = null
                                                viewModel.onAction(HomeAction.DismissSharedImportPicker)
                                                manualImportInProgress = true
                                                scope.launch(Dispatchers.IO) {
                                                    try {
                                                        val submission = projectRepository.getActiveSubmission(project.id)
                                                        val evidenceList = MediaPicker.import(
                                                            context,
                                                            project,
                                                            submission.id,
                                                            uris,
                                                        )
                                                        evidenceList.forEach { mediaRepository.addEvidence(it) }
                                                        withContext(Dispatchers.Main) {
                                                            if (evidenceList.isNotEmpty()) {
                                                                viewModel.onAction(HomeAction.MediaImported(project.id))
                                                            } else {
                                                                snackbarHostState.showSnackbar("Import failed")
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
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_folder),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = project.description ?: "Unnamed folder",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                HorizontalDivider()
                TextButton(
                    onClick = {
                        selectedSpace = null
                        viewModel.onAction(HomeAction.DismissSharedImportPicker)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
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

    val showDrawer = isSettings.not() && (state.spaces.isNotEmpty() || state.hasDwebEntry)

    // Back on settings tab → go to media tab (lower priority, defined first)
    BackHandler(enabled = isSettings) {
        onAction(HomeAction.TabSelected(HomeBottomTab.MEDIA))
    }

    // Back when drawer is open → close drawer (higher priority, defined last)
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

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

    // Hoist UploadManagerViewModel outside the if-block so its viewModelScope and
    // reactive observers (InvalidationBus, UploadEventBus) are bound to HomeScreenContent's
    // stable ViewModelStoreOwner rather than the ModalBottomSheet's dialog window scope,
    // which would create a fresh ViewModel (and cancel its flows) on every open/close.
    val uploadManagerViewModel: UploadManagerViewModel = koinViewModel()

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
                viewModel = uploadManagerViewModel,
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
