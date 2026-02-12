package net.opendasharchive.openarchive.services.snowbird

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.core.domain.Archive
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.dialog.DialogStateManager
import net.opendasharchive.openarchive.features.core.dialog.showErrorDialog
import net.opendasharchive.openarchive.features.main.ui.Navigator
import net.opendasharchive.openarchive.services.snowbird.ISnowbirdRepoRepository
import net.opendasharchive.openarchive.services.snowbird.SnowbirdResult
import net.opendasharchive.openarchive.util.ProcessingTracker
import net.opendasharchive.openarchive.util.trackProcessing

data class SnowbirdRepoState(
    val repos: List<Archive> = emptyList(),
    val isLoading: Boolean = false,
    val vaultId: Long = 0,
    val groupKey: String = ""
)

sealed interface SnowbirdRepoAction {
    data class Init(val vaultId: Long, val groupKey: String) : SnowbirdRepoAction
    data class SelectRepo(val repo: Archive) : SnowbirdRepoAction
    data class CreateRepo(val name: String) : SnowbirdRepoAction
    data object RefreshRepos : SnowbirdRepoAction
    data object RefreshGroupContent : SnowbirdRepoAction
}

sealed interface SnowbirdRepoEvent {
    data class NavigateToFileList(val archiveId: Long, val repoKey: String) : SnowbirdRepoEvent
}

class SnowbirdRepoViewModel(
    private val repository: ISnowbirdRepoRepository,
    private val dialogManager: DialogStateManager,
    private val processingTracker: ProcessingTracker = ProcessingTracker()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SnowbirdRepoState())
    val uiState: StateFlow<SnowbirdRepoState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SnowbirdRepoEvent>()
    val events = _events.asSharedFlow()

    fun onAction(action: SnowbirdRepoAction) {
        when (action) {
            is SnowbirdRepoAction.Init -> {
                _uiState.update { it.copy(vaultId = action.vaultId, groupKey = action.groupKey) }
                observeRepos(action.vaultId)
                fetchRepos()
            }
            is SnowbirdRepoAction.SelectRepo -> {
                viewModelScope.launch {
                    _events.emit(SnowbirdRepoEvent.NavigateToFileList(action.repo.id, action.repo.archiveKey ?: ""))
                }
            }
            is SnowbirdRepoAction.CreateRepo -> createRepo(action.name)
            is SnowbirdRepoAction.RefreshRepos -> fetchRepos(forceRefresh = true)
            is SnowbirdRepoAction.RefreshGroupContent -> refreshGroupContent()
        }
    }

    private fun observeRepos(vaultId: Long) {
        repository.observeRepos(vaultId)
            .onEach { repos ->
                _uiState.update { it.copy(repos = repos) }
            }
            .launchIn(viewModelScope)
    }

    private fun fetchRepos(forceRefresh: Boolean = false) {
        val vaultId = _uiState.value.vaultId
        val groupKey = _uiState.value.groupKey
        if (vaultId == 0L || groupKey.isBlank()) return

        viewModelScope.launch {
            processingTracker.trackProcessing("fetch_repos") {
                _uiState.update { it.copy(isLoading = true) }
                val result = repository.fetchRepos(vaultId, groupKey, forceRefresh)
                _uiState.update { it.copy(isLoading = false) }

                if (result is SnowbirdResult.Error) {
                    dialogManager.showErrorDialog(message = UiText.Dynamic(result.error.friendlyMessage))
                }
            }
        }
    }

    private fun createRepo(name: String) {
        val vaultId = _uiState.value.vaultId
        val groupKey = _uiState.value.groupKey
        viewModelScope.launch {
            processingTracker.trackProcessing("create_repo") {
                _uiState.update { it.copy(isLoading = true) }
                val result = repository.createRepo(vaultId, groupKey, name)
                _uiState.update { it.copy(isLoading = false) }

                if (result is SnowbirdResult.Error) {
                    dialogManager.showErrorDialog(message = UiText.Dynamic(result.error.friendlyMessage))
                }
            }
        }
    }

    private fun refreshGroupContent() {
        val groupKey = _uiState.value.groupKey
        viewModelScope.launch {
            processingTracker.trackProcessing("refresh_group_content") {
                _uiState.update { it.copy(isLoading = true) }
                val result = repository.refreshGroupContent(groupKey)
                _uiState.update { it.copy(isLoading = false) }

                if (result is SnowbirdResult.Error) {
                    dialogManager.showErrorDialog(message = UiText.Dynamic(result.error.friendlyMessage))
                } else {
                    fetchRepos(forceRefresh = true)
                }
            }
        }
    }
}