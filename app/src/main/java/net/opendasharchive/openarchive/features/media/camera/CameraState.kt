package net.opendasharchive.openarchive.features.media.camera

import android.net.Uri
import androidx.camera.core.ImageCapture

data class CameraState(
    val captureMode: CameraCaptureMode = CameraCaptureMode.PHOTO,
    val flashMode: Int = ImageCapture.FLASH_MODE_OFF,
    val isFlashSupported: Boolean = false,
    val isFrontCamera: Boolean = false,
    val showGrid: Boolean = false,
    val isRecording: Boolean = false,
    val recordingStartTime: Long? = null,
    val capturedItems: List<CapturedItem> = emptyList(),
    val showPreview: Boolean = false,
    val currentPreviewItem: CapturedItem? = null
)

data class CapturedItem(
    val uri: Uri,
    val type: CameraCaptureMode,
    val timestamp: Long = System.currentTimeMillis()
)