package net.opendasharchive.openarchive.features.main.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import net.opendasharchive.openarchive.util.Prefs

class Navigator(
    initialBackstack: SnapshotStateList<AppRoute> = mutableListOf<AppRoute>(if (Prefs.didCompleteOnboarding) AppRoute.HomeRoute else AppRoute.WelcomeRoute).toMutableStateList()
) {
    var backstack: SnapshotStateList<AppRoute> by mutableStateOf(initialBackstack)
        private set

    fun navigateTo(route: AppRoute) {
        backstack.add(route)
    }

    fun navigateBack() {
        backstack.removeLastOrNull()
    }

    fun popBackTo(route: AppRoute, inclusive: Boolean = false) {
        val index = backstack.indexOfLast { it == route }
        if (index != -1) {
            val targetIndex = if (inclusive) index else index + 1
            if (targetIndex < backstack.size) {
                backstack.subList(targetIndex, backstack.size).clear()
            }
        }
    }

    fun navigateAndClear(route: AppRoute) {
        backstack.clear()
        backstack.add(route)
    }

    fun currentRoute(): AppRoute? {
        return backstack.lastOrNull()
    }

    companion object {
        fun saver(): Saver<Navigator, List<String>> = Saver(
            save = { navigator ->
                // Convert routes to strings for saving
                navigator.backstack.map { route ->
                    when (route) {
                        is AppRoute.WelcomeRoute -> "welcome"
                        is AppRoute.InstructionsRoute -> "instructions"
                        is AppRoute.HomeRoute -> "home"
                        is AppRoute.ProofModeSettings -> "proof_mode_settings"
                        is AppRoute.MediaCacheRoute -> "media_cache"
                        is AppRoute.AddFolderRoute -> "add_folder"
                        is AppRoute.BrowseExistingFoldersRoute -> "browse_existing_folders"
                        is AppRoute.CreateNewFolderRoute -> "create_new_folder"
                        is AppRoute.FolderDetailRoute -> "folder_detail"
                        is AppRoute.FolderListRoute -> "folder_list"
                        is AppRoute.IADetailRoute -> "ia_detail"
                        is AppRoute.IALoginRoute -> "ia_login"
                        is AppRoute.PasscodeEntryRoute -> "passcode_entry"
                        is AppRoute.PasscodeSetupRoute -> "passcode_setup"
                        is AppRoute.PreviewMediaRoute -> "preview_media"
                        is AppRoute.ReviewMediaRoute -> "review_media"
                        is AppRoute.SetupLicenseRoute -> "setup_license"
                        is AppRoute.SpaceListRoute -> "space_list"
                        is AppRoute.SpaceSetupRoute -> "space_setup"
                        is AppRoute.SpaceSetupSuccessRoute -> "space_setup_success"
                        is AppRoute.WebDavDetailRoute -> "webdav_detail"
                        is AppRoute.WebDavLoginRoute -> "webdav_login"
                    }
                }.toMutableStateList()
            },
            restore = { savedList ->
                // Convert strings back to routes
                val routes = savedList.mapNotNull { savedRoute ->
                    when {
                        savedRoute == "welcome" -> AppRoute.WelcomeRoute
                        savedRoute == "instructions" -> AppRoute.InstructionsRoute
                        savedRoute == "home" -> AppRoute.HomeRoute
                        savedRoute == "proof_mode_settings" -> AppRoute.ProofModeSettings
                        savedRoute == "media_cache" -> AppRoute.MediaCacheRoute
                        else -> null
                    }
                }.toMutableStateList()

                Navigator(
                    if (routes.isEmpty()) {
                        val startDestination =
                            if (Prefs.didCompleteOnboarding) AppRoute.HomeRoute else AppRoute.WelcomeRoute
                        mutableStateListOf(startDestination)
                    } else {
                        routes.toMutableStateList()
                    }
                )
            }
        )
    }
}

@Composable
fun rememberNavigator(): Navigator {
    return rememberSaveable(saver = Navigator.saver()) {
        Navigator()
    }
}