package net.opendasharchive.openarchive.services.snowbird

import android.app.Application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.db.JoinGroupResponse
import net.opendasharchive.openarchive.db.SnowbirdError
import net.opendasharchive.openarchive.db.SnowbirdGroup
import net.opendasharchive.openarchive.util.BaseViewModel
import net.opendasharchive.openarchive.util.trackProcessingWithTimeout

class SnowbirdGroupViewModel(
    application: Application,
    private val repository: ISnowbirdGroupRepository
) : BaseViewModel(application) {

    sealed class GroupState {
        data object Idle : GroupState()
        data object Loading : GroupState()
        data class JoinGroupSuccess(val group: JoinGroupResponse) : GroupState()
        data class SingleGroupSuccess(val group: SnowbirdGroup) : GroupState()
        data class MultiGroupSuccess(val groups: List<SnowbirdGroup>, val isRefresh: Boolean) : GroupState()
        data class Error(val error: SnowbirdError) : GroupState()
    }

    private val _groupState = MutableStateFlow<GroupState>(GroupState.Idle)
    val groupState: StateFlow<GroupState> = _groupState.asStateFlow()

    private val _currentGroup = MutableStateFlow<SnowbirdGroup?>(null)
    val currentGroup: StateFlow<SnowbirdGroup?> = _currentGroup.asStateFlow()

    fun setCurrentGroup(group: SnowbirdGroup) {
        _currentGroup.value = group
    }

    fun fetchGroup(groupKey: String) {
        viewModelScope.launch {
            _groupState.value = GroupState.Loading
            try {
                val result = processingTracker.trackProcessingWithTimeout(30_000, "fetch_group") {
                    repository.fetchGroup(groupKey)
                }

                _groupState.value = when (result) {
                    is SnowbirdResult.Success -> GroupState.SingleGroupSuccess(result.value)
                    is SnowbirdResult.Error -> GroupState.Error(result.error)
                }
            } catch (e: TimeoutCancellationException) {
                _groupState.value = GroupState.Error(SnowbirdError.TimedOut)
            }
        }
    }

    fun fetchGroups(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _groupState.value = GroupState.Loading
            try {
                val result = processingTracker.trackProcessingWithTimeout(30_000, "fetch_groups") {
                    repository.fetchGroups(forceRefresh)
                }

                _groupState.value = when (result) {
                    is SnowbirdResult.Success -> GroupState.MultiGroupSuccess(result.value, forceRefresh)
                    is SnowbirdResult.Error -> GroupState.Error(result.error)
                }
            } catch (e: TimeoutCancellationException) {
                _groupState.value = GroupState.Error(SnowbirdError.TimedOut)
            }
        }
    }

    fun createGroup(groupName: String) {
        viewModelScope.launch {
            _groupState.value = GroupState.Loading
            try {
                val result = processingTracker.trackProcessingWithTimeout(30_000, "create_group") {
                    repository.createGroup(groupName)
                }

                _groupState.value = when (result) {
                    is SnowbirdResult.Success -> GroupState.SingleGroupSuccess(result.value)
                    is SnowbirdResult.Error -> GroupState.Error(result.error)
                }
            } catch (e: TimeoutCancellationException) {
                _groupState.value = GroupState.Error(SnowbirdError.TimedOut)
            }
        }
    }

    fun joinGroup(uriString: String) {
        viewModelScope.launch {
            _groupState.value = GroupState.Loading
            try {
                val result = processingTracker.trackProcessingWithTimeout(60_000, "join_group") {
                    repository.joinGroup(uriString)
                }

                _groupState.value = when (result) {
                    is SnowbirdResult.Success -> GroupState.JoinGroupSuccess(result.value)
                    is SnowbirdResult.Error -> GroupState.Error(result.error)
                }
            } catch (e: TimeoutCancellationException) {
                _groupState.value = GroupState.Error(SnowbirdError.TimedOut)
            }
        }
    }
}