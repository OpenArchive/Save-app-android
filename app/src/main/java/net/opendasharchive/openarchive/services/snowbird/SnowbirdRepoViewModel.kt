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
import net.opendasharchive.openarchive.db.SnowbirdFileItem
import net.opendasharchive.openarchive.db.SnowbirdRepo
import net.opendasharchive.openarchive.db.toRepo
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
        data class RepoFetchSuccess(val repos: List<SnowbirdRepo>, val isRefresh: Boolean) : RepoState()
        data object RefreshGroupContentSuccess: RepoState()
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
                val result = processingTracker.trackProcessingWithTimeout(60_000, "fetch_repos") {
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
                val result = processingTracker.trackProcessingWithTimeout(120_000, "fetch_groups") {
                    repository.refreshGroupContent(groupKey)
                }

                when (result) {
                    is SnowbirdResult.Error -> {
                        AppLogger.e(result.error.friendlyMessage)
                        _repoState.value = RepoState.Error(result.error)
                    }

                    is SnowbirdResult.Success<RefreshGroupResponse> -> {
                        AppLogger.i("Group content refreshed successfully")

                        // Only persist refresh data for repos that belong to this group.
                        val allowedRepoIds: Set<String> = run {
                            val fromNetwork = repository.fetchRepos(groupKey, forceRefresh = true)
                            when (fromNetwork) {
                                is SnowbirdResult.Success -> fromNetwork.value.map { it.key }.toSet()
                                is SnowbirdResult.Error -> {
                                    AppLogger.w("Unable to fetch repos for scoping refresh; falling back to local DB scope: ${fromNetwork.error.friendlyMessage}")
                                    SnowbirdRepo.getAllForGroupKey(groupKey).map { it.key }.toSet()
                                }
                            }
                        }

                        val existingRepos = SnowbirdRepo.getAllForGroupKey(groupKey)
                        val existingReposMap = existingRepos.associateBy { it.key }

                        val repoErrors = mutableListOf<String>()

                        result.value.refreshedRepos.forEach  { repoData ->
                            if (allowedRepoIds.isNotEmpty() && !allowedRepoIds.contains(repoData.repoId)) {
                                AppLogger.e("Refresh returned repo outside group scope. groupKey=$groupKey repoId=${repoData.repoId} name=${repoData.name}")
                                return@forEach
                            }

                            if (!repoData.error.isNullOrEmpty()) {
                                val bucket = classifyRefreshError(repoData.error)
                                val msg = "Repo ${repoData.name} (${repoData.repoId}): $bucket — ${repoData.error}"
                                repoErrors.add(msg)
                                AppLogger.e(msg)
                            }

                            val snowbirdRepo = existingReposMap[repoData.repoId] ?: repoData.toRepo().apply {
                                this.groupKey = groupKey
                            }
                            snowbirdRepo.apply {
                                name = repoData.name
                                hash = repoData.hash ?: hash
                                permissions = if (repoData.canWrite) "READ_WRITE" else "READ_ONLY"
                            }.save()

                            val existingFiles = SnowbirdFileItem.findBy(groupKey, repoData.repoId)
                            val existingFilesMap = existingFiles.associateBy { it.name }

                            repoData.allFiles.forEach { fileName ->
                                val existingFile = existingFilesMap[fileName]

                                if (existingFile == null) {
                                    SnowbirdFileItem(
                                        name = fileName,
                                        repoKey = repoData.repoId,
                                        groupKey = groupKey,
                                    ).save()
                                }
                            }
                        }

                        // Surface per-repo refresh failures while keeping persisted updates.
                        if (repoErrors.isNotEmpty()) {
                            val summary = repoErrors.take(8).joinToString("\n")
                            val suffix = if (repoErrors.size > 8) "\n… and ${repoErrors.size - 8} more" else ""
                            _repoState.value = RepoState.Error(
                                SnowbirdError.GeneralError(
                                    "Some repositories failed to refresh:\n$summary$suffix\n\n(Types: DHT_DISCOVERY vs PEER_DOWNLOAD vs UNKNOWN)"
                                )
                            )
                        } else {
                            _repoState.value = RepoState.RefreshGroupContentSuccess
                        }
                        fetchRepos(groupKey = groupKey)
                    }
                }

            } catch (e: TimeoutCancellationException) {
                _repoState.value = RepoState.Error(SnowbirdError.TimedOut)
            }
        }
    }

    private fun classifyRefreshError(message: String): String {
        val m = message.lowercase()
        return when {
            "dht" in m || "repo root hash" in m -> "DHT_DISCOVERY"
            "download from any peer" in m || "any peer" in m -> "PEER_DOWNLOAD"
            else -> "UNKNOWN"
        }
    }
}
