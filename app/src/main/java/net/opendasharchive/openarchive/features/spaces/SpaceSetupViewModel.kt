package net.opendasharchive.openarchive.features.spaces

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.core.repositories.SpaceRepository
import net.opendasharchive.openarchive.features.main.ui.AppRoute
import net.opendasharchive.openarchive.features.main.ui.Navigator
import net.opendasharchive.openarchive.core.config.AppConfig
import net.opendasharchive.openarchive.core.domain.VaultType

data class SpaceSetupState(
    val isInternetArchiveAllowed: Boolean = false,
    val isDwebEnabled: Boolean = false
)

sealed interface SpaceSetupAction {
    data object WebDavClicked : SpaceSetupAction
    data object InternetArchiveClicked : SpaceSetupAction
    data object DwebClicked : SpaceSetupAction
    data object StorachaClicked : SpaceSetupAction
}

class SpaceSetupViewModel(
    private val appConfig: AppConfig,
    private val navigator: Navigator,
    private val spaceRepository: SpaceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpaceSetupState())
    val uiState: StateFlow<SpaceSetupState> = _uiState.asStateFlow()

    init {
        loadInitialState()
    }

    private fun loadInitialState() {
        viewModelScope.launch {
            val hasInternetArchive = spaceRepository.getSpaces().any { it.type == VaultType.INTERNET_ARCHIVE }
            _uiState.update {
                it.copy(
                    isInternetArchiveAllowed = !hasInternetArchive,
                    isDwebEnabled = appConfig.isDwebEnabled
                )
            }
        }
    }

    fun onAction(action: SpaceSetupAction) {
        when (action) {
            SpaceSetupAction.WebDavClicked -> {
                viewModelScope.launch {
                    navigator.navigateTo(AppRoute.WebDavLoginRoute)
                }
            }

            SpaceSetupAction.InternetArchiveClicked -> {
                viewModelScope.launch {
                    navigator.navigateTo(AppRoute.IALoginRoute)
                }
            }

            SpaceSetupAction.DwebClicked -> {
                viewModelScope.launch {
                    navigator.navigateTo(AppRoute.SnowbirdDashboardRoute)
                }
            }

            SpaceSetupAction.StorachaClicked -> {
                viewModelScope.launch {
                    navigator.navigateTo(AppRoute.StorachaRoute)
                }
            }
        }
    }
}
