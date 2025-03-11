package net.opendasharchive.openarchive.features.main

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import net.opendasharchive.openarchive.core.logger.AppLogger

class MainViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(
        MainUiState(
            currentPagerItem = 0
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {

        AppLogger.i("MainViewModel initialized....")
    }


    fun log(msg: String) {
        AppLogger.i("MainViewModel: $msg")
    }

    fun updateCurrentPagerItem(page: Int) {
        _uiState.update { it.copy(currentPagerItem = page) }
    }

    fun getCurrentPagerItem(): Int = _uiState.value.currentPagerItem
}

data class MainUiState(
    val currentPagerItem: Int
)