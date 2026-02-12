package net.opendasharchive.openarchive.services.snowbird.presentation.group

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
import net.opendasharchive.openarchive.core.domain.DomainResult
import net.opendasharchive.openarchive.core.domain.Vault
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.dialog.DialogStateManager
import net.opendasharchive.openarchive.features.core.dialog.DialogType
import net.opendasharchive.openarchive.features.core.dialog.showDialog
import net.opendasharchive.openarchive.features.core.dialog.showErrorDialog
import net.opendasharchive.openarchive.services.snowbird.service.repository.ISnowbirdGroupRepository
import net.opendasharchive.openarchive.util.ProcessingTracker
import net.opendasharchive.openarchive.util.trackProcessing

data class SnowbirdGroupListState(
    val groups: List<Vault> = emptyList(),
    val isLoading: Boolean = false
)

sealed interface SnowbirdGroupListAction {
    data class SelectGroup(val group: Vault) : SnowbirdGroupListAction
    data class ShareGroup(val group: Vault) : SnowbirdGroupListAction
    data object RefreshGroups : SnowbirdGroupListAction
}

sealed interface SnowbirdGroupListEvent {
    data class NavigateToRepo(val vaultId: Long, val groupKey: String) : SnowbirdGroupListEvent
    data class NavigateToShare(val groupKey: String) : SnowbirdGroupListEvent
}

class SnowbirdGroupListViewModel(
    private val repository: ISnowbirdGroupRepository,
    private val dialogManager: DialogStateManager,
    private val processingTracker: ProcessingTracker = ProcessingTracker()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SnowbirdGroupListState())
    val uiState: StateFlow<SnowbirdGroupListState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SnowbirdGroupListEvent>()
    val events = _events.asSharedFlow()

    init {
        repository.observeGroups()
            .onEach { groups ->
                _uiState.update { it.copy(groups = groups) }
            }
            .launchIn(viewModelScope)
        
        fetchGroups()
    }

    fun onAction(action: SnowbirdGroupListAction) {
        when (action) {
            is SnowbirdGroupListAction.SelectGroup -> {
                viewModelScope.launch {
                    _events.emit(SnowbirdGroupListEvent.NavigateToRepo(action.group.id, action.group.vaultKey ?: ""))
                }
            }
            is SnowbirdGroupListAction.ShareGroup -> {
                showShareConfirmation(action.group)
            }
            is SnowbirdGroupListAction.RefreshGroups -> fetchGroups(forceRefresh = true)
        }
    }

    private fun fetchGroups(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            processingTracker.trackProcessing("fetch_groups") {
                _uiState.update { it.copy(isLoading = true) }
                val result = repository.fetchGroups(forceRefresh)
                _uiState.update { it.copy(isLoading = false) }
                
                if (result is DomainResult.Error) {
                    dialogManager.showErrorDialog(message = UiText.Dynamic(result.error.friendlyMessage))
                }
            }
        }
    }

    private fun showShareConfirmation(group: Vault) {
        dialogManager.showDialog(dialogManager.requireResourceProvider()) {
            type = DialogType.Info
            title = UiText.Dynamic("Share Group")
            message = UiText.Dynamic("Would you like to share this group?")
            positiveButton {
                text = UiText.Dynamic("Yes")
                action = {
                    viewModelScope.launch {
                        _events.emit(SnowbirdGroupListEvent.NavigateToShare(group.vaultKey ?: ""))
                    }
                }
            }
            neutralButton {
                text = UiText.Dynamic("No")
            }
        }
    }
}
