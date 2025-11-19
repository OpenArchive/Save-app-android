package net.opendasharchive.openarchive.features.media.camera

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
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
        _state.value = _state.value.copy(captureMode = mode)
    }
    
    fun toggleFlashMode() {
        val currentFlashMode = _state.value.flashMode
        val newFlashMode = when (currentFlashMode) {
            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
            else -> ImageCapture.FLASH_MODE_OFF
        }
        _state.value = _state.value.copy(flashMode = newFlashMode)
    }
    
    fun updateFlashSupport(isSupported: Boolean) {
        _state.value = _state.value.copy(isFlashSupported = isSupported)
    }
    
    fun toggleCamera() {
        _state.value = _state.value.copy(isFrontCamera = !_state.value.isFrontCamera)
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
            
            currentRecording = videoCapture.output
                .prepareRecording(context, fileOutputOptions)
                .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                    when (recordEvent) {
                        is VideoRecordEvent.Start -> {
                            _state.value = _state.value.copy(isRecording = true)
                            AppLogger.d("Video recording started")
                        }
                        is VideoRecordEvent.Finalize -> {
                            _state.value = _state.value.copy(isRecording = false)
                            
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
                                val error = Exception("Video recording failed: ${recordEvent.error}")
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
    
    fun hidePreview() {
        _state.value = _state.value.copy(
            showPreview = false,
            currentPreviewItem = null
        )
    }
    
    fun confirmCapture(item: CapturedItem): List<Uri> {
        return if (_state.value.capturedItems.contains(item)) {
            listOf(item.uri)
        } else {
            emptyList()
        }
    }
    
    fun retakeCapture(item: CapturedItem) {
        val updatedItems = _state.value.capturedItems.filter { it != item }
        _state.value = _state.value.copy(
            capturedItems = updatedItems,
            showPreview = false,
            currentPreviewItem = null
        )
        
        // For FileProvider URIs, we'll need to track the actual file paths differently
        // For now, rely on cache cleanup mechanisms
        AppLogger.d("Marked file for removal: ${item.uri}")
    }
    
    fun getAllCapturedUris(): List<Uri> {
        return _state.value.capturedItems.map { it.uri }
    }
    
    fun clearCaptures() {
        // Clear state - files in cache will be cleaned up by system
        AppLogger.d("Clearing ${_state.value.capturedItems.size} captured items")
        _state.value = CameraState()
    }
    
    override fun onCleared() {
        super.onCleared()
        stopVideoRecording()
        clearCaptures()
    }
}