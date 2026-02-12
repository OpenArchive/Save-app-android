package net.opendasharchive.openarchive.services.snowbird

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.core.domain.Vault
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.dialog.DialogStateManager
import net.opendasharchive.openarchive.features.core.dialog.showErrorDialog
import net.opendasharchive.openarchive.features.main.ui.AppRoute
import net.opendasharchive.openarchive.features.main.ui.Navigator
import net.opendasharchive.openarchive.util.ProcessingTracker
import net.opendasharchive.openarchive.util.trackProcessing

data class SnowbirdGroupState(
    val groups: List<Vault> = emptyList(),
    val currentGroup: Vault? = null,
    val isLoading: Boolean = false,
    val error: SnowbirdError? = null,
    val scannedUri: String = ""
)

sealed interface SnowbirdGroupAction {
    data class SelectGroup(val group: Vault) : SnowbirdGroupAction
    data class CreateGroup(val name: String) : SnowbirdGroupAction
    data class CreateGroupWithRepo(val groupName: String, val repoName: String) : SnowbirdGroupAction
    data class JoinGroupWithRepo(val uri: String, val repoName: String) : SnowbirdGroupAction
    data class JoinGroup(val uri: String) : SnowbirdGroupAction
    data class UpdateJoinUri(val uri: String) : SnowbirdGroupAction
    data object RefreshGroups : SnowbirdGroupAction
}

sealed interface SnowbirdGroupEvent {
    data class NavigateToRepo(val vaultId: Long, val groupKey: String) : SnowbirdGroupEvent
    data class NavigateToSuccess(val message: String, val groupKey: String) : SnowbirdGroupEvent
    data object GoBack : SnowbirdGroupEvent
    data object NavigateToScanner : SnowbirdGroupEvent
}

class SnowbirdGroupViewModel(
    private val repository: ISnowbirdGroupRepository,
    private val repoRepository: ISnowbirdRepoRepository,
    private val dialogManager: DialogStateManager,
    private val processingTracker: ProcessingTracker = ProcessingTracker()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SnowbirdGroupState())
    val uiState: StateFlow<SnowbirdGroupState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SnowbirdGroupEvent>()
    val events = _events.asSharedFlow()

    init {
        repository.observeGroups()
            .onEach { groups ->
                _uiState.update { it.copy(groups = groups) }
            }
            .launchIn(viewModelScope)
        
        fetchGroups()
    }

    fun onAction(action: SnowbirdGroupAction) {
        when (action) {
            is SnowbirdGroupAction.SelectGroup -> {
                _uiState.update { it.copy(currentGroup = action.group) }
                viewModelScope.launch {
                    _events.emit(SnowbirdGroupEvent.NavigateToRepo(action.group.id, action.group.vaultKey ?: ""))
                }
            }
            is SnowbirdGroupAction.CreateGroup -> createGroup(action.name)
            is SnowbirdGroupAction.CreateGroupWithRepo -> createGroupWithRepo(action.groupName, action.repoName)
            is SnowbirdGroupAction.JoinGroupWithRepo -> joinGroupWithRepo(action.uri, action.repoName)
            is SnowbirdGroupAction.JoinGroup -> joinGroup(action.uri)
            is SnowbirdGroupAction.UpdateJoinUri -> {
                _uiState.update { it.copy(scannedUri = action.uri) }
            }
            is SnowbirdGroupAction.RefreshGroups -> fetchGroups(forceRefresh = true)
        }
    }

    private fun fetchGroups(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            processingTracker.trackProcessing("fetch_groups") {
                _uiState.update { it.copy(isLoading = true) }
                val result = repository.fetchGroups(forceRefresh)
                _uiState.update { it.copy(isLoading = false) }
                
                if (result is SnowbirdResult.Error) {
                    dialogManager.showErrorDialog(message = UiText.Dynamic(result.error.friendlyMessage))
                }
            }
        }
    }

    private fun createGroup(name: String) {
        viewModelScope.launch {
            processingTracker.trackProcessing("create_group") {
                _uiState.update { it.copy(isLoading = true) }
                val result = repository.createGroup(name)
                _uiState.update { it.copy(isLoading = false) }

                when (result) {
                    is SnowbirdResult.Success -> {
                        onAction(SnowbirdGroupAction.SelectGroup(result.value))
                    }
                    is SnowbirdResult.Error -> {
                        dialogManager.showErrorDialog(message = UiText.Dynamic(result.error.friendlyMessage))
                    }
                }
            }
        }
    }

    private fun createGroupWithRepo(groupName: String, repoName: String) {
        viewModelScope.launch {
            processingTracker.trackProcessing("create_group_with_repo") {
                _uiState.update { it.copy(isLoading = true) }
                val groupResult = repository.createGroup(groupName)
                
                when (groupResult) {
                    is SnowbirdResult.Success -> {
                        val group = groupResult.value
                        val repoResult = repoRepository.createRepo(group.id, group.vaultKey ?: "", repoName)
                        _uiState.update { it.copy(isLoading = false) }
                        
                        when (repoResult) {
                            is SnowbirdResult.Success -> {
                                _events.emit(SnowbirdGroupEvent.NavigateToSuccess(
                                    "Successfully created group and repository",
                                    group.vaultKey ?: ""
                                ))
                            }
                            is SnowbirdResult.Error -> {
                                dialogManager.showErrorDialog(message = UiText.Dynamic(repoResult.error.friendlyMessage))
                            }
                        }
                    }
                    is SnowbirdResult.Error -> {
                        _uiState.update { it.copy(isLoading = false) }
                        dialogManager.showErrorDialog(message = UiText.Dynamic(groupResult.error.friendlyMessage))
                    }
                }
            }
        }
    }

    private fun joinGroupWithRepo(uri: String, repoName: String) {
        viewModelScope.launch {
            processingTracker.trackProcessing("join_group_with_repo") {
                _uiState.update { it.copy(isLoading = true) }
                val joinResult = repository.joinGroup(uri)
                
                when (joinResult) {
                    is SnowbirdResult.Success -> {
                        // After joining, we refresh groups to get the new group in our state
                        repository.fetchGroups(forceRefresh = true)
                        // Group key might be in the URI or we just find the new one.
                        // For now, let's just emit success since they are in the list now.
                        _uiState.update { it.copy(isLoading = false) }
                        _events.emit(SnowbirdGroupEvent.NavigateToSuccess(
                            "Successfully joined group",
                            "" // We might not have the key yet if it's not in response
                        ))
                    }
                    is SnowbirdResult.Error -> {
                        _uiState.update { it.copy(isLoading = false) }
                        dialogManager.showErrorDialog(message = UiText.Dynamic(joinResult.error.friendlyMessage))
                    }
                }
            }
        }
    }

    private fun joinGroup(uri: String) {
        viewModelScope.launch {
            processingTracker.trackProcessing("join_group") {
                _uiState.update { it.copy(isLoading = true) }
                val result = repository.joinGroup(uri)
                _uiState.update { it.copy(isLoading = false) }

                if (result is SnowbirdResult.Error) {
                    dialogManager.showErrorDialog(message = UiText.Dynamic(result.error.friendlyMessage))
                } else {
                    fetchGroups(forceRefresh = true)
                }
            }
        }
    }
}