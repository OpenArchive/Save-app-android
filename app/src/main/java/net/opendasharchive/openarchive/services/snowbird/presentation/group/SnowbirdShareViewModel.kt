package net.opendasharchive.openarchive.services.snowbird.presentation.group

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.db.DwebDao
import net.opendasharchive.openarchive.extensions.urlEncode

data class SnowbirdShareState(
    val isLoading: Boolean = false,
    val groupName: String = "",
    val qrContent: String = ""
)

sealed interface SnowbirdShareAction {
    data class LoadGroup(val groupKey: String) : SnowbirdShareAction
    data object ShareQrImage : SnowbirdShareAction
    data object Cancel : SnowbirdShareAction
}

sealed interface SnowbirdShareEvent {
    data object NavigateBack : SnowbirdShareEvent
    data object ShareQrImageExternal : SnowbirdShareEvent
}

class SnowbirdShareViewModel(
    private val dwebDao: DwebDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(SnowbirdShareState())
    val uiState: StateFlow<SnowbirdShareState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SnowbirdShareEvent>()
    val events = _events.asSharedFlow()

    fun onAction(action: SnowbirdShareAction) {
        when (action) {
            is SnowbirdShareAction.LoadGroup -> loadGroup(action.groupKey)
            is SnowbirdShareAction.ShareQrImage -> {
                viewModelScope.launch {
                    _events.emit(SnowbirdShareEvent.ShareQrImageExternal)
                }
            }
            is SnowbirdShareAction.Cancel -> {
                viewModelScope.launch {
                    _events.emit(SnowbirdShareEvent.NavigateBack)
                }
            }
        }
    }

    private fun loadGroup(groupKey: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val vaultWithDweb = dwebDao.getVaultWithDwebByKey(groupKey)
            val groupName = vaultWithDweb?.vault?.name ?: "Unknown Group"
            val actualUri = vaultWithDweb?.dwebMetadata?.vaultKey ?: groupKey
            val qrContent = "$actualUri&name=${groupName.urlEncode()}"

            _uiState.update {
                it.copy(
                    isLoading = false,
                    groupName = groupName,
                    qrContent = qrContent
                )
            }
        }
    }
}
