package net.opendasharchive.openarchive.services.snowbird.presentation

import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.navigation.ResultEventBus
import net.opendasharchive.openarchive.features.main.ui.AppRoute
import net.opendasharchive.openarchive.features.main.ui.Navigator
import net.opendasharchive.openarchive.features.settings.passcode.components.DefaultScaffold
import net.opendasharchive.openarchive.services.snowbird.presentation.dashboard.SnowbirdDashboardScreen
import net.opendasharchive.openarchive.services.snowbird.presentation.dashboard.SnowbirdDashboardViewModel
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
import net.opendasharchive.openarchive.services.snowbird.presentation.repo.SnowbirdRepoListScreen
import net.opendasharchive.openarchive.services.snowbird.presentation.repo.SnowbirdRepoViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Snowbird feature navigation entries.
 * Groups all DWeb/Snowbird screen entries for use in [net.opendasharchive.openarchive.features.main.ui.SaveNavGraph].
 */
fun EntryProviderScope<AppRoute>.snowbirdEntries(
    navigator: Navigator,
    resultBus: ResultEventBus
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
                viewModel = viewModel,
                resultBus = resultBus
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

        DefaultScaffold(
            title = stringResource(id = R.string.dweb_share_group),
            onNavigateBack = { navigator.navigateBack() }
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
            onNavigateBack = { navigator.navigateBack() }
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
            onNavigateBack = { navigator.navigateBack() }
        ) {
            SnowbirdFileListScreen(
                viewModel = viewModel
            )
        }
    }

    entry<AppRoute.SnowbirdQRScannerRoute> { route ->
        QRScannerScreen(
            onQrCodeScanned = { result ->
                resultBus.sendResult(
                    resultKey = "qr_scan_result",
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
