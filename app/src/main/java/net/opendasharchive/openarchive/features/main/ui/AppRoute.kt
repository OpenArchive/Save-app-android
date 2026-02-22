package net.opendasharchive.openarchive.features.main.ui

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import net.opendasharchive.openarchive.core.domain.VaultType
import net.opendasharchive.openarchive.core.navigation.NavigationResultKeys
import net.opendasharchive.openarchive.features.media.camera.CameraConfig

@Serializable
sealed class AppRoute(open val deeplink: String) : NavKey {

    @Serializable
    data object WelcomeRoute : AppRoute("welcome")

    @Serializable
    data object InstructionsRoute : AppRoute("instructions")

    @Serializable
    data object HomeRoute : AppRoute("home")

    @Serializable
    data object SpaceSetupRoute : AppRoute("space_setup")

    @Serializable
    data object WebDavLoginRoute : AppRoute("webdav_login")

    @Serializable
    data class WebDavDetailRoute(
        val spaceId: Long,
    ) : AppRoute("webdav_detail")

    @Serializable
    data object IALoginRoute : AppRoute("ia_login")

    @Serializable
    data class SetupLicenseRoute(
        val spaceId: Long,
        val spaceType: VaultType,
    ) : AppRoute("setup_license")

    @Serializable
    data class SpaceSetupSuccessRoute(
        val spaceType: VaultType
    ) : AppRoute("space_setup_success")

    @Serializable
    data object SpaceListRoute : AppRoute("space_list")

    @Serializable
    data class IADetailRoute(val spaceId: Long) : AppRoute("ia_detail")

    @Serializable
    data class AddFolderRoute(
        val spaceId: Long,
    ) : AppRoute("add_folder")

    @Serializable
    data object CreateNewFolderRoute : AppRoute("create_new_folder")

    @Serializable
    data object BrowseExistingFoldersRoute : AppRoute("browse_existing_folders")

    @Serializable
    data class FolderListRoute(
        val showArchived: Boolean,
        val spaceId: Long?,
    ) : AppRoute("folder_list")

    @Serializable
    data class FolderDetailRoute(val currentProjectId: Long) : AppRoute("folder_detail")

    @Serializable
    data object C2paSettings : AppRoute("c2pa_settings")

    @Serializable
    data class PreviewMediaRoute(val projectId: Long) : AppRoute("preview_media")

    @Serializable
    data class ReviewMediaRoute(
        val mediaIds: LongArray,
        val selectedIdx: Int = 0,
        val batchMode: Boolean = false
    ) : AppRoute("review_media")

    @Serializable
    data object PasscodeSetupRoute : AppRoute("passcode_setup")

    @Serializable
    data object PasscodeEntryRoute : AppRoute("passcode_entry")

    @Serializable
    data object MediaCacheRoute : AppRoute("media_cache")

    @Serializable
    data class CameraRoute(
        val projectId: Long,
        val config: CameraConfig,
        val resultKey: String = NavigationResultKeys.CAMERA_CAPTURE_RESULT
    ) : AppRoute("camera")

    // ── Storacha Route ──

    @Serializable
    data object StorachaRoute : AppRoute("storacha")

    // ── Snowbird Routes ──

    @Serializable
    data object SnowbirdDashboardRoute : AppRoute("snowbird_dashboard")

    @Serializable
    data object SnowbirdCreateGroupRoute : AppRoute("snowbird_create_group")

    @Serializable
    data class SnowbirdJoinGroupRoute(
        val groupKey: String
    ) : AppRoute("snowbird_join_group")

    @Serializable
    data object SnowbirdGroupListRoute : AppRoute("snowbird_group_list")

    @Serializable
    data class SnowbirdShareRoute(
        val groupKey: String,
    ) : AppRoute("snowbird_share")

    @Serializable
    data class SnowbirdRepoListRoute(
        val vaultId: Long,
        val groupKey: String
    ) : AppRoute("snowbird_repo_list")

    @Serializable
    data class SnowbirdFileListRoute(
        val archiveId: Long,
        val groupKey: String,
        val repoKey: String,
        val canWrite: Boolean = true
    ) : AppRoute("snowbird_file_list")

    @Serializable
    data object SnowbirdQRScannerRoute : AppRoute("snowbird_qr_scanner")
}

/**
 * Result data class for camera capture results passed via ResultEventBus.
 */
data class CameraCaptureResult(
    val projectId: Long,
    val capturedUris: List<Uri>
)
