package net.opendasharchive.openarchive.features.media

import android.content.ContentResolver
import androidx.exifinterface.media.ExifInterface
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
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.domain.Evidence
import net.opendasharchive.openarchive.core.domain.EvidenceStatus
import net.opendasharchive.openarchive.core.repositories.MediaRepository
import net.opendasharchive.openarchive.core.repositories.ProjectRepository
import net.opendasharchive.openarchive.core.repositories.SpaceRepository
import net.opendasharchive.openarchive.features.core.UiColor
import net.opendasharchive.openarchive.features.core.UiImage
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.dialog.DialogStateManager
import net.opendasharchive.openarchive.features.core.dialog.showDialog
import net.opendasharchive.openarchive.features.main.ui.AppRoute
import net.opendasharchive.openarchive.features.main.ui.Navigator
import net.opendasharchive.openarchive.util.Prefs
import java.io.File

data class ReviewMediaState(
    val mediaList: List<Evidence> = emptyList(),
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
    val currentMedia: Evidence?
        get() = mediaList.getOrNull(currentIndex)

    val batchPreviewMedia: List<Evidence>
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

}

class ReviewMediaViewModel(
    private val route: AppRoute.ReviewMediaRoute,
    private val navigator: Navigator,
    private val contentResolver: ContentResolver,
    private val dialogManager: DialogStateManager,
    private val mediaRepository: MediaRepository,
    private val projectRepository: ProjectRepository,
    private val spaceRepository: SpaceRepository
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

            val mediaItems = mutableListOf<Evidence>()
            route.mediaIds.forEach { id ->
                mediaRepository.getEvidence(id)?.let { mediaItems.add(it) }
            }
            val mediaList = mediaItems.toList()

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
            navigator.navigateBack()
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
                showFlagHint(newFlagState)
            }

            val updatedMediaList = state.mediaList.map { evidence ->
                if (state.isBatchMode || evidence.id == state.currentMedia?.id) {
                    evidence.copy(isFlagged = newFlagState)
                } else {
                    evidence
                }
            }

            _uiState.update { it.copy(mediaList = updatedMediaList) }
            saveAllMedia()
            updateFlagState()
        }
    }

    private fun showFlagHint(flagged: Boolean) {
        if (Prefs.flagHintShown) {
            return
        }

        dialogManager.showDialog {
            title = UiText.Resource(R.string.popup_flag_title)
            message = UiText.Resource(R.string.popup_flag_desc)
            icon = UiImage.DrawableResource(R.drawable.ic_flag_selected)
            iconColor = UiColor.Resource(R.color.orange_light)
            positiveButton {
                text = UiText.Resource(R.string.lbl_got_it)
            }
        }
        Prefs.flagHintShown = true

    }

    private fun handleUpdateDescription(value: String) {
        viewModelScope.launch {

            _uiState.update { state ->
                val updatedMediaList = state.mediaList.map { evidence ->
                    if (state.isBatchMode || evidence.id == state.currentMedia?.id) {
                        evidence.copy(description = value)
                    } else {
                        evidence
                    }
                }
                state.copy(
                    description = value,
                    mediaList = updatedMediaList
                )
            }

            saveAllMedia()
        }
    }

    private fun handleUpdateLocation(value: String) {
        viewModelScope.launch {

            _uiState.update { state ->
                val updatedMediaList = state.mediaList.map { evidence ->
                    if (state.isBatchMode || evidence.id == state.currentMedia?.id) {
                        evidence.copy(location = value)
                    } else {
                        evidence
                    }
                }
                state.copy(
                    location = value,
                    mediaList = updatedMediaList
                )
            }

            saveAllMedia()
        }
    }

    private fun handleSaveAndFinish() {
        viewModelScope.launch {
            saveAllMedia()
            navigator.navigateBack()
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

        val description = currentMedia.description
        val currentLocation = currentMedia.location

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

            var flagged = state.currentMedia?.isFlagged ?: false

            if (state.isBatchMode && flagged) {
                // Only show flagged if all are flagged
                if (state.mediaList.any { !it.isFlagged }) {
                    flagged = false
                }
            }

            _uiState.update { it.copy(isFlagged = flagged) }
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
        val mediaList = _uiState.value.mediaList
        mediaList.forEach { evidence ->
            var finalEvidence = evidence
            if (finalEvidence.licenseUrl == null) {
                val archive = projectRepository.getProject(evidence.archiveId)
                val license = archive?.licenseUrl ?: archive?.vaultId?.let {
                    spaceRepository.getSpaceById(it)?.licenseUrl
                }
                finalEvidence = finalEvidence.copy(licenseUrl = license)
            }

            if (finalEvidence.status == EvidenceStatus.NEW) {
                finalEvidence = finalEvidence.copy(status = EvidenceStatus.LOCAL)
            }

            mediaRepository.updateEvidence(finalEvidence)
        }
    }

    private suspend fun extractLocationFromExif(media: Evidence): String? {
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
