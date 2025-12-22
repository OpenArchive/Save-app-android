package net.opendasharchive.openarchive.features.media

import android.content.ContentResolver
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.main.ui.AppRoute
import net.opendasharchive.openarchive.features.main.ui.Navigator
import java.io.File

data class ReviewMediaState(
    val mediaList: List<Media> = emptyList(),
    val currentIndex: Int = 0,
    val isBatchMode: Boolean = false,
    val description: String = "",
    val location: String = "",
    val isFlagged: Boolean = false,
    val isLoading: Boolean = false,
    val counter: String = "",
    val showBackButton: Boolean = false,
    val showForwardButton: Boolean = false
) {
    val currentMedia: Media?
        get() = mediaList.getOrNull(currentIndex)

    val batchPreviewMedia: List<Media>
        get() = if (isBatchMode) mediaList.take(3) else emptyList()
}

sealed class ReviewMediaAction {
    data object NavigateBack : ReviewMediaAction()
    data object NavigatePrevious : ReviewMediaAction()
    data object NavigateNext : ReviewMediaAction()
    data object ToggleFlag : ReviewMediaAction()
    data class UpdateDescription(val value: String) : ReviewMediaAction()
    data class UpdateLocation(val value: String) : ReviewMediaAction()
    data object SaveAndFinish : ReviewMediaAction()
}

sealed class ReviewMediaEvent {
    data object NavigateBack : ReviewMediaEvent()
    data class ShowFlagHint(val isFlagged: Boolean) : ReviewMediaEvent()
}

