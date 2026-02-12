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
import net.opendasharchive.openarchive.services.snowbird.service.repository.ISnowbirdRepoRepository
import net.opendasharchive.openarchive.util.ProcessingTracker
import net.opendasharchive.openarchive.util.trackProcessing

data class SnowbirdCreateGroupState(
    val groupName: String = "",
    val repoName: String = "",
    val isLoading: Boolean = false
)

sealed interface SnowbirdCreateGroupAction {
    data class UpdateGroupName(val name: String) : SnowbirdCreateGroupAction
    data class UpdateRepoName(val name: String) : SnowbirdCreateGroupAction
    data object CreateGroup : SnowbirdCreateGroupAction
    data object Cancel : SnowbirdCreateGroupAction
}

sealed interface SnowbirdCreateGroupEvent {
    data class NavigateToSuccess(val message: String, val groupKey: String) : SnowbirdCreateGroupEvent
    data object GoBack : SnowbirdCreateGroupEvent
}

class SnowbirdCreateGroupViewModel(
    private val repository: ISnowbirdGroupRepository,
    private val repoRepository: ISnowbirdRepoRepository,
    private val dialogManager: DialogStateManager,
    private val processingTracker: ProcessingTracker = ProcessingTracker()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SnowbirdCreateGroupState())
    val uiState: StateFlow<SnowbirdCreateGroupState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SnowbirdCreateGroupEvent>()
    val events = _events.asSharedFlow()

    fun onAction(action: SnowbirdCreateGroupAction) {
        when (action) {
            is SnowbirdCreateGroupAction.UpdateGroupName -> {
                _uiState.update { it.copy(groupName = action.name) }
            }
            is SnowbirdCreateGroupAction.UpdateRepoName -> {
                _uiState.update { it.copy(repoName = action.name) }
            }
            is SnowbirdCreateGroupAction.CreateGroup -> createGroupWithRepo()
            is SnowbirdCreateGroupAction.Cancel -> {
                viewModelScope.launch {
                    _events.emit(SnowbirdCreateGroupEvent.GoBack)
                }
            }
        }
    }

    private fun createGroupWithRepo() {
        val currentState = _uiState.value
        val groupName = currentState.groupName
        val repoName = currentState.repoName

        if (groupName.isBlank() || repoName.isBlank()) return

        viewModelScope.launch {
            processingTracker.trackProcessing("create_group_with_repo") {
                _uiState.update { it.copy(isLoading = true) }
                val groupResult = repository.createGroup(groupName)
                
                when (groupResult) {
                    is DomainResult.Success -> {
                        val group = groupResult.data
                        val repoResult = repoRepository.createRepo(group.id, group.vaultKey ?: "", repoName)
                        _uiState.update { it.copy(isLoading = false) }
                        
                        when (repoResult) {
                            is DomainResult.Success -> {
                                _events.emit(SnowbirdCreateGroupEvent.NavigateToSuccess(
                                    "Successfully created group and repository",
                                    group.vaultKey ?: ""
                                ))
                            }
                            is DomainResult.Error -> {
                                dialogManager.showErrorDialog(message = UiText.Dynamic(repoResult.error.friendlyMessage))
                            }
                        }
                    }
                    is DomainResult.Error -> {
                        _uiState.update { it.copy(isLoading = false) }
                        dialogManager.showErrorDialog(message = UiText.Dynamic(groupResult.error.friendlyMessage))
                    }
                }
            }
        }
    }
}
