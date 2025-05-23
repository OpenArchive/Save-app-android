package net.opendasharchive.openarchive.services.snowbird

import android.app.Application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.db.RefreshGroupResponse
import net.opendasharchive.openarchive.db.SnowbirdError
import net.opendasharchive.openarchive.db.SnowbirdGroup
import net.opendasharchive.openarchive.db.SnowbirdRepo
import net.opendasharchive.openarchive.services.snowbird.SnowbirdGroupViewModel.GroupState
import net.opendasharchive.openarchive.services.snowbird.SnowbirdResult
import net.opendasharchive.openarchive.util.BaseViewModel
import net.opendasharchive.openarchive.util.trackProcessingWithTimeout

class SnowbirdRepoViewModel(
    application: Application,
    private val repository: ISnowbirdRepoRepository
) : BaseViewModel(application) {

    sealed class RepoState {
        data object Idle : RepoState()
        data object Loading : RepoState()
        data class SingleRepoSuccess(val groupKey: String, val repo: SnowbirdRepo) : RepoState()
        data class MultiRepoSuccess(val repos: List<SnowbirdRepo>) : RepoState()
        data class RepoFetchSuccess(val repos: List<SnowbirdRepo>, val isRefresh: Boolean) :
            RepoState()

        data class Error(val error: SnowbirdError) : RepoState()
    }

    private val _repoState = MutableStateFlow<RepoState>(RepoState.Idle)
    val repoState: StateFlow<RepoState> = _repoState.asStateFlow()

    fun createRepo(groupKey: String, repoName: String) {
        viewModelScope.launch {
            _repoState.value = RepoState.Loading
            try {
                val result = processingTracker.trackProcessingWithTimeout(60_000, "create_repo") {
                    repository.createRepo(groupKey, repoName)
                }

                _repoState.value = when (result) {
                    is SnowbirdResult.Success -> RepoState.SingleRepoSuccess(groupKey, result.value)
                    is SnowbirdResult.Error -> RepoState.Error(result.error)
                }
            } catch (e: TimeoutCancellationException) {
                _repoState.value = RepoState.Error(SnowbirdError.TimedOut)
            }
        }
    }

    fun fetchRepos(groupKey: String, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _repoState.value = RepoState.Loading
            try {
                val result = processingTracker.trackProcessingWithTimeout(30_000, "fetch_repos") {
                    repository.fetchRepos(groupKey, forceRefresh)
                }

                _repoState.value = when (result) {
                    is SnowbirdResult.Success -> RepoState.RepoFetchSuccess(
                        result.value,
                        forceRefresh
                    )

                    is SnowbirdResult.Error -> RepoState.Error(result.error)
                }
            } catch (e: TimeoutCancellationException) {
                _repoState.value = RepoState.Error(SnowbirdError.TimedOut)
            }
        }
    }

    fun refreshGroups(groupKey: String) {
        viewModelScope.launch {
            _repoState.value = RepoState.Loading
            try {
                val result = processingTracker.trackProcessingWithTimeout(30_000, "fetch_groups") {
                    repository.refreshGroupContent(groupKey)
                }

                when (result) {
                    is SnowbirdResult.Error -> {
                        AppLogger.e(result.error.friendlyMessage)
                        _repoState.value = RepoState.Error(result.error)
                    }

                    is SnowbirdResult.Success<RefreshGroupResponse> -> {
                        AppLogger.i(result.value.toString())
                    }
                }

            } catch (e: TimeoutCancellationException) {
                _repoState.value = RepoState.Error(SnowbirdError.TimedOut)
            }
        }
    }
}