class ReviewMediaViewModel(
    private val route: AppRoute.ReviewMediaRoute,
    private val navigator: Navigator,
    private val contentResolver: ContentResolver
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewMediaState())
    val uiState: StateFlow<ReviewMediaState> = _uiState.asStateFlow()

    private val _events = Channel<ReviewMediaEvent>()
    val events = _events.receiveAsFlow()

    init {
        loadMediaList()
    }

    private fun loadMediaList() {
        viewModelScope.launch {

            val mediaList = withContext(Dispatchers.IO) {
                route.mediaIds.toList().mapNotNull { id -> Media.get(id) }
            }

            if (mediaList.isEmpty()) {
                navigator.navigateBack()
                return@launch
            }

            _uiState.update { state ->
                state.copy(
                    mediaList = mediaList,
                    currentIndex = route.selectedIdx.coerceIn(0, mediaList.size - 1),
                    isBatchMode = route.batchMode,
                    isLoading = false
                )
            }

            refreshUI()
        }
    }

    fun onAction(action: ReviewMediaAction) {
        when (action) {
            ReviewMediaAction.NavigateBack -> handleNavigateBack()
            ReviewMediaAction.NavigatePrevious -> handleNavigatePrevious()
            ReviewMediaAction.NavigateNext -> handleNavigateNext()
            ReviewMediaAction.ToggleFlag -> handleToggleFlag()
            is ReviewMediaAction.UpdateDescription -> handleUpdateDescription(action.value)
            is ReviewMediaAction.UpdateLocation -> handleUpdateLocation(action.value)
            ReviewMediaAction.SaveAndFinish -> handleSaveAndFinish()
        }
    }

    private fun handleNavigateBack() {
        viewModelScope.launch {
            saveAllMedia()
            _events.send(ReviewMediaEvent.NavigateBack)
        }
    }

    private fun handleNavigatePrevious() {
        val currentIndex = _uiState.value.currentIndex
        if (currentIndex > 0) {
            _uiState.update { it.copy(currentIndex = currentIndex - 1) }
            refreshUI()
        }
    }

    private fun handleNavigateNext() {
        val currentIndex = _uiState.value.currentIndex
        val maxIndex = _uiState.value.mediaList.size - 1
        if (currentIndex < maxIndex) {
            _uiState.update { it.copy(currentIndex = currentIndex + 1) }
            refreshUI()
        }
    }

    private fun handleToggleFlag() {
        viewModelScope.launch {
            val state = _uiState.value
            val newFlagState = !state.isFlagged

            // Show hint only once
            if (newFlagState) {
                _events.send(ReviewMediaEvent.ShowFlagHint(newFlagState))
            }

            if (state.isBatchMode) {
                withContext(Dispatchers.IO) {
                    state.mediaList.forEach { media ->
                        media.flag = newFlagState
                    }
                }
            } else {
                withContext(Dispatchers.IO) {
                    state.currentMedia?.flag = newFlagState
                }
            }

            saveAllMedia()
            updateFlagState()
        }
    }

    private fun handleUpdateDescription(value: String) {
        viewModelScope.launch {
            val state = _uiState.value

            _uiState.update { it.copy(description = value) }

            withContext(Dispatchers.IO) {
                if (state.isBatchMode) {
                    state.mediaList.forEach { media ->
                        media.description = value
                    }
                } else {
                    state.currentMedia?.description = value
                }
            }

            saveAllMedia()
        }
    }

    private fun handleUpdateLocation(value: String) {
        viewModelScope.launch {
            val state = _uiState.value

            _uiState.update { it.copy(location = value) }

            withContext(Dispatchers.IO) {
                if (state.isBatchMode) {
                    state.mediaList.forEach { media ->
                        media.location = value
                    }
                } else {
                    state.currentMedia?.location = value
                }
            }

            saveAllMedia()
        }
    }

    private fun handleSaveAndFinish() {
        viewModelScope.launch {
            saveAllMedia()
            _events.send(ReviewMediaEvent.NavigateBack)
        }
    }

    private fun refreshUI() {
        viewModelScope.launch {
            val state = _uiState.value

            if (state.isBatchMode) {
                refreshBatchMode()
            } else {
                refreshSingleMode()
            }

            updateNavigationButtons()
            updateFlagState()
            updateCounter()
        }
    }

    private suspend fun refreshBatchMode() {
        val state = _uiState.value
        val firstMedia = state.mediaList.firstOrNull()

        // Check if all descriptions/locations are the same
        var commonDescription = firstMedia?.description
        var commonLocation = firstMedia?.location

        withContext(Dispatchers.IO) {
            for (media in state.mediaList) {
                if (media.description != commonDescription) {
                    commonDescription = null
                }
                if (media.location != commonLocation) {
                    commonLocation = null
                }

                if (commonDescription == null && commonLocation == null) {
                    break
                }
            }
        }

        _uiState.update {
            it.copy(
                description = commonDescription ?: "",
                location = commonLocation ?: ""
            )
        }
    }

    private suspend fun refreshSingleMode() {
        val state = _uiState.value
        val currentMedia = state.currentMedia ?: return

        val description = withContext(Dispatchers.IO) {
            currentMedia.description
        }

        val currentLocation = withContext(Dispatchers.IO) {
            currentMedia.location
        }

        val location = if (currentLocation.isNullOrEmpty()) {
            extractLocationFromExif(currentMedia) ?: ""
        } else {
            currentLocation
        }

        _uiState.update {
            it.copy(
                description = description,
                location = location
            )
        }
    }

    private fun updateNavigationButtons() {
        val state = _uiState.value
        _uiState.update {
            it.copy(
                showBackButton = !state.isBatchMode && state.currentIndex > 0,
                showForwardButton = !state.isBatchMode && state.currentIndex < state.mediaList.size - 1
            )
        }
    }

    private fun updateFlagState() {
        viewModelScope.launch {
            val state = _uiState.value

            val isFlagged = withContext(Dispatchers.IO) {
                var flagged = state.currentMedia?.flag ?: false

                if (state.isBatchMode && flagged) {
                    // Only show flagged if all are flagged
                    if (state.mediaList.any { !it.flag }) {
                        flagged = false
                    }
                }

                flagged
            }

            _uiState.update { it.copy(isFlagged = isFlagged) }
        }
    }

    private fun updateCounter() {
        val state = _uiState.value
        val counter = if (state.isBatchMode) {
            "${state.mediaList.size}"
        } else {
            "${state.currentIndex + 1}/${state.mediaList.size}"
        }

        _uiState.update { it.copy(counter = counter) }
    }

    private suspend fun saveAllMedia() {
        withContext(Dispatchers.IO) {
            _uiState.value.mediaList.forEach { media ->
                media.licenseUrl = media.project?.licenseUrl ?: media.space?.license

                if (media.sStatus == Media.Status.New) {
                    media.sStatus = Media.Status.Local
                }

                media.save()
            }
        }
    }

    private suspend fun extractLocationFromExif(media: Media): String? {
        if (!media.mimeType.startsWith("image")) {
            return null
        }

        return withContext(Dispatchers.IO) {
            try {
                val exif: ExifInterface? = when {
                    media.fileUri != null -> {
                        try {
                            contentResolver.openInputStream(media.fileUri)?.use { inputStream ->
                                ExifInterface(inputStream)
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }
                    !media.originalFilePath.isNullOrEmpty() -> {
                        val file = File(media.originalFilePath)
                        if (file.exists()) {
                            ExifInterface(file.absolutePath)
                        } else {
                            null
                        }
                    }
                    else -> null
                }

                if (exif != null) {
                    val latLong = FloatArray(2)
                    if (exif.getLatLong(latLong)) {
                        val latitude = latLong[0].toDouble()
                        val longitude = latLong[1].toDouble()
                        String.format("%.6f, %.6f", latitude, longitude)
                    } else {
                        null
                    }
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}
