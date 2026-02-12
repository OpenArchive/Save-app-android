package net.opendasharchive.openarchive.services.snowbird

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.media.AddMediaType
import net.opendasharchive.openarchive.services.snowbird.service.ServiceStatus
import net.opendasharchive.openarchive.services.snowbird.service.SnowbirdService
import net.opendasharchive.openarchive.services.snowbird.util.SnowbirdQRDecoder
import net.opendasharchive.openarchive.util.ProcessingTracker
import net.opendasharchive.openarchive.util.trackProcessing
import net.opendasharchive.openarchive.extensions.getQueryParameter

data class SnowbirdDashboardState(
    val isLoading: Boolean = false,
    val error: SnowbirdError? = null,
    val serverStatus: ServiceStatus = ServiceStatus.Stopped,
    val showContentPicker: Boolean = false
)

sealed interface SnowbirdDashboardAction {
    data object JoinGroupClick : SnowbirdDashboardAction
    data object CreateGroupClick : SnowbirdDashboardAction
    data object MyGroupsClick : SnowbirdDashboardAction
    data class ToggleServer(val enabled: Boolean) : SnowbirdDashboardAction
    data object ContentPickerDismissed : SnowbirdDashboardAction
    data class MediaPicked(val type: AddMediaType) : SnowbirdDashboardAction
    data class QRResultScanned(val result: String) : SnowbirdDashboardAction
    data class ImagePickedForQR(val uri: Uri, val context: Context) : SnowbirdDashboardAction
}

sealed interface SnowbirdDashboardEvent {
    data object NavigateToCreateGroup : SnowbirdDashboardEvent
    data object NavigateToGroupList : SnowbirdDashboardEvent
    data class NavigateToJoinGroup(val groupKey: String) : SnowbirdDashboardEvent
    data object NavigateToScanner : SnowbirdDashboardEvent
    data class ShowMessage(val message: UiText) : SnowbirdDashboardEvent
    data class ToggleServer(val enabled: Boolean) : SnowbirdDashboardEvent
}

class SnowbirdDashboardViewModel(
    private val processingTracker: ProcessingTracker = ProcessingTracker()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SnowbirdDashboardState())
    val uiState: StateFlow<SnowbirdDashboardState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SnowbirdDashboardEvent>()
    val events = _events.asSharedFlow()

    init {
        // Observe server status reactively
        SnowbirdService.serviceStatus
            .onEach { status ->
                _uiState.update { it.copy(serverStatus = status) }
            }
            .launchIn(viewModelScope)
    }

    fun onAction(action: SnowbirdDashboardAction) {
        when (action) {
            is SnowbirdDashboardAction.JoinGroupClick -> {
                _uiState.update { it.copy(showContentPicker = true) }
            }
            is SnowbirdDashboardAction.CreateGroupClick -> {
                viewModelScope.launch { _events.emit(SnowbirdDashboardEvent.NavigateToCreateGroup) }
            }
            is SnowbirdDashboardAction.MyGroupsClick -> {
                viewModelScope.launch { _events.emit(SnowbirdDashboardEvent.NavigateToGroupList) }
            }
            is SnowbirdDashboardAction.ToggleServer -> {
                viewModelScope.launch { _events.emit(SnowbirdDashboardEvent.ToggleServer(action.enabled)) }
            }
            is SnowbirdDashboardAction.ContentPickerDismissed -> {
                _uiState.update { it.copy(showContentPicker = false) }
            }
            is SnowbirdDashboardAction.MediaPicked -> {
                _uiState.update { it.copy(showContentPicker = false) }
                when (action.type) {
                    AddMediaType.CAMERA -> {
                        viewModelScope.launch { _events.emit(SnowbirdDashboardEvent.NavigateToScanner) }
                    }
                    AddMediaType.GALLERY -> {
                        // Fragment triggers gallery picker launcher
                    }
                    else -> Unit
                }
            }
            is SnowbirdDashboardAction.QRResultScanned -> {
                processScannedData(action.result)
            }
            is SnowbirdDashboardAction.ImagePickedForQR -> {
                processImageForQR(action.uri, action.context)
            }
        }
    }

    private fun processImageForQR(uri: Uri, context: Context) {
        viewModelScope.launch {
            processingTracker.trackProcessing("decode_qr") {
                _uiState.update { it.copy(isLoading = true) }
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()

                    if (bitmap != null) {
                        val qrContent = SnowbirdQRDecoder.decodeFromBitmap(bitmap)
                        if (qrContent != null) {
                            processScannedData(qrContent)
                        } else {
                            _events.emit(SnowbirdDashboardEvent.ShowMessage(UiText.Dynamic("No QR code found in the image.")))
                        }
                    } else {
                        _events.emit(SnowbirdDashboardEvent.ShowMessage(UiText.Dynamic("Could not load selected image.")))
                    }
                } catch (e: Exception) {
                    _events.emit(SnowbirdDashboardEvent.ShowMessage(UiText.Dynamic("Error processing image: ${e.message}")))
                } finally {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    private fun processScannedData(uriString: String) {
        val name = uriString.getQueryParameter("name")
        if (name == null) {
            viewModelScope.launch {
                _events.emit(SnowbirdDashboardEvent.ShowMessage(UiText.Dynamic("Unable to determine group name from QR code.")))
            }
            return
        }

        viewModelScope.launch {
            _events.emit(SnowbirdDashboardEvent.NavigateToJoinGroup(uriString))
        }
    }
}
