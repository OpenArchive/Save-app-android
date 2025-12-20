package net.opendasharchive.openarchive.features.settings.license

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.main.data.SpaceRepository
import net.opendasharchive.openarchive.features.main.ui.AppRoute
import net.opendasharchive.openarchive.features.main.ui.Navigator
import net.opendasharchive.openarchive.features.settings.CreativeCommonsLicenseManager

class SetupLicenseViewModel(
    private val route: AppRoute.SetupLicenseRoute,
    private val navigator: Navigator,
    private val spaceRepository: SpaceRepository,
) : ViewModel() {


    private val _uiState = MutableStateFlow(
        SetupLicenseState(
            spaceId = route.spaceId,
            spaceType = route.spaceType
        )
    )
    val uiState: StateFlow<SetupLicenseState> = _uiState.asStateFlow()

    init {
        loadSpace()
    }

    fun onAction(action: SetupLicenseAction) {

        when (action) {

            is SetupLicenseAction.UpdateServerName -> {
                _uiState.update { it.copy(serverName = action.serverName) }
            }

            is SetupLicenseAction.Next -> onNext()

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

    private fun loadSpace() = viewModelScope.launch {
        // Since we come directly from WebDavScreen/InternetArchiveLoginScreen where
        // Space.current is set, we can use it directly. This ensures we're updating
        // the SAME space that was just authenticated, not creating a new one.
        val currentState = uiState.value

        val space = spaceRepository.getSpaceById(route.spaceId) ?: error("Space not found")

        val licenseState =
            initializeLicenseState(state = currentState, currentLicense = space.license)
        _uiState.update { currentState ->
            currentState.copy(
                space = space,
                serverName = space.name,
                ccEnabled = licenseState.ccEnabled,
                allowRemix = licenseState.allowRemix,
                requireShareAlike = licenseState.requireShareAlike,
                allowCommercial = licenseState.allowCommercial,
                cc0Enabled = licenseState.cc0Enabled,
                licenseUrl = licenseState.licenseUrl
            )
        }

    }

    private fun onNext() = viewModelScope.launch {
        val currentState = uiState.value
        val space = currentState.space ?: return@launch
        // Save all changes (name + license) when user taps Next


        // Only save nickname for WebDAV/private servers, not for Internet Archive
        if (currentState.spaceType != Space.Type.INTERNET_ARCHIVE) {
            space.name = currentState.serverName
        }

        space.license = currentState.licenseUrl

        AppLogger.d("Updating space - ID: ${space.id}, type: ${currentState.spaceType}, name: '${space.name}', license: '${space.license}'")

        // Save updates to existing space
        spaceRepository.updateSpace(route.spaceId, space)

        AppLogger.d("Space saved successfully - ID: ${space.id}")

        navigator.navigateTo(AppRoute.SpaceSetupSuccessRoute(currentState.spaceType))
    }

    private fun initializeLicenseState(
        state: SetupLicenseState,
        currentLicense: String?
    ): SetupLicenseState {
        val isCc0 = currentLicense?.contains("publicdomain/zero", true) ?: false
        val isCC = currentLicense?.contains("creativecommons.org/licenses", true) ?: false

        return if (isCc0) {
            // CC0 license detected
            state.copy(
                ccEnabled = true,
                cc0Enabled = true,
                allowRemix = false,
                allowCommercial = false,
                requireShareAlike = false,
                licenseUrl = currentLicense
            )
        } else if (isCC) {
            // Regular CC license detected
            state.copy(
                ccEnabled = true,
                cc0Enabled = false,
                allowRemix = !(currentLicense.contains("-nd", true)),
                allowCommercial = !(currentLicense.contains("-nc", true)),
                requireShareAlike = !(currentLicense.contains(
                    "-nd",
                    true
                )) && currentLicense.contains("-sa", true),
                licenseUrl = currentLicense
            )
        } else {
            // No license
            state.copy(
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
    }
}

@Immutable
data class SetupLicenseState(
    val spaceId: Long,
    val space: Space? = null,
    val serverName: String = "",
    val spaceType: Space.Type,
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

    // Creative Commons License actions
    data class UpdateCcEnabled(val enabled: Boolean) : SetupLicenseAction
    data class UpdateAllowRemix(val allowed: Boolean) : SetupLicenseAction
    data class UpdateRequireShareAlike(val required: Boolean) : SetupLicenseAction
    data class UpdateAllowCommercial(val allowed: Boolean) : SetupLicenseAction
    data class UpdateCc0Enabled(val enabled: Boolean) : SetupLicenseAction
}