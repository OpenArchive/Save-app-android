package net.opendasharchive.openarchive.core.navigation

/**
 * Constants for result keys used in [ResultEventBus] and [androidx.navigation3.runtime.result.ResultEffect].
 */
object NavigationResultKeys {
    const val QR_SCAN_RESULT = "qr_scan_result"
    const val CAMERA_CAPTURE_RESULT = "camera_capture_result"
    const val CAMERA_CAPTURE_RESULT_FROM_PREVIEW = "camera_capture_result_from_preview"
    const val SNOWBIRD_CAMERA_RESULT = "snowbird_camera_result"
    const val SHARED_MEDIA_IMPORT = "shared_media_import"
    const val REFRESH_SPACES = "refresh_spaces"
    const val REVIEW_MEDIA_SAVED = "review_media_saved"
    const val FOLDER_CREATED = "folder_created"
}
