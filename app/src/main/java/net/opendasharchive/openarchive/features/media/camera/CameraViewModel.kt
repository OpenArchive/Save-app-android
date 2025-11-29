package net.opendasharchive.openarchive.features.media.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.util.Utility
import java.io.File

class CameraViewModel : ViewModel() {

    private val _state = MutableStateFlow(CameraState())
    val state: StateFlow<CameraState> = _state.asStateFlow()

    private var currentRecording: Recording? = null

    fun updateCaptureMode(mode: CameraCaptureMode) {
        val currentFlashMode = _state.value.flashMode

        // When switching to video mode, convert AUTO flash to ON
        // (video mode doesn't support AUTO, only ON/OFF)
        val newFlashMode = if (mode == CameraCaptureMode.VIDEO &&
            currentFlashMode == ImageCapture.FLASH_MODE_AUTO) {
            ImageCapture.FLASH_MODE_ON
        } else {
            currentFlashMode
        }

        _state.value = _state.value.copy(
            captureMode = mode,
            flashMode = newFlashMode
        )
    }

    fun toggleFlashMode() {
        val currentFlashMode = _state.value.flashMode
        val currentCaptureMode = _state.value.captureMode

        // Video mode: Toggle between OFF and ON only (no AUTO)
        // Photo mode: Cycle through OFF, ON, and AUTO
        val newFlashMode = if (currentCaptureMode == CameraCaptureMode.VIDEO) {
            // Video mode: OFF <-> ON (skip AUTO)
            when (currentFlashMode) {
                ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                else -> ImageCapture.FLASH_MODE_OFF
            }
        } else {
            // Photo mode: OFF -> ON -> AUTO -> OFF
            when (currentFlashMode) {
                ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                else -> ImageCapture.FLASH_MODE_OFF
            }
        }

        _state.value = _state.value.copy(flashMode = newFlashMode)
    }

    fun updateFlashSupport(isSupported: Boolean) {
        _state.value = _state.value.copy(isFlashSupported = isSupported)
    }

    fun toggleCamera() {
        // When switching cameras, reset flash support and flash mode
        // Flash support will be updated to true by onFlashSupportChanged if the new camera has flash
        _state.value = _state.value.copy(
            isFrontCamera = !_state.value.isFrontCamera,
            isFlashSupported = false,
            flashMode = ImageCapture.FLASH_MODE_OFF
        )
    }

    fun toggleGrid() {
        _state.value = _state.value.copy(showGrid = !_state.value.showGrid)
    }

    fun capturePhoto(
        context: Context,
        imageCapture: ImageCapture,
        useCleanFilenames: Boolean = false,
        onSuccess: (Uri) -> Unit,
        onError: (Exception) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val filename = "IMG_${System.currentTimeMillis()}.jpg"
                val outputFile = if (useCleanFilenames) {
                    Utility.getOutputMediaFileByCacheNoTimestamp(context, filename)
                } else {
                    Utility.getOutputMediaFileByCache(context, filename)
                }
                
                if (outputFile == null) {
                    onError(Exception("Failed to create output file"))
                    return@launch
                }

                val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

                imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            // Use FileProvider URI like other camera implementations
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.provider",
                                outputFile
                            )
                            val capturedItem = CapturedItem(uri, CameraCaptureMode.PHOTO)

                            val updatedItems = _state.value.capturedItems + capturedItem
                            _state.value = _state.value.copy(
                                capturedItems = updatedItems,
                                showPreview = true,
                                currentPreviewItem = capturedItem
                            )

