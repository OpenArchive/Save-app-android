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
import net.opendasharchive.openarchive.features.main.ui.AppRoute
import net.opendasharchive.openarchive.features.main.ui.Navigator
import net.opendasharchive.openarchive.db.DwebDao
import net.opendasharchive.openarchive.services.snowbird.util.SnowbirdJoinCode

data class SnowbirdShareState(
    val isLoading: Boolean = false,
    val groupName: String = "",
    val qrContent: String = ""
)

sealed interface SnowbirdShareAction {
    data object ShareQrImage : SnowbirdShareAction
    data object Cancel : SnowbirdShareAction
}

sealed interface SnowbirdShareEvent {
    data object ShareQrImageExternal : SnowbirdShareEvent
}

class SnowbirdShareViewModel(
    private val navigator: Navigator,
    private val route: AppRoute.SnowbirdShareRoute,
    private val dwebDao: DwebDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(SnowbirdShareState())
    val uiState: StateFlow<SnowbirdShareState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SnowbirdShareEvent>()
    val events = _events.asSharedFlow()

    init {
        loadGroup(route.groupKey)
    }

    fun onAction(action: SnowbirdShareAction) {
        when (action) {
            is SnowbirdShareAction.ShareQrImage -> {
                viewModelScope.launch {
                    _events.emit(SnowbirdShareEvent.ShareQrImageExternal)
                }
            }
            is SnowbirdShareAction.Cancel -> {
                navigator.navigateBack()
            }
        }
    }

    private fun loadGroup(groupKey: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val vaultWithDweb = dwebDao.getVaultWithDwebByKey(groupKey)
            val groupName = vaultWithDweb?.vault?.name ?: "Unknown Group"
            val groupUri = vaultWithDweb?.vault?.host?.trim().orEmpty()
            val qrContent = SnowbirdJoinCode.build(groupUri, groupName)

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
