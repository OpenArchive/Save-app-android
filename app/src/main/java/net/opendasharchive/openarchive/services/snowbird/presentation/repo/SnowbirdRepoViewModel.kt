package net.opendasharchive.openarchive.services.snowbird.presentation.repo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.features.main.ui.AppRoute
import net.opendasharchive.openarchive.features.main.ui.Navigator
import net.opendasharchive.openarchive.core.domain.Archive
import net.opendasharchive.openarchive.core.domain.DomainResult
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.dialog.DialogStateManager
import net.opendasharchive.openarchive.features.core.dialog.showErrorDialog
import net.opendasharchive.openarchive.services.snowbird.service.repository.ISnowbirdRepoRepository
import net.opendasharchive.openarchive.util.ProcessingTracker
import net.opendasharchive.openarchive.util.trackProcessing

data class SnowbirdRepoState(
    val repos: List<Archive> = emptyList(),
    val isLoading: Boolean = false,
    val vaultId: Long = 0,
    val groupKey: String = ""
)

sealed interface SnowbirdRepoAction {
    data class SelectRepo(val repo: Archive) : SnowbirdRepoAction
    data class CreateRepo(val name: String) : SnowbirdRepoAction
    data object RefreshRepos : SnowbirdRepoAction
    data object RefreshGroupContent : SnowbirdRepoAction
}



class SnowbirdRepoViewModel(
    private val navigator: Navigator,
    private val route: AppRoute.SnowbirdRepoListRoute,
    private val repository: ISnowbirdRepoRepository,
    private val dialogManager: DialogStateManager,
    private val processingTracker: ProcessingTracker = ProcessingTracker()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SnowbirdRepoState(
            vaultId = route.vaultId,
            groupKey = route.groupKey
        )
    )
    val uiState: StateFlow<SnowbirdRepoState> = _uiState.asStateFlow()

    init {
        observeRepos(route.vaultId)
        fetchRepos()
    }

    fun onAction(action: SnowbirdRepoAction) {
        when (action) {
            is SnowbirdRepoAction.SelectRepo -> {
                navigator.navigateTo(
                    AppRoute.SnowbirdFileListRoute(
                        archiveId = action.repo.id,
                        groupKey = _uiState.value.groupKey,
                        repoKey = action.repo.archiveKey ?: ""
                    )
                )
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

                if (result is DomainResult.Error) {
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

                if (result is DomainResult.Error) {
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

                when (result) {
                    is DomainResult.Error -> {
                        dialogManager.showErrorDialog(message = UiText.Dynamic(result.error.friendlyMessage))
                    }
                    is DomainResult.Success -> {
                        val repoErrors = result.data.refreshedRepos.mapNotNull { repo ->
                            val error = repo.error?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                            val bucket = classifyRefreshError(error)
                            "Repo ${repo.name} (${repo.repoId}): $bucket - $error"
                        }
                        if (repoErrors.isNotEmpty()) {
                            val summary = repoErrors.take(8).joinToString("\n")
                            val suffix = if (repoErrors.size > 8) "\n... and ${repoErrors.size - 8} more" else ""
                            dialogManager.showErrorDialog(
                                message = UiText.Dynamic(
                                    "Some repositories failed to refresh:\n$summary$suffix"
                                )
                            )
                        }
                        fetchRepos(forceRefresh = true)
                    }
                }
            }
        }
    }

    private fun classifyRefreshError(message: String): String {
        val value = message.lowercase()
        return when {
            "dht" in value || "repo root hash" in value -> "DHT_DISCOVERY"
            "download from any peer" in value || "any peer" in value -> "PEER_DOWNLOAD"
            else -> "UNKNOWN"
        }
    }
}
