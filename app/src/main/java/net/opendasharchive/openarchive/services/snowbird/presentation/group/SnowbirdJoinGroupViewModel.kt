package net.opendasharchive.openarchive.services.snowbird.presentation.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.core.domain.VaultType
import net.opendasharchive.openarchive.features.main.ui.AppRoute
import net.opendasharchive.openarchive.features.main.ui.Navigator
import net.opendasharchive.openarchive.core.domain.DomainResult
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.dialog.DialogStateManager
import net.opendasharchive.openarchive.features.core.dialog.showErrorDialog
import net.opendasharchive.openarchive.services.snowbird.service.repository.ISnowbirdGroupRepository
import net.opendasharchive.openarchive.util.ProcessingTracker
import net.opendasharchive.openarchive.util.trackProcessing

import net.opendasharchive.openarchive.extensions.getQueryParameter

data class SnowbirdJoinGroupState(
    val isLoading: Boolean = false,
    val scannedUri: String = "",
    val groupName: String = "",
    val repoName: String = ""
) {
    val isFormValid: Boolean get() = scannedUri.isNotBlank() && repoName.isNotBlank()
}

sealed interface SnowbirdJoinGroupAction {
    data class JoinGroupWithRepo(val uri: String, val repoName: String) : SnowbirdJoinGroupAction
    data class UpdateJoinUri(val uri: String) : SnowbirdJoinGroupAction
    data class UpdateRepoName(val name: String) : SnowbirdJoinGroupAction
    data object Authenticate : SnowbirdJoinGroupAction
    data object Cancel : SnowbirdJoinGroupAction
}



class SnowbirdJoinGroupViewModel(
    private val navigator: Navigator,
    private val route: AppRoute.SnowbirdJoinGroupRoute,
    private val repository: ISnowbirdGroupRepository,
    private val dialogManager: DialogStateManager,
    private val processingTracker: ProcessingTracker = ProcessingTracker()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SnowbirdJoinGroupState())
    val uiState: StateFlow<SnowbirdJoinGroupState> = _uiState.asStateFlow()

    init {
        // Auto-initialize from route arguments
        val groupName = route.groupKey.getQueryParameter("name") ?: ""
        _uiState.update { it.copy(scannedUri = route.groupKey, groupName = groupName) }
    }

    fun onAction(action: SnowbirdJoinGroupAction) {
        when (action) {
            is SnowbirdJoinGroupAction.JoinGroupWithRepo -> joinGroupWithRepo(action.uri, action.repoName)
            is SnowbirdJoinGroupAction.UpdateJoinUri -> {
                val groupName = action.uri.getQueryParameter("name") ?: ""
                _uiState.update { it.copy(scannedUri = action.uri, groupName = groupName) }
            }
            is SnowbirdJoinGroupAction.UpdateRepoName -> {
                _uiState.update { it.copy(repoName = action.name) }
            }
            is SnowbirdJoinGroupAction.Authenticate -> {
                val state = _uiState.value
                joinGroupWithRepo(state.scannedUri, state.repoName)
            }
            is SnowbirdJoinGroupAction.Cancel -> {
                navigator.navigateBack()
            }
        }
    }

    private fun joinGroupWithRepo(uri: String, repoName: String) {
        viewModelScope.launch {
            processingTracker.trackProcessing("join_group_with_repo") {
                _uiState.update { it.copy(isLoading = true) }
                val joinResult = repository.joinGroup(uri)
                
                when (joinResult) {
                    is DomainResult.Success -> {
                        // After joining, we refresh groups to get the new group in our state
                        repository.fetchGroups(forceRefresh = true)
                        _uiState.update { it.copy(isLoading = false) }
                        navigator.navigateTo(
                            AppRoute.SpaceSetupSuccessRoute(VaultType.DWEB_STORAGE)
                        )
                    }
                    is DomainResult.Error -> {
                        _uiState.update { it.copy(isLoading = false) }
                        dialogManager.showErrorDialog(message = UiText.Dynamic(joinResult.error.friendlyMessage))
                    }
                }
            }
        }
    }
}
