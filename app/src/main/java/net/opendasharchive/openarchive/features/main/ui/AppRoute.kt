package net.opendasharchive.openarchive.features.main.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed class AppRoute(val route: String): NavKey {

    @Serializable
    data object WelcomeRoute : AppRoute("welcome")

    @Serializable
    data object InstructionsRoute : AppRoute("instructions")

    @Serializable
    data object HomeRoute : AppRoute("home")

    @Serializable
    data object SpaceSetupRoute : AppRoute("space_setup")

    @Serializable
    data object WebDavLoginRoute: AppRoute("webdav_login")

    @Serializable
    data object IALoginRoute: AppRoute("ia_login")

    @Serializable
    data object SetupLicenseRoute : AppRoute("setup_license")

    @Serializable
    data object SpaceSetupSuccessRoute : AppRoute("space_setup_success")

    @Serializable
    data object SpaceListRoute : AppRoute("space_list")

    @Serializable
    data object WebDavDetailRoute : AppRoute("webdav_detail")

    @Serializable
    data object IADetailRoute : AppRoute("ia_detail")

    @Serializable
    data object AddFolderRoute : AppRoute("add_folder")

    @Serializable
    data object CreateNewFolderRoute : AppRoute("create_new_folder")

    @Serializable
    data object BrowseExistingFoldersRoute : AppRoute("browse_existing_folders")

    @Serializable
    data object FolderListRoute : AppRoute("folder_list")

    @Serializable
    data object FolderDetailRoute : AppRoute("folder_detail")

    @Serializable
    data object ProofModeSettings: AppRoute("proof_mode_settings")

    @Serializable
    data object PreviewMediaRoute : AppRoute("preview_media")

    @Serializable
    data object ReviewMediaRoute : AppRoute("review_media")

    @Serializable
    data object PasscodeSetupRoute : AppRoute("passcode_setup")

    @Serializable
    data object PasscodeEntryRoute : AppRoute("passcode_entry")

    @Serializable
    data object MediaCacheRoute : AppRoute("media_cache")
}