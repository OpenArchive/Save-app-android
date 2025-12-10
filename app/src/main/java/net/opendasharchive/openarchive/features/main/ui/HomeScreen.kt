package net.opendasharchive.openarchive.features.main.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
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
import androidx.compose.runtime.DisposableEffect
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
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
import kotlin.math.max

@Composable
fun HomeScreen(
    viewModel: MainMediaViewModel = koinViewModel(),
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
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is MainMediaEvent.NavigateToProofModeSettings -> {
                    onNavigateToProofModeSettings()
                }

                is MainMediaEvent.NavigateToSpaceList -> {
                    onNavigateToSpaceList()
                }

                is MainMediaEvent.NavigateToArchivedFolders -> {
                    onNavigateToArchivedFolders(state.space?.id)
                }

                is MainMediaEvent.ShowUploadManager -> TODO()
                is MainMediaEvent.ShowErrorDialog -> TODO()
                is MainMediaEvent.SelectionModeChanged -> TODO()
                MainMediaEvent.FocusFolderNameInput -> TODO()
                is MainMediaEvent.NavigateToPreview -> onNavigateToPreview(event.projectId)
                MainMediaEvent.ShowDeleteConfirmation -> TODO()
                is MainMediaEvent.ShowFolderOptionsPopup -> TODO()
                MainMediaEvent.ShowUploadManager -> TODO()
            }
        }
    }

    // BroadcastReceiver setup for upload progress
    DisposableEffect(Unit) {
        val handler = Handler(Looper.getMainLooper())
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = BroadcastManager.getAction(intent) ?: return
                when (action) {
                    BroadcastManager.Action.Change -> {
                        handler.post {
                            viewModel.onAction(
                                MainMediaAction.UpdateMediaItem(
                                    collectionId = action.collectionId,
                                    mediaId = action.mediaId,
                                    progress = action.progress,
                                    isUploaded = action.isUploaded
                                )
                            )
                        }
                    }

                    BroadcastManager.Action.Delete -> {
                        val projectId = state.project?.id ?: return
                        handler.post { viewModel.onAction(MainMediaAction.Refresh(projectId)) }
                    }
                }
            }
        }
        BroadcastManager.register(context, receiver)
        onDispose { BroadcastManager.unregister(context, receiver) }
    }

    HomeScreenContent(
        onExit = onExit,
        state = state,
        onAction = viewModel::onAction,
        onNavigateToCache = onNavigateToCache,
        onAddMedia = onAddMedia,
        onNavigateToPreview = onNavigateToPreview,
        onNavigateToSpaceSetup = onNavigateToSpaceSetup,
        onNavigateToAddNewFolder = onNavigateToAddNewFolder,
        onNavigateToSpaceList = onNavigateToSpaceList,
        onNavigateToArchivedFolders = onNavigateToArchivedFolders,
    )


}


