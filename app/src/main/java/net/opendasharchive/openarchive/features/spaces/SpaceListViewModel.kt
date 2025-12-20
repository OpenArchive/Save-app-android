package net.opendasharchive.openarchive.features.spaces

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.main.ui.AppRoute
import net.opendasharchive.openarchive.features.main.ui.Navigator

class SpaceListViewModel(
    private val navigator: Navigator,
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

    private fun refreshSpaces() = viewModelScope.launch{
        val spaceList = Space.getAll().asSequence().toList()
        _uiState.update { it.copy(spaceList = spaceList) }
    }

    private fun navigateToSpaceDetail(spaceId: Long, spaceType: Space.Type) {
        when(spaceType) {
            Space.Type.WEBDAV -> navigateToWebDavDetail(spaceId)
            Space.Type.INTERNET_ARCHIVE -> navigateToInternetArchiveDetail(spaceId)
            Space.Type.RAVEN -> TODO("Not implemented yet")
        }
    }

    private fun navigateToWebDavDetail(spaceId: Long) {
        navigator.navigateTo(AppRoute.WebDavDetailRoute(spaceId))
    }

    private fun navigateToInternetArchiveDetail(spaceId: Long) {
        navigator.navigateTo(AppRoute.IADetailRoute(spaceId))
    }
}

data class SpaceListState(
    val spaceList: List<Space>,
)

sealed interface SpaceListAction {
    data class NavigateToSpace(val spaceId: Long, val spaceType: Space.Type) : SpaceListAction
    data object AddNewSpace : SpaceListAction
    data object RefreshSpaces : SpaceListAction
}