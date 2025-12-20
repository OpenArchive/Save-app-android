package net.opendasharchive.openarchive.features.main.ui

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import net.opendasharchive.openarchive.core.navigation.LocalResultEventBus
import net.opendasharchive.openarchive.core.navigation.ResultEventBus
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.dialog.DialogHost
import net.opendasharchive.openarchive.features.core.dialog.DialogStateManager
import net.opendasharchive.openarchive.features.folders.AddFolderScreen
import net.opendasharchive.openarchive.features.folders.BrowseFolderScreen
import net.opendasharchive.openarchive.features.folders.CreateNewFolderScreen
import net.opendasharchive.openarchive.features.internetarchive.presentation.details.InternetArchiveDetailsScreen
import net.opendasharchive.openarchive.features.internetarchive.presentation.details.InternetArchiveDetailsViewModel
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginScreen
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginViewModel
import net.opendasharchive.openarchive.features.media.PreviewMediaScreen
import net.opendasharchive.openarchive.features.media.ReviewMediaScreen
import net.opendasharchive.openarchive.features.onboarding.OnboardingInstructionsScreen
import net.opendasharchive.openarchive.features.onboarding.OnboardingWelcomeScreen
import net.opendasharchive.openarchive.features.settings.FolderDetailScreen
import net.opendasharchive.openarchive.features.settings.FoldersScreen
import net.opendasharchive.openarchive.features.settings.ProofModeSettingsScreen
import net.opendasharchive.openarchive.features.settings.SpaceSetupSuccessScreen
import net.opendasharchive.openarchive.features.media.camera.CameraScreenWrapper
import net.opendasharchive.openarchive.features.settings.SpaceSetupSuccessViewModel
import net.opendasharchive.openarchive.features.settings.license.SetupLicenseScreen
import net.opendasharchive.openarchive.features.settings.license.SetupLicenseViewModel
import net.opendasharchive.openarchive.features.settings.passcode.components.DefaultScaffold
import net.opendasharchive.openarchive.features.settings.passcode.passcode_entry.PasscodeEntryScreen
import net.opendasharchive.openarchive.features.settings.passcode.passcode_setup.PasscodeSetupScreen
import net.opendasharchive.openarchive.features.spaces.SpaceListScreen
import net.opendasharchive.openarchive.features.spaces.SpaceListViewModel
import net.opendasharchive.openarchive.features.spaces.SpaceSetupScreen
import net.opendasharchive.openarchive.features.spaces.SpaceSetupViewModel
import net.opendasharchive.openarchive.services.webdav.detail.WebDavDetailScreen
import net.opendasharchive.openarchive.services.webdav.detail.WebDavDetailViewModel
import net.opendasharchive.openarchive.services.webdav.login.WebDavLoginScreen
import net.opendasharchive.openarchive.services.webdav.login.WebDavLoginViewModel
import net.opendasharchive.openarchive.util.Prefs
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun SaveNavGraph(
    dialogManager: DialogStateManager = koinInject()
) {

    val context = LocalContext.current
    val navigator = rememberNavigator()
    val resultBus = remember { ResultEventBus() }

    SaveAppTheme {

        DialogHost(dialogManager)

        CompositionLocalProvider(LocalResultEventBus provides resultBus) {
            NavDisplay(
            modifier = Modifier.fillMaxSize(),
            backStack = navigator.backstack,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
                remember { LoggingNavEntryDecorator() }
            ),
            transitionSpec = {
                // Slide in from right when navigating forward
                slideInHorizontally(initialOffsetX = { it }) togetherWith
                        slideOutHorizontally(targetOffsetX = { -it })
            },
            popTransitionSpec = {
                // Slide in from left when navigating back
                slideInHorizontally(initialOffsetX = { -it }) togetherWith
                        slideOutHorizontally(targetOffsetX = { it })
            },
            predictivePopTransitionSpec = {
                // Slide in from left when navigating back
                slideInHorizontally(initialOffsetX = { -it }) togetherWith
                        slideOutHorizontally(targetOffsetX = { it })
            },
            entryProvider = entryProvider {

                entry<AppRoute.HomeRoute> { key ->

                    val viewModel = koinViewModel<HomeViewModel> {
                        parametersOf(navigator, key)
                    }

                    HomeScreen(
                        viewModel = viewModel,
                        invokeNavEvent = { event ->
                            when(event) {
                                is HomeNavigation.AddNewFolder -> {
                                    navigator.navigateTo(AppRoute.AddFolderRoute(event.spaceId))
                                }
                                is HomeNavigation.ArchivedFolders -> {
                                    navigator.navigateTo(
                                        AppRoute.FolderListRoute(
                                            spaceId = event.spaceId,
                                            showArchived = true
                                        )
                                    )
                                }
                                HomeNavigation.Cache -> navigator.navigateTo(AppRoute.MediaCacheRoute)
                                HomeNavigation.ProofMode -> navigator.navigateTo(AppRoute.ProofModeSettings)
                                HomeNavigation.SpaceList -> navigator.navigateTo(AppRoute.SpaceListRoute)
                                HomeNavigation.SpaceSetup ->  navigator.navigateTo(AppRoute.SpaceSetupRoute)
                                is HomeNavigation.PreviewMedia -> navigator.navigateTo(AppRoute.PreviewMediaRoute(event.projectId))
                                is HomeNavigation.Camera -> navigator.navigateTo(AppRoute.CameraRoute(event.projectId, event.config))
                            }
                        },
                    )
                }

                entry<AppRoute.WelcomeRoute> { key ->

                    OnboardingWelcomeScreen(
                        onGetStartedClick = {
                            navigator.navigateTo(AppRoute.InstructionsRoute)
                        }
                    )
                }

                entry<AppRoute.InstructionsRoute> { key ->

                    OnboardingInstructionsScreen(
                        onDone = {
                            Prefs.didCompleteOnboarding = true
                            navigator.navigateAndClear(AppRoute.HomeRoute)
                        },
                        onBackPressed = {
                            navigator.navigateBack()
                        }
                    )
                }

                entry<AppRoute.SpaceSetupRoute> { key ->

                    val viewModel = koinViewModel<SpaceSetupViewModel> {
                        parametersOf(navigator, key)
                    }

                    DefaultScaffold(
                        title = stringResource(id = R.string.space_setup_title),
                        onNavigateBack = { navigator.navigateBack() }
                    ) {
                        SpaceSetupScreen(
                            viewModel = viewModel,
                        )
                    }
                }

                entry<AppRoute.SpaceListRoute> { key ->

                    val viewModel: SpaceListViewModel = koinViewModel {
                        parametersOf(navigator, key)
                    }

                    DefaultScaffold(
                        title = stringResource(id = R.string.pref_title_media_servers),
                        onNavigateBack = { navigator.navigateBack() }
                    ) {

                        SpaceListScreen(
                            viewModel = viewModel,
                        )
                    }
                }

                entry<AppRoute.WebDavLoginRoute> { key ->

                    val viewModel = koinViewModel<WebDavLoginViewModel> {
                        parametersOf(navigator, key)
                    }

                    DefaultScaffold(
                        title = stringResource(id = R.string.private_server),
                        onNavigateBack = { navigator.navigateBack() }
                    ) {

                        WebDavLoginScreen(
                            viewModel = viewModel,
                            dialogManager = dialogManager,
                        )
                    }
                }

                entry<AppRoute.WebDavDetailRoute> { route ->

                    val viewModel = koinViewModel<WebDavDetailViewModel> {
                        parametersOf(navigator, route)
                    }

                    val existingName = remember(route.spaceId) {
                        Space.get(route.spaceId)?.let { space ->
                            when {
                                space.name.isNotBlank() -> space.name
                                space.friendlyName.isNotBlank() -> space.friendlyName
                                else -> null
                            }
                        }
                    }

                    DefaultScaffold(
                        title = existingName ?: stringResource(id = R.string.private_server),
                        onNavigateBack = { navigator.navigateBack() }
                    ) {
                        WebDavDetailScreen(
                            viewModel = viewModel,
                            dialogManager = dialogManager,
                        )
                    }
                }

                entry<AppRoute.IALoginRoute> { key ->

                    val viewModel = koinViewModel<InternetArchiveLoginViewModel> {
                        parametersOf(navigator, key)
                    }

                    DefaultScaffold(
                        title = stringResource(id = R.string.internet_archive),
                        onNavigateBack = { navigator.navigateBack() }
                    ) {
                        InternetArchiveLoginScreen(
                            viewModel = viewModel,
                        )
                    }
                }

                entry<AppRoute.SetupLicenseRoute> { key ->

                    val viewModel = koinViewModel<SetupLicenseViewModel> {
                        parametersOf(navigator, key)
                    }

                    val titleRes = when (key.spaceType) {
                        Space.Type.INTERNET_ARCHIVE -> R.string.internet_archive
                        Space.Type.WEBDAV -> R.string.private_server
                        Space.Type.RAVEN -> R.string.dweb_title
                    }

                    DefaultScaffold(
                        title = stringResource(id = titleRes),
                        onNavigateBack = { navigator.navigateBack() },
                        showNavigationIcon = false
                    ) {
                        SetupLicenseScreen(
                            viewModel = viewModel,
                        )
                    }
                }

                entry<AppRoute.SpaceSetupSuccessRoute> { key ->

                    val viewModel = koinViewModel<SpaceSetupSuccessViewModel> {
                        parametersOf(navigator, key)
                    }

                    DefaultScaffold(
                        title = stringResource(id = R.string.space_setup_success_title),
                        onNavigateBack = { navigator.navigateBack() },
                        showNavigationIcon = false
                    ) {
                        SpaceSetupSuccessScreen(
                            viewModel = viewModel,
                            onNavigateToMain = {
                                Prefs.didCompleteOnboarding = true
                                navigator.navigateAndClear(AppRoute.HomeRoute)
                            }
                        )
                    }
                }

                entry<AppRoute.IADetailRoute> { key ->

                    val viewModel = koinViewModel<InternetArchiveDetailsViewModel> {
                        parametersOf(navigator, key)
                    }

                    DefaultScaffold(
                        title = stringResource(id = R.string.internet_archive),
                        onNavigateBack = { navigator.navigateBack() }
                    ) {
                        InternetArchiveDetailsScreen(
                            viewModel = viewModel,
                            dialogManager = dialogManager,
                        )
                    }
                }

                entry<AppRoute.AddFolderRoute> {
                    AddFolderScreen(
                        onCreateFolder = {
                            navigator.navigateTo(AppRoute.CreateNewFolderRoute)
                        },
                        onBrowseFolders = {
                            navigator.navigateTo(AppRoute.BrowseExistingFoldersRoute)
                        },
                        onNavigateBack = { navigator.navigateBack() }
                    )
                }

                entry<AppRoute.BrowseExistingFoldersRoute> {
                    DefaultScaffold(
                        title = stringResource(id = R.string.browse_existing),
                        onNavigateBack = { navigator.navigateBack() }
                    ) {
                        BrowseFolderScreen(
                            onNavigateBackWithResult = { projectId ->
                                //onFolderSelected(projectId)
                                navigator.navigateBack()
                            }
                        )
                    }
                }

                entry<AppRoute.CreateNewFolderRoute> {
                    DefaultScaffold(
                        title = stringResource(id = R.string.create_a_new_folder),
                        onNavigateBack = { navigator.navigateBack() }
                    ) {
                        CreateNewFolderScreen(
                            onNavigateBackWithResult = { projectId ->
                                //onFolderSelected(projectId)
                                navigator.navigateBack()
                            },
                            onNavigateBackCanceled = { navigator.navigateBack() }
                        )
                    }
                }

                entry<AppRoute.FolderListRoute> { route ->
                    DefaultScaffold(
                        title = stringResource(
                            id = if (route.showArchived) R.string.archived_folders else R.string.folders
                        ),
                        onNavigateBack = { navigator.navigateBack() }
                    ) {
                        FoldersScreen(
                            onNavigateToFolderDetail = { projectId ->
                                navigator.navigateTo(AppRoute.FolderDetailRoute(projectId))
                            },
                            onNavigateToArchivedFolders = { spaceId ->
                                navigator.navigateTo(
                                    AppRoute.FolderListRoute(
                                        showArchived = true,
                                        spaceId = spaceId,
                                    )
                                )
                            }
                        )
                    }
                }

                entry<AppRoute.FolderDetailRoute> {
                    DefaultScaffold(
                        title = "Edit Folder",
                        onNavigateBack = { navigator.navigateBack() }
                    ) {
                        FolderDetailScreen(
                            onNavigateBack = { navigator.navigateBack() }
                        )
                    }
                }

                entry<AppRoute.PreviewMediaRoute> { route ->
                    DefaultScaffold(
                        title = stringResource(id = R.string.preview_media),
                        onNavigateBack = { navigator.navigateBack() }
                    ) {
                        PreviewMediaScreen(
                            onNavigateToReview = { media, selected, batchMode ->
                                val ids = media.map { it.id }.toLongArray()
                                val selectedIndex =
                                    selected?.let { media.indexOf(it) }?.takeIf { it >= 0 } ?: 0
                                navigator.navigateTo(
                                    AppRoute.ReviewMediaRoute(
                                        mediaIds = ids,
                                        selectedIdx = selectedIndex,
                                        batchMode = batchMode
                                    )
                                )
                            },
                            onRequestAddMore = {
                                //onAddMedia(AddMediaType.GALLERY)
                                               },
                            onPickMedia = { type ->
                                //onAddMedia(type)
                                          },
                            onShowBatchHint = { Prefs.batchHintShown = true },
                            onCloseScreen = { navigator.navigateBack() }
                        )
                    }
                }

                entry<AppRoute.ReviewMediaRoute> { route ->
                    DefaultScaffold(
                        title = stringResource(
                            id = if (route.batchMode) {
                                R.string.bulk_edit_media_info
                            } else {
                                R.string.edit_media_info
                            }
                        ),
                        onNavigateBack = { navigator.navigateBack() }
                    ) {
                        ReviewMediaScreen(
                            onNavigateBack = { navigator.navigateBack() }
                        )
                    }
                }

                entry<AppRoute.ProofModeSettings> {
                    ProofModeSettingsScreen(
                        onNavigateBack = {
                            navigator.navigateBack()
                        }
                    )
                }

                entry<AppRoute.MediaCacheRoute> {
                    MediaCacheScreen {
                        navigator.navigateBack()
                    }
                }

                entry<AppRoute.PasscodeSetupRoute> {
                    DefaultScaffold(
                        title = stringResource(id = R.string.set_passcode),
                        onNavigateBack = { navigator.navigateBack() }
                    ) {
                        PasscodeSetupScreen(
                            onPasscodeSet = { navigator.navigateBack() },
                            onCancel = { navigator.navigateBack() }
                        )
                    }
                }

                entry<AppRoute.PasscodeEntryRoute> {
                    DefaultScaffold(
                        title = stringResource(id = R.string.enter_passcode),
                        onNavigateBack = { navigator.navigateBack() }
                    ) {
                        PasscodeEntryScreen(
                            onPasscodeSuccess = { navigator.navigateBack() },
                            onExit = { navigator.navigateBack() }
                        )
                    }
                }

                entry<AppRoute.CameraRoute> { route ->
                    CameraScreenWrapper(
                        config = route.config,
                        onCaptureComplete = { uris ->
                            // Send captured URIs via ResultEventBus
                            resultBus.sendResult(
                                resultKey = "camera_capture_result",
                                result = CameraCaptureResult(
                                    projectId = route.projectId,
                                    capturedUris = uris
                                )
                            )
                            navigator.navigateBack()
                        },
                        onCancel = {
                            navigator.navigateBack()
                        }
                    )
                }
            }
        )
        }
    }
}


class LoggingNavEntryDecorator<T : Any> : NavEntryDecorator<T>(
    decorate = { entry ->
        LaunchedEffect(entry.contentKey) {
            AppLogger.d("Navigation", "Navigated to: ${entry.contentKey}")
            // Analytics logging
            //analytics.logScreenView(entry.key.toString())
        }
        entry.Content()
    },
    onPop = { contentKey ->
        AppLogger.d("Navigation", "Popped: $contentKey")
    }
)
