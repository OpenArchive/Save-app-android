package net.opendasharchive.openarchive.features.internetarchive.presentation.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.core.domain.Vault
import net.opendasharchive.openarchive.features.internetarchive.domain.model.InternetArchive
import net.opendasharchive.openarchive.core.repositories.SpaceRepository
import net.opendasharchive.openarchive.features.main.ui.AppRoute
import net.opendasharchive.openarchive.features.main.ui.Navigator
import net.opendasharchive.openarchive.features.settings.CreativeCommonsLicenseManager
import org.koin.core.component.KoinComponent

class InternetArchiveDetailsViewModel(
    private val route: AppRoute.IADetailRoute,
    private val navigator: Navigator,
    private val gson: Gson,
    private val spaceRepository: SpaceRepository,
) : ViewModel(), KoinComponent {

    private lateinit var vault: Vault

    private val _uiState = MutableStateFlow(
        InternetArchiveDetailsState(
            spaceId = route.spaceId
        )
    )
    val uiState: StateFlow<InternetArchiveDetailsState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<InternetArchiveDetailsEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        loadSpaceData()
    }

    fun onAction(action: InternetArchiveDetailsAction) {
        when (action) {
            is InternetArchiveDetailsAction.RemoveSpace -> {
                removeSpace()
            }

            is InternetArchiveDetailsAction.UpdateLicense -> {
                _uiState.update { it.copy(license = action.license) }
                updateLicense(action.license)
            }

            is InternetArchiveDetailsAction.UpdateCcEnabled -> {
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

            is InternetArchiveDetailsAction.UpdateAllowRemix -> {
                _uiState.update { currentState ->
                    currentState.copy(
                        allowRemix = action.allowed,
                        cc0Enabled = if (action.allowed) false else currentState.cc0Enabled,  // Disable CC0 if remix is enabled
                        requireShareAlike = if (!action.allowed) false else currentState.requireShareAlike  // Auto-disable ShareAlike when Remix is disabled
                    )
                }
                generateAndUpdateLicense()
            }

            is InternetArchiveDetailsAction.UpdateRequireShareAlike -> {
                _uiState.update { currentState ->
                    currentState.copy(
                        requireShareAlike = action.required,
                        cc0Enabled = if (action.required) false else currentState.cc0Enabled  // Disable CC0 if share alike is enabled
                    )
                }
                generateAndUpdateLicense()
            }

            is InternetArchiveDetailsAction.UpdateAllowCommercial -> {
                _uiState.update { currentState ->
                    currentState.copy(
                        allowCommercial = action.allowed,
                        cc0Enabled = if (action.allowed) false else currentState.cc0Enabled  // Disable CC0 if commercial is enabled
                    )
                }
                generateAndUpdateLicense()
            }

            is InternetArchiveDetailsAction.UpdateCc0Enabled -> {
                _uiState.update { currentState ->
                    if (action.enabled) {
                        // When CC0 is enabled, disable all other options
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

            InternetArchiveDetailsAction.ShowRemoveSpaceDialog -> viewModelScope.launch {
                _uiEvent.send(InternetArchiveDetailsEvent.ShowRemoveSpaceDialog)
            }
        }
    }

    private fun loadSpaceData() = viewModelScope.launch {

        vault = spaceRepository.getSpaceById(route.spaceId) ?: run {
            navigator.navigateBack()
            return@launch
        }

        try {
            val metaData = if (vault.metaData.isNotEmpty()) {
                gson.fromJson(vault.metaData, InternetArchive.MetaData::class.java)
            } else {
                // Fallback to space properties if no metaData
                InternetArchive.MetaData(
                    userName = vault.username,
                    screenName = vault.displayName.ifEmpty { vault.username },
                    email = vault.username
                )
            }

            _uiState.update { currentState ->
                val newState = currentState.copy(
                    userName = metaData.userName,
                    email = metaData.email,
                    screenName = metaData.screenName,
                    license = vault.licenseUrl
                )

                initializeLicenseState(newState, vault.licenseUrl)
            }

        } catch (e: Exception) {
            // If JSON parsing fails, use space properties as fallback
            val fallbackMetaData = InternetArchive.MetaData(
                userName = vault.username,
                screenName = vault.displayName.ifEmpty { vault.username },
                email = vault.username
            )
            _uiState.update { currentState ->
                val newState = currentState.copy(
                    userName = fallbackMetaData.userName,
                    email = fallbackMetaData.email,
                    screenName = fallbackMetaData.screenName,
                    license = vault.licenseUrl
                )

                initializeLicenseState(newState, vault.licenseUrl)
            }
        }
    }

    private fun removeSpace() {
        viewModelScope.launch {
            val isSuccess = spaceRepository.deleteSpace(route.spaceId)
            if (!isSuccess) {
                return@launch
            }
            navigator.navigateBack()
        }
    }

    private fun updateLicense(license: String?) = viewModelScope.launch {
        vault = vault.copy(licenseUrl = license)
        spaceRepository.updateSpace(route.spaceId, vault)
    }

    private fun initializeLicenseState(
        currentState: InternetArchiveDetailsState,
        currentLicense: String?
    ): InternetArchiveDetailsState {
        val isCc0 = currentLicense?.contains("publicdomain/zero", true) ?: false
        val isCC = currentLicense?.contains("creativecommons.org/licenses", true) ?: false

        return if (isCc0) {
            // CC0 license detected
            currentState.copy(
                ccEnabled = true,
                cc0Enabled = true,
                allowRemix = false,
                allowCommercial = false,
                requireShareAlike = false,
                licenseUrl = currentLicense
            )
        } else if (isCC) {
            // Regular CC license detected
            currentState.copy(
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
            currentState.copy(
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
        updateLicense(newLicense)
    }
}