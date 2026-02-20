package net.opendasharchive.openarchive.features.spaces

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.core.domain.Vault
import net.opendasharchive.openarchive.core.domain.VaultType
import net.opendasharchive.openarchive.core.repositories.SpaceRepository
import net.opendasharchive.openarchive.features.main.ui.AppRoute
import net.opendasharchive.openarchive.features.main.ui.Navigator

class SpaceListViewModel(
    private val navigator: Navigator,
    private val spaceRepository: SpaceRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpaceListState(emptyList()))
    val uiState: StateFlow<SpaceListState> = _uiState.asStateFlow()

    init {
        refreshSpaces()
    }

    fun onAction(action: SpaceListAction) {
        when (action) {
            is SpaceListAction.NavigateToSpace -> {
                navigateToSpaceDetail(spaceId = action.spaceId, spaceType = action.spaceType)
            }

            is SpaceListAction.AddNewSpace -> {
                navigator.navigateTo(AppRoute.SpaceSetupRoute)
            }

            SpaceListAction.RefreshSpaces -> refreshSpaces()
        }
    }

    private fun refreshSpaces() = viewModelScope.launch {
        val spaceList = spaceRepository.getSpaces()
        _uiState.update { it.copy(spaceList = spaceList) }
    }

    private fun navigateToSpaceDetail(spaceId: Long, spaceType: VaultType) {
        when (spaceType) {
            VaultType.PRIVATE_SERVER -> navigator.navigateTo(AppRoute.WebDavDetailRoute(spaceId))
            VaultType.INTERNET_ARCHIVE -> navigator.navigateTo(AppRoute.IADetailRoute(spaceId))
            VaultType.DWEB_STORAGE -> navigator.navigateTo(AppRoute.SnowbirdDashboardRoute)
            VaultType.STORACHA -> navigator.navigateTo(AppRoute.StorachaRoute)
        }
    }
}

data class SpaceListState(
    val spaceList: List<Vault>,
)

sealed interface SpaceListAction {
    data class NavigateToSpace(val spaceId: Long, val spaceType: VaultType) : SpaceListAction
    data object AddNewSpace : SpaceListAction
    data object RefreshSpaces : SpaceListAction
}