@Composable
fun HomeScreenContent(
    onExit: () -> Unit,
    state: MainMediaState,
    onAction: (MainMediaAction) -> Unit,
    onNavigateToCache: () -> Unit = {},
    onAddMedia: (AddMediaType) -> Unit,
    onNavigateToPreview: (Long) -> Unit,
    onNavigateToSpaceSetup: () -> Unit,
    onNavigateToAddNewFolder: (Long) -> Unit,
    onNavigateToSpaceList: () -> Unit = {},
    onNavigateToArchivedFolders: (Long?) -> Unit = {}
) {
    val contentPicker = rememberContentPickerLaunchers(
        projectProvider = {
            state.project
        },
        onMediaImported = { mediaList ->
            // refresh project collection here
            // The main media content should be refreshed
            // and then navigate to preview screen
            val project = state.project ?: return@rememberContentPickerLaunchers
            onNavigateToPreview(project.id)
        }
    )

    var showContentPicker by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()


    val totalPages = max(1, state.projects.size) + 1

    // Always start at last media page (never settings) for fresh starts
    // For configuration changes, the activity handles restoration
    val initialPage = Prefs.currentHomePage.coerceIn(0, (totalPages - 2).coerceAtLeast(0))
    val pagerState = rememberPagerState(initialPage = initialPage) { totalPages }

    val currentProjectIndex = state.project?.let { selected ->
        state.projects.indexOfFirst { it.id == selected.id }.takeIf { it >= 0 } ?: 0
    } ?: 0

    var selectedTab: HomeBottomTab by remember {
        val tab =
            if (totalPages == currentProjectIndex) HomeBottomTab.SETTINGS else HomeBottomTab.MEDIA
        mutableStateOf(tab)
    }

    val isSettings = pagerState.currentPage == (totalPages - 1)

    val showDrawer = isSettings.not() && state.spaces.isNotEmpty()

    // Save current page ONLY if it's a media page (not settings)
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage < totalPages - 1) {
            Prefs.currentHomePage = pagerState.currentPage
        }
    }

    // Whenever the pager's current page changes and it represents a project page,
    // update the view model's selected project.
    LaunchedEffect(pagerState.currentPage, state.projects) {
        if (state.projects.isNotEmpty() && pagerState.currentPage < state.projects.size) {
            val newlySelectedProject = state.projects[pagerState.currentPage]
            onAction(MainMediaAction.UpdateSelectedProject(newlySelectedProject))
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
                        selectedSpace = state.space,
                        spaceList = state.spaces,
                        projects = state.projects,
                        selectedProject = state.project,
                        onProjectSelected = { project ->
                            // Update selected project and close drawer
                            onAction(MainMediaAction.UpdateSelectedProject(project))
                            scope.launch {
                                drawerState.close()
                                // Navigate to the project's page
                                val projectIndex = state.projects.indexOf(project)
                                if (projectIndex >= 0) {
                                    pagerState.scrollToPage(projectIndex)
                                }
                            }
                        },
                        onNewFolderClick = onNewFolderClick@{
                            val spaceId = state.space?.id ?: return@onNewFolderClick
                            onNavigateToAddNewFolder(spaceId)
                        },
                        onSpaceSelected = { space ->
                            scope.launch { drawerState.close() }
                            onAction(MainMediaAction.UpdateSelectedSpace(space))
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
                                            if (state.projects.isEmpty()) 0 else currentProjectIndex
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
                                    state.space == null -> {
                                        scope.launch {
                                            onNavigateToSpaceSetup()
                                        }
                                    }

                                    state.projects.isEmpty() || state.project == null -> {
                                        val space = state.space
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
                                    state.space == null -> {
                                        scope.launch { onNavigateToSpaceSetup() }
                                    }

                                    state.projects.isEmpty() || state.project == null -> {
                                        val space = state.space
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
                                val firstProject = state.projects.firstOrNull()
                                MainMediaScreen(
                                    projectId = firstProject?.id ?: -1,
                                    state = state,
                                    onAction = onAction
                                )
                            }

                            in 1 until state.projects.size -> {
                                // Next project IDs (page - 1)
                                val currentProject = state.projects[page]
                                MainMediaScreen(
                                    projectId = currentProject.id,
                                    state = state,
                                    onAction = onAction,
                                )
                            }

                            totalPages - 1 -> {
                                // Always settings screen as the last page
                                SettingsScreen(
                                    onNavigateToSpaceList = onNavigateToSpaceList,
                                    onNavigateToArchivedFolders = {
                                        onNavigateToArchivedFolders(state.space?.id)
                                    },
                                    onNavigateToCache = onNavigateToCache,
                                    onNavigateToProofMode = {
                                        onAction(MainMediaAction.NavigateToProofModeSettings)
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
            state = MainMediaState(),
            onAction = {},
            onNavigateToAddNewFolder = {},
            onNavigateToCache = {},
            onAddMedia = {},
            onNavigateToPreview = {},
            onNavigateToSpaceSetup = {},
        )
    }
}
