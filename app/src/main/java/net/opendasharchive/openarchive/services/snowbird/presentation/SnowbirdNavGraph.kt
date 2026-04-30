package net.opendasharchive.openarchive.services.snowbird.presentation

import android.content.Intent
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.navigation.NavigationResultKeys
import net.opendasharchive.openarchive.core.navigation.ResultEventBus
import net.opendasharchive.openarchive.features.main.ui.AppRoute
import net.opendasharchive.openarchive.features.main.ui.Navigator
import net.opendasharchive.openarchive.features.settings.passcode.components.DefaultScaffold
import net.opendasharchive.openarchive.services.snowbird.presentation.dashboard.SnowbirdDashboardScreen
import net.opendasharchive.openarchive.services.snowbird.presentation.dashboard.SnowbirdDashboardViewModel
import net.opendasharchive.openarchive.services.snowbird.presentation.file.SnowbirdFileAction
import net.opendasharchive.openarchive.services.snowbird.presentation.file.SnowbirdFileListScreen
import net.opendasharchive.openarchive.services.snowbird.presentation.file.SnowbirdFileViewModel
import net.opendasharchive.openarchive.services.snowbird.presentation.group.SnowbirdCreateGroupScreen
import net.opendasharchive.openarchive.services.snowbird.presentation.group.SnowbirdCreateGroupViewModel
import net.opendasharchive.openarchive.services.snowbird.presentation.group.SnowbirdGroupListScreen
import net.opendasharchive.openarchive.services.snowbird.presentation.group.SnowbirdGroupListViewModel
import net.opendasharchive.openarchive.services.snowbird.presentation.group.SnowbirdJoinGroupScreen
import net.opendasharchive.openarchive.services.snowbird.presentation.group.SnowbirdJoinGroupViewModel
import net.opendasharchive.openarchive.services.snowbird.presentation.group.SnowbirdShareScreen
import net.opendasharchive.openarchive.services.snowbird.presentation.group.SnowbirdShareViewModel
import net.opendasharchive.openarchive.services.snowbird.presentation.qrscanner.QRScannerScreen
import net.opendasharchive.openarchive.services.snowbird.presentation.repo.SnowbirdRepoAction
import net.opendasharchive.openarchive.services.snowbird.presentation.repo.SnowbirdRepoListScreen
import net.opendasharchive.openarchive.services.snowbird.presentation.repo.SnowbirdRepoViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Snowbird feature navigation entries.
 * Groups all DWeb/Snowbird screen entries for use in [net.opendasharchive.openarchive.features.main.ui.SaveNavGraph].
 */
fun EntryProviderScope<AppRoute>.snowbirdEntries(
    navigator: Navigator
) {
    entry<AppRoute.SnowbirdDashboardRoute> { route ->
        val viewModel = koinViewModel<SnowbirdDashboardViewModel> {
            parametersOf(navigator, route)
        }

        DefaultScaffold(
            title = stringResource(id = R.string.dweb_storage),
            onNavigateBack = { navigator.navigateBack() }
        ) {
            SnowbirdDashboardScreen(
                viewModel = viewModel
            )
        }
    }

    entry<AppRoute.SnowbirdCreateGroupRoute> { route ->
        val viewModel = koinViewModel<SnowbirdCreateGroupViewModel> {
            parametersOf(navigator, route)
        }

        DefaultScaffold(
            title = stringResource(id = R.string.dweb_create_group),
            onNavigateBack = { navigator.navigateBack() }
        ) {
            SnowbirdCreateGroupScreen(
                viewModel = viewModel
            )
        }
    }

    entry<AppRoute.SnowbirdJoinGroupRoute> { route ->
        val viewModel = koinViewModel<SnowbirdJoinGroupViewModel> {
            parametersOf(navigator, route)
        }

        DefaultScaffold(
            title = stringResource(id = R.string.dweb_join_group),
            onNavigateBack = { navigator.navigateBack() }
        ) {
            SnowbirdJoinGroupScreen(
                viewModel = viewModel
            )
        }
    }

    entry<AppRoute.SnowbirdGroupListRoute> { route ->
        val viewModel = koinViewModel<SnowbirdGroupListViewModel> {
            parametersOf(navigator, route)
        }

        DefaultScaffold(
            title = stringResource(id = R.string.dweb_my_groups),
            onNavigateBack = { navigator.navigateBack() }
        ) {
            SnowbirdGroupListScreen(
                viewModel = viewModel
            )
        }
    }

    entry<AppRoute.SnowbirdShareRoute> { route ->
        val viewModel = koinViewModel<SnowbirdShareViewModel> {
            parametersOf(navigator, route)
        }
        val context = androidx.compose.ui.platform.LocalContext.current
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        DefaultScaffold(
            title = stringResource(id = R.string.dweb_share_group),
            onNavigateBack = { navigator.navigateBack() },
            actions = {
                TextButton(
                    enabled = state.qrContent.isNotBlank() && !state.isLoading,
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, state.groupName)
                            putExtra(Intent.EXTRA_TEXT, state.qrContent)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, state.groupName))
                    }
                ) {
                    Text(text = "Share", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        ) {
            SnowbirdShareScreen(
                viewModel = viewModel,
                onShareQr = { qrContent, groupName ->
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, groupName)
                        putExtra(Intent.EXTRA_TEXT, qrContent)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, groupName))
                }
            )
        }
    }

    entry<AppRoute.SnowbirdRepoListRoute> { route ->
        val viewModel = koinViewModel<SnowbirdRepoViewModel> {
            parametersOf(navigator, route)
        }

        DefaultScaffold(
            title = stringResource(id = R.string.dweb_repos),
            onNavigateBack = { navigator.navigateBack() },
            actions = {
                IconButton(
                    onClick = {
                        viewModel.onAction(SnowbirdRepoAction.RefreshGroupContent)
                    }
                ) {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(R.drawable.refresh),
                        contentDescription = stringResource(R.string.refresh)
                    )
                }
            }
        ) {
            SnowbirdRepoListScreen(
                viewModel = viewModel
            )
        }
    }

    entry<AppRoute.SnowbirdFileListRoute> { route ->
        val viewModel = koinViewModel<SnowbirdFileViewModel> {
            parametersOf(navigator, route)
        }

        DefaultScaffold(
            title = stringResource(id = R.string.dweb_files),
            onNavigateBack = { viewModel.onAction(SnowbirdFileAction.NavigateBack) },
            actions = {
                if (route.canWrite) {
                    IconButton(onClick = { viewModel.onAction(SnowbirdFileAction.ShowContentPicker) }) {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = "Add Files"
                        )
                    }
                }
            }
        ) {
            SnowbirdFileListScreen(
                viewModel = viewModel
            )
        }
    }

    entry<AppRoute.SnowbirdQRScannerRoute> { route ->
        QRScannerScreen(
            onQrCodeScanned = { result ->
                ResultEventBus.sendResult(
                    resultKey = NavigationResultKeys.QR_SCAN_RESULT,
                    result = result
                )
                navigator.navigateBack()
            },
            onNavigateBack = {
                navigator.navigateBack()
            }
        )
    }
}
