package net.opendasharchive.openarchive.features.settings.passcode.passcode_entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.features.settings.passcode.AppConfig
import net.opendasharchive.openarchive.features.settings.passcode.PasscodeRepository

class PasscodeEntryViewModel(
    private val repository: PasscodeRepository,
    private val config: AppConfig
) : ViewModel() {

//    private val _passcode = MutableStateFlow("")
//    val passcode: StateFlow<String> = _passcode.asStateFlow()

//    private val _isCheckingPasscode = MutableStateFlow(false)
//    val isCheckingPasscode: StateFlow<Boolean> = _isCheckingPasscode.asStateFlow()

    private val _uiState =
        MutableStateFlow(PasscodeEntryScreenState(passcodeLength = config.passcodeLength))
    val uiState: StateFlow<PasscodeEntryScreenState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<PasscodeEntryUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

//    val passcodeLength: Int
//        get() = config.passcodeLength

    fun onAction(action: PasscodeEntryScreenAction) {
        when (action) {
            is PasscodeEntryScreenAction.OnNumberClick -> onNumberClick(action.number)
            PasscodeEntryScreenAction.OnBackspaceClick -> onBackspaceClick()
            PasscodeEntryScreenAction.OnSubmit -> onSubmit()
        }
    }

    private fun onNumberClick(number: String) {

//        if (_isCheckingPasscode.value) return
//
//        if (_passcode.value.length >= config.passcodeLength) return
//
//        _passcode.value += number

        _uiState.update { state ->
            if (state.isProcessing || state.passcode.length >= config.passcodeLength) state
            else state.copy(passcode = state.passcode + number)
        }

        if (uiState.value.passcode.length == config.passcodeLength) {
//            _isCheckingPasscode.value = true
            _uiState.update { it.copy(isProcessing = true) }
            checkPasscode()
        }

    }

    private fun onBackspaceClick() {
//        if (_isCheckingPasscode.value || _passcode.value.isEmpty()) return
//        _passcode.value = _passcode.value.dropLast(1)
        _uiState.update { state ->
            if (state.isProcessing || state.passcode.isEmpty()) state
            else state.copy(passcode = state.passcode.dropLast(1))
        }
    }

    private fun onSubmit() {

    }

    private fun checkPasscode() = viewModelScope.launch {
        val currentState = uiState.value
        val currentPasscode = currentState.passcode
        delay(200)
        val (passcodeHash, passcodeSalt) = repository.getPasscodeHashAndSalt()

        if (passcodeHash != null && passcodeSalt != null) {
            val hash = repository.hashPasscode(currentPasscode, passcodeSalt)
            if (hash.contentEquals(passcodeHash)) {
                repository.resetFailedAttempts()
                _uiEvent.send(PasscodeEntryUiEvent.Success)
            } else {
                repository.recordFailedAttempt()
                val remainingAttempts: Int? = if (config.maxRetryLimitEnabled) {
                    repository.getRemainingAttempts()
                } else null

                if (repository.isLockedOut()) {
                    _uiEvent.send(PasscodeEntryUiEvent.LockedOut)
                } else {
                    _uiEvent.send(PasscodeEntryUiEvent.IncorrectPasscode(remainingAttempts))
                }
                _uiState.update { it.copy(shouldShake = true) }
                delay(500)
                _uiState.update { it.copy(shouldShake = false) }
            }
        } else {
            _uiEvent.send(PasscodeEntryUiEvent.PasscodeNotSet)
            _uiState.update { it.copy(shouldShake = true) }
            delay(500)
            _uiState.update { it.copy(shouldShake = false) }
        }

        _uiState.update {
            it.copy(
                passcode = "",
                isProcessing = false
            )
        }
//        _passcode.value = ""
//        _isCheckingPasscode.value = false

    }
}

data class PasscodeEntryScreenState(
    val passcode: String = "",
    val passcodeLength: Int,
    val isProcessing: Boolean = false,
    val shouldShake: Boolean = false
)

sealed class PasscodeEntryScreenAction {
    data class OnNumberClick(val number: String) : PasscodeEntryScreenAction()
    data object OnBackspaceClick : PasscodeEntryScreenAction()
    data object OnSubmit: PasscodeEntryScreenAction()
}

sealed class PasscodeEntryUiEvent {
    data object Success : PasscodeEntryUiEvent()
    data class IncorrectPasscode(val remainingAttempts: Int? = null) : PasscodeEntryUiEvent()
    data object PasscodeNotSet : PasscodeEntryUiEvent()
    data object LockedOut : PasscodeEntryUiEvent()
}