                            AppLogger.d("Photo captured successfully: $uri")
                            onSuccess(uri)
                        }

                        override fun onError(exception: ImageCaptureException) {
                            AppLogger.e("Photo capture failed", exception)
                            onError(exception)
                        }
                    }
                )
            } catch (e: Exception) {
                AppLogger.e("Error setting up photo capture", e)
                onError(e)
            }
        }
    }

    fun startVideoRecording(
        context: Context,
        videoCapture: androidx.camera.video.VideoCapture<androidx.camera.video.Recorder>,
        useCleanFilenames: Boolean = false,
        onSuccess: (Uri) -> Unit,
        onError: (Exception) -> Unit
    ) {
        if (_state.value.isRecording) {
            AppLogger.w("Video recording already in progress")
            return
        }

        try {
            val filename = "VID_${System.currentTimeMillis()}.mp4"
            val outputFile = if (useCleanFilenames) {
                Utility.getOutputMediaFileByCacheNoTimestamp(context, filename)
            } else {
                Utility.getOutputMediaFileByCache(context, filename)
            }
            
            if (outputFile == null) {
                onError(Exception("Failed to create output file"))
                return
            }

            val fileOutputOptions = FileOutputOptions.Builder(outputFile).build()

            val hasAudioPermission =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED

            var pendingRecording = videoCapture.output
                .prepareRecording(context, fileOutputOptions)

            if (hasAudioPermission) {
                pendingRecording = pendingRecording.withAudioEnabled()
            } else {
                AppLogger.w("RECORD_AUDIO permission not granted, recording without audio")
            }

            currentRecording =
                pendingRecording.start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                    when (recordEvent) {
                        is VideoRecordEvent.Start -> {
                            _state.value = _state.value.copy(
                                isRecording = true,
                                recordingStartTime = System.currentTimeMillis()
                            )
                            AppLogger.d("Video recording started")
                        }

                        is VideoRecordEvent.Finalize -> {
                            _state.value = _state.value.copy(
                                isRecording = false,
                                recordingStartTime = null
                            )

                            if (!recordEvent.hasError()) {
                                // Use FileProvider URI like other camera implementations
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.provider",
                                    outputFile
                                )
                                val capturedItem = CapturedItem(uri, CameraCaptureMode.VIDEO)

                                val updatedItems = _state.value.capturedItems + capturedItem
                                _state.value = _state.value.copy(
                                    capturedItems = updatedItems,
                                    showPreview = true,
                                    currentPreviewItem = capturedItem
                                )

                                AppLogger.d("Video captured successfully: $uri")
                                onSuccess(uri)
                            } else {
                                val error =
                                    Exception("Video recording failed: ${recordEvent.error}")
                                AppLogger.e("Video recording failed", error)
                                onError(error)
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            AppLogger.e("Error starting video recording", e)
            onError(e)
        }
    }

    fun stopVideoRecording() {
        if (_state.value.isRecording) {
            currentRecording?.stop()
            currentRecording = null
        }
    }

    fun showPreview(item: CapturedItem) {
        _state.value = _state.value.copy(
            showPreview = true,
            currentPreviewItem = item
        )
    }

    fun hidePreview(deleteFile: Boolean = false) {
        // When cancelling from preview (deleteFile=true), remove item and delete file
        // When hiding after confirm (deleteFile=false), just hide preview
        _state.value.currentPreviewItem?.let { item ->
            if (deleteFile) {
                // Remove from captured items
                val updatedItems = _state.value.capturedItems.filter { it != item }

                // Delete the file
                deleteFile(item.uri)
                AppLogger.d("Deleted cancelled capture: ${item.uri}")

                _state.value = _state.value.copy(
                    capturedItems = updatedItems,
                    showPreview = false,
                    currentPreviewItem = null
                )
            } else {
                // Just hide preview without deleting (file was confirmed)
                _state.value = _state.value.copy(
                    showPreview = false,
                    currentPreviewItem = null
                )
            }
        } ?: run {
            // No current preview item, just hide preview
            _state.value = _state.value.copy(
                showPreview = false,
                currentPreviewItem = null
            )
        }
    }

    fun confirmCapture(item: CapturedItem): List<Uri> {
        return if (_state.value.capturedItems.contains(item)) {
            listOf(item.uri)
        } else {
            emptyList()
        }
    }

    fun retakeCapture(item: CapturedItem) {
        // Delete the file being retaken
        deleteFile(item.uri)

        val updatedItems = _state.value.capturedItems.filter { it != item }
        _state.value = _state.value.copy(
            capturedItems = updatedItems,
            showPreview = false,
            currentPreviewItem = null
        )

        AppLogger.d("Deleted file for retake: ${item.uri}")
    }

    fun getAllCapturedUris(): List<Uri> {
        return _state.value.capturedItems.map { it.uri }
    }

    fun clearCaptures() {
        // Delete all captured files that weren't confirmed
        val itemsToDelete = _state.value.capturedItems
        AppLogger.d("Deleting ${itemsToDelete.size} unconfirmed captured items")

        itemsToDelete.forEach { item ->
            deleteFile(item.uri)
        }

        _state.value = CameraState()
    }

    override fun onCleared() {
        super.onCleared()
        stopVideoRecording()
        clearCaptures()
    }

    /**
     * Deletes a file from the given URI.
     * Handles both FileProvider URIs and file:// URIs.
     */
    private fun deleteFile(uri: Uri) {
        try {
            // Try to get the file from FileProvider URI
            val file = when (uri.scheme) {
                "content" -> {
                    // FileProvider URI - extract the file path
                    // The URI format is: content://package.provider/cache_path/filename
                    val path = uri.path
                    if (path != null) {
                        // Extract the actual file path after the authority
                        val segments = path.split("/")
                        if (segments.size >= 2) {
                            // Reconstruct the file path from cache directory
                            File(segments.drop(1).joinToString("/"))
                        } else null
                    } else null
                }

                "file" -> {
                    // Direct file URI
                    try {
                        uri.toFile()
                    } catch (e: Exception) {
                        null
                    }
                }

                else -> null
            }

            if (file != null && file.exists()) {
                val deleted = file.delete()
                if (deleted) {
                    AppLogger.d("Successfully deleted file: ${file.absolutePath}")
                } else {
                    AppLogger.w("Failed to delete file: ${file.absolutePath}")
                }
            } else {
                AppLogger.w("File not found or invalid URI: $uri")
            }
        } catch (e: Exception) {
            AppLogger.e("Error deleting file for URI: $uri", e)
        }
    }
}