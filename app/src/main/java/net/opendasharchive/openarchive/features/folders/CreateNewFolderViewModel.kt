package net.opendasharchive.openarchive.features.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.UiText
import java.util.Date

data class CreateNewFolderState(
    val folderName: String = "",
    val isValid: Boolean = false,
    val error: UiText? = null,
    // Creative Commons License state
    val ccEnabled: Boolean = false,
    val allowRemix: Boolean = false,
    val requireShareAlike: Boolean = false,
    val allowCommercial: Boolean = false,
    val cc0Enabled: Boolean = false,
    val licenseUrl: String? = null
)

sealed interface CreateNewFolderAction {
    data class UpdateFolderName(val name: String) : CreateNewFolderAction
    data object CreateFolder : CreateNewFolderAction
    data object Cancel : CreateNewFolderAction
    // Creative Commons License actions
    data class UpdateCcEnabled(val enabled: Boolean) : CreateNewFolderAction
    data class UpdateAllowRemix(val allowed: Boolean) : CreateNewFolderAction
    data class UpdateRequireShareAlike(val required: Boolean) : CreateNewFolderAction
    data class UpdateAllowCommercial(val allowed: Boolean) : CreateNewFolderAction
    data class UpdateCc0Enabled(val enabled: Boolean) : CreateNewFolderAction
}

sealed interface CreateNewFolderEvent {
    data class NavigateBackWithResult(val projectId: Long) : CreateNewFolderEvent
    data class ShowSuccessDialog(val projectId: Long) : CreateNewFolderEvent
    data object NavigateBackCanceled : CreateNewFolderEvent
    data class ShowError(val message: UiText) : CreateNewFolderEvent
}

class CreateNewFolderViewModel : ViewModel() {

    companion object {
        private const val SPECIAL_CHARS = ".*[\\\\/*\\s]"
    }

    private val _uiState = MutableStateFlow(CreateNewFolderState())
    val uiState: StateFlow<CreateNewFolderState> = _uiState.asStateFlow()

    private val _events = Channel<CreateNewFolderEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: CreateNewFolderAction) {
        when (action) {
            is CreateNewFolderAction.UpdateFolderName -> {
                _uiState.update {
                    it.copy(
                        folderName = action.name,
                        isValid = action.name.trim().isNotEmpty(),
                        error = null
                    )
                }
            }

            is CreateNewFolderAction.CreateFolder -> {
                createFolder()
            }

            is CreateNewFolderAction.Cancel -> {
                viewModelScope.launch {
                    _events.send(CreateNewFolderEvent.NavigateBackCanceled)
                }
            }

            is CreateNewFolderAction.UpdateCcEnabled -> {
                _uiState.update { currentState ->
                    if (action.enabled) {
                        // When CC is enabled, start fresh with no options selected
                        currentState.copy(
                            ccEnabled = true,
                            cc0Enabled = false,
                            allowRemix = false,
                            requireShareAlike = false,
                            allowCommercial = false,
                            licenseUrl = null
                        )
                    } else {
                        // When CC is disabled, reset all other CC options
                        currentState.copy(
                            ccEnabled = false,
                            allowRemix = false,
                            requireShareAlike = false,
                            allowCommercial = false,
                            cc0Enabled = false,
                            licenseUrl = null
                        )
                    }
                }
                generateAndUpdateLicense()
            }

            is CreateNewFolderAction.UpdateAllowRemix -> {
                _uiState.update { currentState ->
                    currentState.copy(
                        allowRemix = action.allowed,
                        cc0Enabled = if (action.allowed) false else currentState.cc0Enabled,
                        requireShareAlike = if (!action.allowed) false else currentState.requireShareAlike
                    )
                }
                generateAndUpdateLicense()
            }

            is CreateNewFolderAction.UpdateRequireShareAlike -> {
                _uiState.update { currentState ->
                    currentState.copy(
                        requireShareAlike = action.required,
                        cc0Enabled = if (action.required) false else currentState.cc0Enabled
                    )
                }
                generateAndUpdateLicense()
            }

            is CreateNewFolderAction.UpdateAllowCommercial -> {
                _uiState.update { currentState ->
                    currentState.copy(
                        allowCommercial = action.allowed,
                        cc0Enabled = if (action.allowed) false else currentState.cc0Enabled
                    )
                }
                generateAndUpdateLicense()
            }

            is CreateNewFolderAction.UpdateCc0Enabled -> {
                _uiState.update { currentState ->
                    if (action.enabled) {
                        // When CC0 is enabled, disable CC and reset all other options
                        currentState.copy(
                            cc0Enabled = true,
                            allowRemix = false,
                            requireShareAlike = false,
                            allowCommercial = false
                        )
                    } else {
                        currentState.copy(cc0Enabled = false)
                    }
                }
                generateAndUpdateLicense()
            }
        }
    }

    private fun createFolder() {
        val name = _uiState.value.folderName.trim()

        if (name.isBlank()) {
            return
        }

        if (name.matches(SPECIAL_CHARS.toRegex())) {
            viewModelScope.launch {
                _events.send(
                    CreateNewFolderEvent.ShowError(
                        UiText.StringResource(net.opendasharchive.openarchive.R.string.please_do_not_include_special_characters_in_the_name)
                    )
                )
            }
            return
        }

        val space = Space.current ?: return

        if (space.hasProject(name)) {
            viewModelScope.launch {
                _events.send(
                    CreateNewFolderEvent.ShowError(
                        UiText.StringResource(net.opendasharchive.openarchive.R.string.folder_name_already_exists)
                    )
                )
            }
            return
        }

        val license = _uiState.value.licenseUrl ?: space.license

        val project = Project(name, Date(), space.id, licenseUrl = license)
        project.save()

        viewModelScope.launch {
            _events.send(CreateNewFolderEvent.ShowSuccessDialog(project.id))
        }
    }

    private fun generateAndUpdateLicense() {
        val currentState = _uiState.value
        val newLicense = generateLicenseUrl(
            ccEnabled = currentState.ccEnabled,
            allowRemix = currentState.allowRemix,
            requireShareAlike = currentState.requireShareAlike,
            allowCommercial = currentState.allowCommercial,
            cc0Enabled = currentState.cc0Enabled
        )

        _uiState.update { it.copy(licenseUrl = newLicense) }
    }

    private fun generateLicenseUrl(
        ccEnabled: Boolean,
        allowRemix: Boolean,
        requireShareAlike: Boolean,
        allowCommercial: Boolean,
        cc0Enabled: Boolean
    ): String? {
        if (!ccEnabled) return null

        if (cc0Enabled) {
            return "https://creativecommons.org/publicdomain/zero/1.0/"
        }

        val parts = mutableListOf<String>()

        if (!allowCommercial) parts.add("nc")
        if (!allowRemix) {
            parts.add("nd")
        } else if (requireShareAlike) {
            parts.add("sa")
        }

        val suffix = if (parts.isEmpty()) "" else "-${parts.joinToString("-")}"
        return "https://creativecommons.org/licenses/by$suffix/4.0/"
    }

    fun navigateBackWithResult(projectId: Long) {
        viewModelScope.launch {
            _events.send(CreateNewFolderEvent.NavigateBackWithResult(projectId))
        }
    }
}
