package net.opendasharchive.openarchive.services.snowbird.presentation.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.core.domain.DomainResult
import net.opendasharchive.openarchive.core.domain.VaultType
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.dialog.DialogStateManager
import net.opendasharchive.openarchive.features.core.dialog.showErrorDialog
import net.opendasharchive.openarchive.features.main.ui.AppRoute
import net.opendasharchive.openarchive.features.main.ui.Navigator
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


class SnowbirdCreateGroupViewModel(
    private val navigator: Navigator,
    route: AppRoute.SnowbirdCreateGroupRoute,
    private val repository: ISnowbirdGroupRepository,
    private val repoRepository: ISnowbirdRepoRepository,
    private val dialogManager: DialogStateManager,
    private val processingTracker: ProcessingTracker = ProcessingTracker()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SnowbirdCreateGroupState())
    val uiState: StateFlow<SnowbirdCreateGroupState> = _uiState.asStateFlow()

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
                navigator.navigateBack()
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
                when (val groupResult = repository.createGroup(groupName)) {
                    is DomainResult.Success -> {
                        val group = groupResult.data
                        val groupKey = group.vaultKey.orEmpty()

                        val repoResult = repoRepository.createRepo(
                            vaultId = group.id,
                            groupKey = groupKey,
                            repoName = repoName
                        )

                        when (repoResult) {
                            is DomainResult.Success -> {
                                navigator.navigateTo(AppRoute.SpaceSetupSuccessRoute(VaultType.DWEB_STORAGE))
                            }

                            is DomainResult.Error -> {
                                _uiState.update { it.copy(isLoading = false) }
                                dialogManager.showErrorDialog(
                                    message = UiText.Dynamic(repoResult.error.friendlyMessage)
                                )
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
