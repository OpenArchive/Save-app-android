package net.opendasharchive.openarchive.features.settings.license

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.settings.CreativeCommonsLicenseManager

class SetupLicenseViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val spaceId: Long = savedStateHandle.get<Long>("spaceId") ?: -1L
    private val isEditing: Boolean = savedStateHandle.get<Boolean>("isEditing") ?: false
    private val spaceType: Space.Type = savedStateHandle.get<Space.Type>("space_type") ?: Space.Type.WEBDAV

    private val _uiState =
        MutableStateFlow(SetupLicenseState(
            spaceId = spaceId,
            isEditing = isEditing,
            spaceType = spaceType
        ))
    val uiState: StateFlow<SetupLicenseState> = _uiState.asStateFlow()

    private val _events = Channel<SetupLicenseEvent>()
    val events = _events.receiveAsFlow()

    private var space: Space? = null

    init {
        loadSpace()
    }

    fun onAction(action: SetupLicenseAction) {
        when (action) {
            is SetupLicenseAction.UpdateServerName -> {
                _uiState.update { it.copy(serverName = action.serverName) }
                // Update space object but don't save yet - wait for Next button
                updateSpace { space ->
                    space.name = action.serverName
                }
            }

            is SetupLicenseAction.Next -> {
                // Save all changes (name + license) when user taps Next
                space?.let { currentSpace ->
                    val currentState = _uiState.value

                    // Only save nickname for WebDAV/private servers, not for Internet Archive
                    if (currentState.spaceType != Space.Type.INTERNET_ARCHIVE) {
                        currentSpace.name = currentState.serverName
                    }

                    currentSpace.license = currentState.licenseUrl

                    AppLogger.d("SetupLicenseViewModel", "Updating space - ID: ${currentSpace.id}, type: ${currentState.spaceType}, name: '${currentSpace.name}', license: '${currentSpace.license}'")

                    // Save updates to existing space
                    currentSpace.save()

                    AppLogger.d("SetupLicenseViewModel", "Space saved successfully - ID: ${currentSpace.id}")
                }
                viewModelScope.launch {
                    _events.send(SetupLicenseEvent.NavigateNext)
                }
            }

            is SetupLicenseAction.Cancel -> {
                viewModelScope.launch {
                    _events.send(SetupLicenseEvent.NavigateBack)
                }
            }

            is SetupLicenseAction.UpdateCcEnabled -> {
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

            is SetupLicenseAction.UpdateAllowRemix -> {
                _uiState.update { currentState ->
                    currentState.copy(
                        allowRemix = action.allowed,
                        cc0Enabled = if (action.allowed) false else currentState.cc0Enabled,  // Disable CC0 if remix is enabled
                        requireShareAlike = if (!action.allowed) false else currentState.requireShareAlike  // Auto-disable ShareAlike when Remix is disabled
                    )
                }
                generateAndUpdateLicense()
            }

            is SetupLicenseAction.UpdateRequireShareAlike -> {
                _uiState.update { currentState ->
                    currentState.copy(
                        requireShareAlike = action.required,
                        cc0Enabled = if (action.required) false else currentState.cc0Enabled  // Disable CC0 if share alike is enabled
                    )
                }
                generateAndUpdateLicense()
            }

            is SetupLicenseAction.UpdateAllowCommercial -> {
                _uiState.update { currentState ->
                    currentState.copy(
                        allowCommercial = action.allowed,
                        cc0Enabled = if (action.allowed) false else currentState.cc0Enabled  // Disable CC0 if commercial is enabled
                    )
                }
                generateAndUpdateLicense()
            }

            is SetupLicenseAction.UpdateCc0Enabled -> {
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

    private fun loadSpace() {
        // Since we come directly from WebDavScreen/InternetArchiveLoginScreen where
        // Space.current is set, we can use it directly. This ensures we're updating
        // the SAME space that was just authenticated, not creating a new one.
        space = Space.current

        space?.let { currentSpace ->
            val licenseState = initializeLicenseState(currentSpace.license)
            _uiState.update { currentState ->
                currentState.copy(
                    serverName = currentSpace.name.orEmpty(),
                    ccEnabled = licenseState.ccEnabled,
                    allowRemix = licenseState.allowRemix,
                    requireShareAlike = licenseState.requireShareAlike,
                    allowCommercial = licenseState.allowCommercial,
                    cc0Enabled = licenseState.cc0Enabled,
                    licenseUrl = licenseState.licenseUrl
                )
            }
        }
    }

    private fun updateSpace(action: (Space) -> Unit) {
        space?.let(action)
    }

    private fun initializeLicenseState(currentLicense: String?): SetupLicenseState {
        val isCc0 = currentLicense?.contains("publicdomain/zero", true) ?: false
        val isCC = currentLicense?.contains("creativecommons.org/licenses", true) ?: false

        return if (isCc0) {
            // CC0 license detected
            SetupLicenseState(
                ccEnabled = true,
                cc0Enabled = true,
                allowRemix = false,
                allowCommercial = false,
                requireShareAlike = false,
                licenseUrl = currentLicense
            )
        } else if (isCC && currentLicense != null) {
            // Regular CC license detected
            SetupLicenseState(
                ccEnabled = true,
                cc0Enabled = false,
                allowRemix = !(currentLicense.contains("-nd", true)),
                allowCommercial = !(currentLicense.contains("-nc", true)),
                requireShareAlike = !(currentLicense.contains("-nd", true)) && currentLicense.contains("-sa", true),
                licenseUrl = currentLicense
            )
        } else {
            // No license
            SetupLicenseState(
                ccEnabled = false,
                cc0Enabled = false,
                allowRemix = false,  // Changed from true to fix auto-enable bug
                allowCommercial = false,
                requireShareAlike = false,
                licenseUrl = null
            )
        }
    }

    private fun generateAndUpdateLicense() {
        val currentState = _uiState.value
        val newLicense = CreativeCommonsLicenseManager.generateLicenseUrl(
            ccEnabled = currentState.ccEnabled,
            allowRemix = currentState.allowRemix,
            requireShareAlike = currentState.requireShareAlike,
            allowCommercial = currentState.allowCommercial,
            cc0Enabled = currentState.cc0Enabled
        )

        _uiState.update { it.copy(licenseUrl = newLicense) }
        // Update space object but don't save yet - wait for Next button
        // This prevents duplicate space records
        updateSpace { space ->
            space.license = newLicense
        }
    }
}

@Immutable
data class SetupLicenseState(
    val serverName: String = "",
    val spaceId: Long = -1L,
    val isEditing: Boolean = false,
    val spaceType: Space.Type = Space.Type.WEBDAV,
    // Creative Commons License state
    val ccEnabled: Boolean = false,
    val allowRemix: Boolean = false,
    val requireShareAlike: Boolean = false,
    val allowCommercial: Boolean = false,
    val cc0Enabled: Boolean = false,
    val licenseUrl: String? = null,
    val isLoading: Boolean = false
)

sealed interface SetupLicenseAction {
    data class UpdateServerName(val serverName: String) : SetupLicenseAction
    data object Next : SetupLicenseAction
    data object Cancel : SetupLicenseAction
    // Creative Commons License actions
    data class UpdateCcEnabled(val enabled: Boolean) : SetupLicenseAction
    data class UpdateAllowRemix(val allowed: Boolean) : SetupLicenseAction
    data class UpdateRequireShareAlike(val required: Boolean) : SetupLicenseAction
    data class UpdateAllowCommercial(val allowed: Boolean) : SetupLicenseAction
    data class UpdateCc0Enabled(val enabled: Boolean) : SetupLicenseAction
}

sealed interface SetupLicenseEvent {
    data object NavigateNext : SetupLicenseEvent
    data object NavigateBack : SetupLicenseEvent
}