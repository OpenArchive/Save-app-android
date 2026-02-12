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
import net.opendasharchive.openarchive.core.domain.DomainResult
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.dialog.DialogStateManager
import net.opendasharchive.openarchive.features.core.dialog.showErrorDialog
import net.opendasharchive.openarchive.services.snowbird.service.repository.ISnowbirdGroupRepository
import net.opendasharchive.openarchive.util.ProcessingTracker
import net.opendasharchive.openarchive.util.trackProcessing

data class SnowbirdJoinGroupState(
    val isLoading: Boolean = false,
    val scannedUri: String = ""
)

sealed interface SnowbirdJoinGroupAction {
    data class JoinGroupWithRepo(val uri: String, val repoName: String) : SnowbirdJoinGroupAction
    data class UpdateJoinUri(val uri: String) : SnowbirdJoinGroupAction
}

sealed interface SnowbirdJoinGroupEvent {
    data class NavigateToSuccess(val message: String, val groupKey: String) : SnowbirdJoinGroupEvent
    data object GoBack : SnowbirdJoinGroupEvent
}

class SnowbirdJoinGroupViewModel(
    private val repository: ISnowbirdGroupRepository,
    private val dialogManager: DialogStateManager,
    private val processingTracker: ProcessingTracker = ProcessingTracker()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SnowbirdJoinGroupState())
    val uiState: StateFlow<SnowbirdJoinGroupState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SnowbirdJoinGroupEvent>()
    val events = _events.asSharedFlow()

    fun onAction(action: SnowbirdJoinGroupAction) {
        when (action) {
            is SnowbirdJoinGroupAction.JoinGroupWithRepo -> joinGroupWithRepo(action.uri, action.repoName)
            is SnowbirdJoinGroupAction.UpdateJoinUri -> {
                _uiState.update { it.copy(scannedUri = action.uri) }
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
                        _events.emit(SnowbirdJoinGroupEvent.NavigateToSuccess(
                            "Successfully joined group",
                            "" // We might not have the key yet if it's not in response
                        ))
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
