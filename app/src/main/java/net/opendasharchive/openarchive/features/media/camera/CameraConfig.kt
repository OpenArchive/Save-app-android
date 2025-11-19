package net.opendasharchive.openarchive.features.media.camera

import java.io.Serializable

data class CameraConfig(
    val allowVideoCapture: Boolean = true,
    val allowPhotoCapture: Boolean = true,
    val allowMultipleCapture: Boolean = false,
    val enablePreview: Boolean = true,
    val showFlashToggle: Boolean = true,
    val showGridToggle: Boolean = true,
    val showCameraSwitch: Boolean = true,
    val initialMode: CameraCaptureMode = CameraCaptureMode.PHOTO,
    val useCleanFilenames: Boolean = false // When true, uses IMG_123.jpg instead of 20250119_143045.IMG_123.jpg
) : Serializable

enum class CameraCaptureMode : Serializable {
    PHOTO,
    VIDEO
}