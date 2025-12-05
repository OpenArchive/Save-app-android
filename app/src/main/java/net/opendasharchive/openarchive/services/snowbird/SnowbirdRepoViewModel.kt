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
                        //TODO: Save Repo List and Media List to DB

                        // Get existing repos for group
                        val existingRepos = SnowbirdRepo.getAllForGroupKey(groupKey)
                        val existingReposMap = existingRepos.associateBy { it.key }

                        result.value.refreshedRepos.forEach  { repoData ->

                            // Log repo errors if any
                            if (!repoData.error.isNullOrEmpty()) {
                                AppLogger.e("Error refreshing repo ${repoData.repoId}: ${repoData.error}")
                            }

                            // Update or create repo
                            val snowbirdRepo = existingReposMap[repoData.repoId] ?: repoData.toRepo().apply {
                                this.groupKey = groupKey
                            }
                            snowbirdRepo.apply {
                                name = repoData.name
                                hash = repoData.hash ?: hash
                                permissions = if (repoData.canWrite) "READ_WRITE" else "READ_ONLY"
                            }.save()

                            // Get existing files for this repo
                            val existingFiles = SnowbirdFileItem.findBy(groupKey, repoData.repoId)
                            val existingFilesMap = existingFiles.associateBy { it.name }

                            // Process all files (not just refreshed ones)
                            repoData.allFiles.forEach { fileName ->
                                val existingFile = existingFilesMap[fileName]

                                if (existingFile == null) {
                                    // Create new file if it doesn't exist
                                    SnowbirdFileItem(
                                        name = fileName,
                                        repoKey = repoData.repoId,
                                        groupKey = groupKey,
                                    ).save()
                                } else {
                                    // Update existing file without overwriting with null
                                    // Note: The refresh API doesn't provide file details,
                                    // so we just maintain the existing file record
                                }
                            }
                        }
                        _repoState.value = RepoState.RefreshGroupContentSuccess
                        fetchRepos(groupKey = groupKey)
                    }
                }

            } catch (e: TimeoutCancellationException) {
                _repoState.value = RepoState.Error(SnowbirdError.TimedOut)
            }
        }
    }
}