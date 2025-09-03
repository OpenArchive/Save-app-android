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

    private val spaceId: Long = savedStateHandle.get<Long>("space_id") ?: -1L
    private val space: Space = if (spaceId == -1L) {
        getInternetArchiveSpace() ?: Space(Space.Type.INTERNET_ARCHIVE)
    } else {
        Space.get(spaceId) ?: Space(Space.Type.INTERNET_ARCHIVE)
    }

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
                    currentState.copy(ccEnabled = action.enabled).let { newState ->
                        if (!action.enabled) {
                            // Reset other switches when CC is disabled
                            newState.copy(
                                requireShareAlike = false,
                                licenseUrl = null
                            )
                        } else {
                            newState
                        }
                    }
                }
                generateAndUpdateLicense()
            }

            is InternetArchiveDetailsAction.UpdateAllowRemix -> {
                _uiState.update { currentState ->
                    currentState.copy(allowRemix = action.allowed).let { newState ->
                        if (!action.allowed) {
                            // Auto-disable ShareAlike when Remix is disabled
                            newState.copy(requireShareAlike = false)
                        } else {
                            newState
                        }
                    }
                }
                generateAndUpdateLicense()
            }

            is InternetArchiveDetailsAction.UpdateRequireShareAlike -> {
                _uiState.update { it.copy(requireShareAlike = action.required) }
                generateAndUpdateLicense()
            }

            is InternetArchiveDetailsAction.UpdateAllowCommercial -> {
                _uiState.update { it.copy(allowCommercial = action.allowed) }
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
        val isActive = currentLicense?.contains("creativecommons.org", true) ?: false
        return if (isActive && currentLicense != null) {
            currentState.copy(
                ccEnabled = true,
                allowRemix = !(currentLicense.contains("-nd", true)),
                allowCommercial = !(currentLicense.contains("-nc", true)),
                requireShareAlike = !(currentLicense.contains("-nd", true)) && currentLicense.contains("-sa", true),
                licenseUrl = currentLicense
            )
        } else {
            currentState.copy(
                ccEnabled = false,
                allowRemix = true,  // XML default
                allowCommercial = false,  // XML default
                requireShareAlike = false,  // XML default
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
            allowCommercial = currentState.allowCommercial
        )
        
        _uiState.update { it.copy(licenseUrl = newLicense) }
        updateLicense(newLicense)
    }
}