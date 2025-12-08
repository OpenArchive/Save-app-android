package net.opendasharchive.openarchive.features.main.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.features.media.AddMediaType
import net.opendasharchive.openarchive.features.settings.ProofModeSettingsScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun SaveNavGraph(
    viewModel: HomeViewModel = koinViewModel(),
    onExit: () -> Unit,
    onNewFolder: () -> Unit,
    onFolderSelected: (Long) -> Unit,
    onAddMedia: (AddMediaType) -> Unit
) {

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
            entryProvider = entryProvider {
                entry<AppRoute.HomeRoute> {
                    HomeScreen(
                        viewModel = viewModel,
                        onExit = onExit,
                        onNewFolder = onNewFolder,
                        onFolderSelected = onFolderSelected,
                        onAddMedia = onAddMedia,
                        onNavigateToCache = {
                            navigator.navigateTo(AppRoute.MediaCacheRoute)
                        },
                        onNavigateToProofModeSettings = {
                            navigator.navigateTo(AppRoute.ProofModeSettings)
                        }
                    )
                }

                entry<AppRoute.ProofModeSettings> {
                    ProofModeSettingsScreen(
                        onNavigateBack = {
                            navigator.navigateBack()
                        }
                    )
                }

                entry<AppRoute.MediaCacheRoute> {
                    MediaCacheScreen() {
                        navigator.navigateBack()
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
