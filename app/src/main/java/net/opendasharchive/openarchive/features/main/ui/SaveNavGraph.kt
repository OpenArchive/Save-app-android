package net.opendasharchive.openarchive.features.main.ui

import android.content.Intent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
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
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.folders.AddFolderScreen
import net.opendasharchive.openarchive.features.folders.BrowseFolderScreen
import net.opendasharchive.openarchive.features.folders.CreateNewFolderScreen
import net.opendasharchive.openarchive.features.internetarchive.presentation.details.InternetArchiveDetailsScreen
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginScreen
import net.opendasharchive.openarchive.features.media.AddMediaType
import net.opendasharchive.openarchive.features.media.PreviewMediaScreen
import net.opendasharchive.openarchive.features.media.ReviewMediaScreen
import net.opendasharchive.openarchive.features.main.ui.HomeViewModel
import net.opendasharchive.openarchive.features.onboarding.OnboardingInstructionsScreen
import net.opendasharchive.openarchive.features.onboarding.OnboardingWelcomeScreen
import net.opendasharchive.openarchive.features.settings.FolderDetailScreen
import net.opendasharchive.openarchive.features.settings.FoldersScreen
import net.opendasharchive.openarchive.features.settings.ProofModeSettingsScreen
import net.opendasharchive.openarchive.features.settings.SpaceSetupSuccessScreen
import net.opendasharchive.openarchive.features.settings.license.SetupLicenseScreen
import net.opendasharchive.openarchive.features.settings.passcode.AppConfig
import net.opendasharchive.openarchive.features.settings.passcode.components.DefaultScaffold
import net.opendasharchive.openarchive.features.settings.passcode.passcode_entry.PasscodeEntryScreen
import net.opendasharchive.openarchive.features.settings.passcode.passcode_setup.PasscodeSetupScreen
import net.opendasharchive.openarchive.features.spaces.SpaceListScreen
import net.opendasharchive.openarchive.features.spaces.SpaceListViewModel
import net.opendasharchive.openarchive.features.spaces.SpaceSetupScreen
import net.opendasharchive.openarchive.services.snowbird.SnowbirdActivity
import net.opendasharchive.openarchive.services.webdav.WebDavScreen
import net.opendasharchive.openarchive.services.webdav.WebDavViewModel
import net.opendasharchive.openarchive.util.Prefs
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun SaveNavGraph(
    onExit: () -> Unit,
    onNewFolder: () -> Unit,
    onFolderSelected: (Long) -> Unit,
    onAddMedia: (AddMediaType) -> Unit
) {

    val context = LocalContext.current
    val navigator = rememberNavigator()

    SaveAppTheme {

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

                entry<AppRoute.HomeRoute> {
                    val homeViewModel = koinViewModel<HomeViewModel>()
                    HomeScreen(
                        homeViewModel = homeViewModel,
                        onExit = onExit,
                        onFolderSelected = onFolderSelected,
                        onAddMedia = onAddMedia,
                        onNavigateToCache = {
                            navigator.navigateTo(AppRoute.MediaCacheRoute)
                        },
                        onNavigateToProofModeSettings = {
                            navigator.navigateTo(AppRoute.ProofModeSettings)
                        },
                        onNavigateToPreview = { projectId ->
                            navigator.navigateTo(AppRoute.PreviewMediaRoute(projectId))
                        },
                        onNavigateToSpaceSetup = {
                            navigator.navigateTo(AppRoute.SpaceSetupRoute)
                        },
                        onNavigateToAddNewFolder = { spaceId ->
                            navigator.navigateTo(AppRoute.AddFolderRoute(spaceId))
                        },
                        onNavigateToSpaceList = {
                            navigator.navigateTo(AppRoute.SpaceListRoute("Hello World"))
                        },
                        onNavigateToArchivedFolders = { spaceId ->
                            navigator.navigateTo(AppRoute.FolderListRoute(spaceId = spaceId, showArchived = true))
                        }
                    )
                }

                entry<AppRoute.WelcomeRoute> {
                    OnboardingWelcomeScreen(
                        onGetStartedClick = {
                            navigator.navigateTo(AppRoute.InstructionsRoute)
                        }
                    )
                }

                entry<AppRoute.InstructionsRoute> {
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

                entry<AppRoute.SpaceSetupRoute> {
                    val context = LocalContext.current
                    val appConfig: AppConfig = koinInject()
                    val isInternetArchiveAllowed = !Space.has(Space.Type.INTERNET_ARCHIVE)

                    DefaultScaffold(
                        title = stringResource(id = R.string.space_setup_title),
                        onNavigateBack = { navigator.navigateBack() }
                    ) {
                        SpaceSetupScreen(
                            onWebDavClick = {
                                navigator.navigateTo(AppRoute.WebDavLoginRoute)
                            },
                            isInternetArchiveAllowed = isInternetArchiveAllowed,
                            onInternetArchiveClick = {
                                navigator.navigateTo(AppRoute.IALoginRoute)
                            },
                            isDwebEnabled = appConfig.isDwebEnabled,
                            onDwebClicked = {
                                context.startActivity(
                                    Intent(context, SnowbirdActivity::class.java)
                                )
                            }
                        )
                    }
                }

                entry<AppRoute.SpaceListRoute> { key ->
                    DefaultScaffold(
                        title = stringResource(id = R.string.pref_title_media_servers),
                        onNavigateBack = { navigator.navigateBack() }
                    ) {

                        SpaceListScreen(
                            onSpaceClicked = { id, type ->
                                when (type) {
                                    Space.Type.INTERNET_ARCHIVE -> {
                                        navigator.navigateTo(
                                            AppRoute.IADetailRoute(id)
                                        )
                                    }

                                    Space.Type.WEBDAV -> {
                                        navigator.navigateTo(
                                            AppRoute.WebDavDetailRoute(id)
                                        )
                                    }

                                    else -> Unit
                                }
                            },
                            onAddServerClicked = {
                                navigator.navigateTo(AppRoute.SpaceSetupRoute)
                            }
                        )
                    }
                }

                entry<AppRoute.WebDavLoginRoute> {

                    DefaultScaffold(
                        title = stringResource(id = R.string.private_server),
                        onNavigateBack = { navigator.navigateBack() }
                    ) {
                        val vm = koinViewModel<WebDavViewModel>()
                        WebDavScreen(
                            viewModel = vm,
                            onNavigateToLicenseSetup = { spaceId ->
                                navigator.navigateTo(
                                    AppRoute.SetupLicenseRoute(
                                        spaceId = spaceId,
                                        isEditing = false,
                                        spaceType = Space.Type.WEBDAV
                                    )
                                )
                            },
                            onNavigateBack = { navigator.navigateBack() }
                        )
                    }
                }

                entry<AppRoute.WebDavDetailRoute> { route ->
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
                        WebDavScreen(
                            onNavigateToLicenseSetup = { spaceId ->
                                navigator.navigateTo(
                                    AppRoute.SetupLicenseRoute(
                                        spaceId = spaceId,
                                        isEditing = route.spaceId != WebDavViewModel.ARG_VAL_NEW_SPACE,
                                        spaceType = Space.Type.WEBDAV
                                    )
                                )
                            },
                            onNavigateBack = { navigator.navigateBack() }
                        )
                    }
                }

                entry<AppRoute.IALoginRoute> {
                    DefaultScaffold(
                        title = stringResource(id = R.string.internet_archive),
                        onNavigateBack = { navigator.navigateBack() }
                    ) {
                        InternetArchiveLoginScreen(
                            onLoginSuccess = { spaceId ->
                                navigator.navigateTo(
                                    AppRoute.SetupLicenseRoute(
                                        spaceId = spaceId,
                                        isEditing = false,
                                        spaceType = Space.Type.INTERNET_ARCHIVE
                                    )
                                )
                            },
                            onCancel = { navigator.navigateBack() }
                        )
                    }
                }

                entry<AppRoute.SetupLicenseRoute> { route ->
                    val titleRes =
                        if (route.spaceType == Space.Type.INTERNET_ARCHIVE) R.string.internet_archive else R.string.private_server
                    val context = LocalContext.current
                    DefaultScaffold(
                        title = stringResource(id = titleRes),
                        onNavigateBack = { navigator.navigateBack() },
                        showNavigationIcon = false
                    ) {
                        SetupLicenseScreen(
                            onNext = {
                                val message =
                                    if (route.spaceType == Space.Type.WEBDAV) {
                                        context.getString(R.string.you_have_successfully_connected_to_a_private_server)
                                    } else {
                                        context.getString(R.string.you_have_successfully_connected_to_the_internet_archive)
                                    }
                                navigator.navigateTo(
                                    AppRoute.SpaceSetupSuccessRoute(
                                        message = message,
                                        spaceType = route.spaceType
                                    )
                                )
                            },
                            onCancel = { navigator.navigateBack() }
                        )
                    }
                }

                entry<AppRoute.SpaceSetupSuccessRoute> {
                    DefaultScaffold(
                        title = stringResource(id = R.string.space_setup_success_title),
                        onNavigateBack = { navigator.navigateBack() },
                        showNavigationIcon = false
                    ) {
                        SpaceSetupSuccessScreen(
                            onNavigateToMain = {
                                Prefs.didCompleteOnboarding = true
                                navigator.navigateAndClear(AppRoute.HomeRoute)
                            }
                        )
                    }
                }

                entry<AppRoute.IADetailRoute> {
                    DefaultScaffold(
                        title = stringResource(id = R.string.internet_archive),
                        onNavigateBack = { navigator.navigateBack() }
                    ) {
                        InternetArchiveDetailsScreen(
                            onNavigateBack = { navigator.navigateBack() }
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
                                onFolderSelected(projectId)
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
                                onFolderSelected(projectId)
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
                            onRequestAddMore = { onAddMedia(AddMediaType.GALLERY) },
                            onPickMedia = { type -> onAddMedia(type) },
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
            }
        )
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
