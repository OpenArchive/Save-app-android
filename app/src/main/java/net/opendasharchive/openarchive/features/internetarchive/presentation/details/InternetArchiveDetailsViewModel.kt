package net.opendasharchive.openarchive.features.internetarchive.presentation.details

import androidx.lifecycle.SavedStateHandle
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
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.internetarchive.domain.model.InternetArchive
import net.opendasharchive.openarchive.features.settings.CreativeCommonsLicenseManager
import org.koin.core.component.KoinComponent

class InternetArchiveDetailsViewModel(
    private val gson: Gson,
    savedStateHandle: SavedStateHandle
) : ViewModel(), KoinComponent {

    private val args = InternetArchiveDetailFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val space: Space = Space.get(args.spaceId)!!


    private val _uiState = MutableStateFlow(InternetArchiveDetailsState())
    val uiState: StateFlow<InternetArchiveDetailsState> = _uiState.asStateFlow()

    private val _events = Channel<InternetArchiveDetailsEvent>()
    val events = _events.receiveAsFlow()

    init {
        loadSpaceData()
    }

    fun onAction(action: InternetArchiveDetailsAction) {
        when (action) {
            is InternetArchiveDetailsAction.Remove -> {
                removeSpace()
            }

            is InternetArchiveDetailsAction.Cancel -> {
                viewModelScope.launch {
                    _events.send(InternetArchiveDetailsEvent.NavigateBack)
                }
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
        }
    }

    private fun loadSpaceData() {
        try {
            val metaData = if (space.metaData.isNotEmpty()) {
                gson.fromJson(space.metaData, InternetArchive.MetaData::class.java)
            } else {
                // Fallback to space properties if no metaData
                InternetArchive.MetaData(
                    userName = space.username,
                    screenName = space.displayname.ifEmpty { space.username },
                    email = space.username
                )
            }
            _uiState.update { currentState ->
                val newState = currentState.copy(
                    userName = metaData.userName,
                    email = metaData.email,
                    screenName = metaData.screenName,
                    license = space.license
                )
                initializeLicenseState(newState, space.license)
            }
        } catch (e: Exception) {
            // If JSON parsing fails, use space properties as fallback
            val fallbackMetaData = InternetArchive.MetaData(
                userName = space.username,
                screenName = space.displayname.ifEmpty { space.username },
                email = space.username
            )
            _uiState.update { currentState ->
                val newState = currentState.copy(
                    userName = fallbackMetaData.userName,
                    email = fallbackMetaData.email,
                    screenName = fallbackMetaData.screenName,
                    license = space.license
                )
                initializeLicenseState(newState, space.license)
            }
        }
    }

    private fun removeSpace() {
        viewModelScope.launch {
            space.delete()
            _events.send(InternetArchiveDetailsEvent.NavigateBack)
        }
    }

    private fun updateLicense(license: String?) {
        space.license = license
        space.save()
    }

    private fun getInternetArchiveSpace(): Space? {
        val iaSpaces = Space.get(Space.Type.INTERNET_ARCHIVE)
        return iaSpaces.firstOrNull()
    }

    private fun initializeLicenseState(currentState: InternetArchiveDetailsState, currentLicense: String?): InternetArchiveDetailsState {
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
        } else if (isCC && currentLicense != null) {
            // Regular CC license detected
            currentState.copy(
                ccEnabled = true,
                cc0Enabled = false,
                allowRemix = !(currentLicense.contains("-nd", true)),
                allowCommercial = !(currentLicense.contains("-nc", true)),
                requireShareAlike = !(currentLicense.contains("-nd", true)) && currentLicense.contains("-sa", true),
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