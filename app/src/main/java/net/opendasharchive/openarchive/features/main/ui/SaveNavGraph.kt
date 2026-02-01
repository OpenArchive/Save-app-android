package net.opendasharchive.openarchive.features.main.ui

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.domain.VaultType
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.core.navigation.LocalResultEventBus
import net.opendasharchive.openarchive.core.navigation.ResultEventBus
import net.opendasharchive.openarchive.core.navigation.rememberResultStore
import net.opendasharchive.openarchive.db.sugar.Space
import net.opendasharchive.openarchive.features.core.dialog.DialogHost
import net.opendasharchive.openarchive.features.core.dialog.DialogStateManager
import net.opendasharchive.openarchive.features.folders.AddFolderScreen
import net.opendasharchive.openarchive.features.folders.BrowseFolderScreen
import net.opendasharchive.openarchive.features.folders.CreateNewFolderScreen
import net.opendasharchive.openarchive.features.folders.CreateNewFolderViewModel
import net.opendasharchive.openarchive.services.internetarchive.presentation.details.InternetArchiveDetailsScreen
import net.opendasharchive.openarchive.services.internetarchive.presentation.details.InternetArchiveDetailsViewModel
import net.opendasharchive.openarchive.services.internetarchive.presentation.login.InternetArchiveLoginScreen
import net.opendasharchive.openarchive.services.internetarchive.presentation.login.InternetArchiveLoginViewModel
import net.opendasharchive.openarchive.features.media.PreviewMediaAction
import net.opendasharchive.openarchive.features.media.PreviewMediaScreen
import net.opendasharchive.openarchive.features.media.PreviewMediaViewModel
import net.opendasharchive.openarchive.features.media.ReviewMediaScreen
import net.opendasharchive.openarchive.features.media.ReviewMediaViewModel
import net.opendasharchive.openarchive.features.media.camera.CameraScreenWrapper
import net.opendasharchive.openarchive.features.onboarding.OnboardingInstructionsScreen
import net.opendasharchive.openarchive.features.onboarding.OnboardingWelcomeScreen
import net.opendasharchive.openarchive.features.settings.FolderDetailScreen
import net.opendasharchive.openarchive.features.settings.FolderDetailViewModel
import net.opendasharchive.openarchive.features.settings.FoldersScreen
import net.opendasharchive.openarchive.features.settings.FoldersViewModel
import net.opendasharchive.openarchive.features.settings.ProofModeSettingsScreen
import net.opendasharchive.openarchive.features.settings.ProofModeSettingsViewModel
import net.opendasharchive.openarchive.features.settings.SpaceSetupSuccessScreen
import net.opendasharchive.openarchive.features.settings.SpaceSetupSuccessViewModel
import net.opendasharchive.openarchive.features.settings.license.SetupLicenseScreen
import net.opendasharchive.openarchive.features.settings.license.SetupLicenseViewModel
import net.opendasharchive.openarchive.features.settings.passcode.PasscodeFlowState
import net.opendasharchive.openarchive.features.settings.passcode.components.DefaultScaffold
import net.opendasharchive.openarchive.features.settings.passcode.passcode_setup.PasscodeSetupScreen
import net.opendasharchive.openarchive.features.settings.passcode.passcode_setup.PasscodeSetupViewModel
import net.opendasharchive.openarchive.features.spaces.SpaceListScreen
import net.opendasharchive.openarchive.features.spaces.SpaceListViewModel
import net.opendasharchive.openarchive.features.spaces.SpaceSetupScreen
import net.opendasharchive.openarchive.features.spaces.SpaceSetupViewModel
import net.opendasharchive.openarchive.services.webdav.presentation.detail.WebDavDetailScreen
import net.opendasharchive.openarchive.services.webdav.presentation.detail.WebDavDetailViewModel
import net.opendasharchive.openarchive.services.webdav.presentation.login.WebDavLoginScreen
import net.opendasharchive.openarchive.services.webdav.presentation.login.WebDavLoginViewModel
import net.opendasharchive.openarchive.util.Prefs
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun SaveNavGraph(
    dialogManager: DialogStateManager,
    navigator: Navigator
) {
    val resultBus = remember { ResultEventBus() }
    val resultStore = rememberResultStore()
    val passcodeFlowState: PasscodeFlowState = koinInject()

    val currentRoute = navigator.backstack.lastOrNull()
    AppLogger.d("Navigation", "Current route: $currentRoute")
    // LaunchedEffect restarts whenever currentRoute changes
    LaunchedEffect(currentRoute) {
        val isPasscodeFlow = currentRoute is AppRoute.PasscodeEntryRoute ||
            currentRoute is AppRoute.PasscodeSetupRoute
        passcodeFlowState.setActive(isPasscodeFlow)

        when (currentRoute) {
            is AppRoute.CameraRoute -> {
                // We are at camera route
                AppLogger.d("Navigation", "At camera route")
            }

            else -> Unit
        }
    }


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

                    entry<AppRoute.HomeRoute> { route ->

                        val viewModel = koinViewModel<HomeViewModel> {
                            parametersOf(navigator, route)
                        }

                        HomeScreen(viewModel)
                    }

                    entry<AppRoute.WelcomeRoute> { route ->

                        OnboardingWelcomeScreen(
                            onGetStartedClick = {
                                navigator.navigateTo(AppRoute.InstructionsRoute)
                            }
                        )
                    }

                    entry<AppRoute.InstructionsRoute> { route ->

                        OnboardingInstructionsScreen(
                            onDone = {
                                Prefs.didCompleteOnboarding = true
                                navigator.navigateAndClear(AppRoute.HomeRoute)
                            },
                        )
                    }

                    entry<AppRoute.SpaceSetupRoute> { route ->

                        val viewModel = koinViewModel<SpaceSetupViewModel> {
                            parametersOf(navigator, route)
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

                    entry<AppRoute.SpaceListRoute> { route ->

                        val viewModel: SpaceListViewModel = koinViewModel {
                            parametersOf(navigator, route)
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

                    entry<AppRoute.WebDavLoginRoute> { route ->

                        val viewModel = koinViewModel<WebDavLoginViewModel> {
                            parametersOf(navigator, route)
                        }

                        DefaultScaffold(
                            title = stringResource(id = R.string.private_server),
                            onNavigateBack = { navigator.navigateBack() }
                        ) {

                            WebDavLoginScreen(
                                viewModel = viewModel
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
                            )
                        }
                    }

                    entry<AppRoute.IALoginRoute> { route ->

                        val viewModel = koinViewModel<InternetArchiveLoginViewModel> {
                            parametersOf(navigator, route)
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

                    entry<AppRoute.SetupLicenseRoute> { route ->

                        val viewModel = koinViewModel<SetupLicenseViewModel> {
                            parametersOf(navigator, route)
                        }

                        val titleRes = when (route.spaceType) {
                            VaultType.INTERNET_ARCHIVE -> R.string.internet_archive
                            VaultType.PRIVATE_SERVER -> R.string.private_server
                            VaultType.DWEB_STORAGE -> R.string.dweb_title
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

                    entry<AppRoute.SpaceSetupSuccessRoute> { route ->

                        val viewModel = koinViewModel<SpaceSetupSuccessViewModel> {
                            parametersOf(navigator, route)
                        }

                        DefaultScaffold(
                            title = stringResource(id = R.string.space_setup_success_title),
                            onNavigateBack = { navigator.navigateBack() },
                            showNavigationIcon = false
                        ) {
                            SpaceSetupSuccessScreen(
                                viewModel = viewModel,
                                onNavigateBack = {
                                    resultBus.sendResult(
                                        resultKey = "refresh_spaces",
                                        result = true
                                    )
                                }
                            )
                        }
                    }

                    entry<AppRoute.IADetailRoute> { route ->

                        val viewModel = koinViewModel<InternetArchiveDetailsViewModel> {
                            parametersOf(navigator, route)
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

                    entry<AppRoute.CreateNewFolderRoute> { route ->
                        val viewModel = koinViewModel<CreateNewFolderViewModel>() {
                            parametersOf(navigator, route)
                        }
                        DefaultScaffold(
                            title = stringResource(id = R.string.create_a_new_folder),
                            onNavigateBack = { navigator.navigateBack() }
                        ) {
                            CreateNewFolderScreen(
                                viewModel = viewModel
                            )
                        }
                    }

                    entry<AppRoute.FolderListRoute> { route ->

                        val viewModel = koinViewModel<FoldersViewModel> {
                            parametersOf(navigator, route)
                        }

                        DefaultScaffold(
                            title = stringResource(
                                id = if (route.showArchived) R.string.archived_folders else R.string.folders
                            ),
                            onNavigateBack = { navigator.navigateBack() }
                        ) {
                            FoldersScreen(
                                viewModel = viewModel,
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

                    entry<AppRoute.FolderDetailRoute> { route ->

                        val viewModel = koinViewModel<FolderDetailViewModel> {
                            parametersOf(navigator, route)
                        }

                        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                        DefaultScaffold(
                            title = uiState.folderName,
                            onNavigateBack = { navigator.navigateBack() }
                        ) {
                            FolderDetailScreen(
                                viewModel = viewModel,
                            )
                        }
                    }

                    entry<AppRoute.PreviewMediaRoute> { route ->

                        val viewModel = koinViewModel<PreviewMediaViewModel> {
                            parametersOf(navigator, route)
                        }

                        DefaultScaffold(
                            title = stringResource(id = R.string.preview_media),
                            onNavigateBack = { navigator.navigateBack() },
                            actions = {
                                TextButton(
                                    onClick = {
                                        viewModel.onAction(PreviewMediaAction.UploadAll)
                                    },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text(stringResource(R.string.action_upload))
                                }
                            }
                        ) {
                            PreviewMediaScreen(
                                viewModel = viewModel,
                            )
                        }
                    }

                    entry<AppRoute.ReviewMediaRoute> { route ->

                        val viewModel = koinViewModel<ReviewMediaViewModel> {
                            parametersOf(navigator, route)
                        }

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
                                viewModel = viewModel
                            )
                        }
                    }

                    entry<AppRoute.ProofModeSettings> { route ->
                        val viewModel = koinViewModel<ProofModeSettingsViewModel> {
                            parametersOf(navigator, route)
                        }

                        DefaultScaffold(
                            title = stringResource(id = R.string.proofmode),
                            onNavigateBack = { navigator.navigateBack() }
                        ) {
                            ProofModeSettingsScreen(viewModel = viewModel)
                        }
                    }

                    entry<AppRoute.MediaCacheRoute> {
                        MediaCacheScreen {
                            navigator.navigateBack()
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


class LoggingNavEntryDecorator<T : Any> : NavEntryDecorator<T>(
    decorate = { entry ->
        LaunchedEffect(entry.contentKey) {
            AppLogger.d("EzzioNavigation", "Navigated to: ${entry.contentKey}")
            // Analytics logging
            //analytics.logScreenView(entry.key.toString())
        }
        entry.Content()
    },
    onPop = { contentKey ->
        AppLogger.d("EzzioNavigation", "Popped: $contentKey")
    }
)
