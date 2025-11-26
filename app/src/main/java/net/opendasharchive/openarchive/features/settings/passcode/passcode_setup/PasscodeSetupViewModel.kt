package net.opendasharchive.openarchive.features.settings.passcode.passcode_setup

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

class PasscodeSetupViewModel(
    private val repository: PasscodeRepository,
    private val config: AppConfig
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(PasscodeSetupUiState(passcodeLength = config.passcodeLength))
    val uiState: StateFlow<PasscodeSetupUiState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<PasscodeSetupUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    fun onAction(action: PasscodeSetupUiAction) {
        when (action) {
            is PasscodeSetupUiAction.OnNumberClick -> onNumberClick(action.number)
            PasscodeSetupUiAction.OnBackspaceClick -> onBackspaceClick()
            PasscodeSetupUiAction.OnCancel -> onCancel()
            PasscodeSetupUiAction.OnSubmit -> onSubmit()
        }
    }

    private fun onNumberClick(number: String) {

//        val state = uiState.value // current state
//
//        if (state.isProcessing) return // Block input during processing
//
//        if (state.passcode.length >= config.passcodeLength) return // block race conditions
//
//        _uiState.update { it.copy(passcode = it.passcode + number) }
//
//        val newState = uiState.value // updated state
//
//        if (newState.passcode.length == config.passcodeLength) {
//            _uiState.update { it.copy(isProcessing = true) }
//            processPasscodeEntry()
//        }

        _uiState.update { state ->
            // Block input during processing or if the passcode length is already reached
            if (state.isProcessing || state.passcode.length >= config.passcodeLength) state
            else state.copy(passcode = state.passcode + number)
        }

//        // Process passcode only when the required length is reached
//        if (_uiState.value.passcode.length == config.passcodeLength) {
//            _uiState.update { it.copy(isProcessing = true) }
//            processPasscodeEntry()
//        }
    }

    private fun onBackspaceClick() {
//        val state = uiState.value // current state
//        if (state.isConfirming || state.passcode.isEmpty()) return
//        _uiState.update { it.copy(passcode = it.passcode.dropLast(1)) }

        _uiState.update { state->
            // Remove the last digit from the passcode if not confirming or empty
            if (state.isProcessing || state.passcode.isEmpty()) state
            else state.copy(passcode = state.passcode.dropLast(1))
        }
    }

    private fun onSubmit() {
        val state = _uiState.value

        // Ensure passcode length is correct before submission
        if (state.passcode.length == config.passcodeLength) {
            _uiState.update { it.copy(isProcessing = true) }
            processPasscodeEntry()
        }
    }

    private fun processPasscodeEntry() = viewModelScope.launch {
        val state = uiState.value // current state
        if (state.isConfirming) {
            // Confirmation step
            val passcode = state.passcode
            delay(100)
            val confirmPasscode = state.confirmPasscode
            if (passcode == confirmPasscode) {
                val salt = repository.generateSalt()
                val hash = repository.hashPasscode(passcode, salt)
                repository.storePasscodeHashAndSalt(hash, salt)
                _uiEvent.send(PasscodeSetupUiEvent.PasscodeSet)
            } else {
                _uiState.update { it.copy(shouldShake = true) }
                _uiEvent.send(PasscodeSetupUiEvent.PasscodeDoNotMatch)
                delay(500) // Allow time for shake animation to complete
                reset()
            }
        } else {
            // Assigning passcode to confirm passcode and moving to confirmation step
            delay(500)
            val passcode = state.passcode
            _uiState.update {
                it.copy(
                    confirmPasscode = passcode,
                    passcode = "",
                    isConfirming = true,
                    isProcessing = false
                )
            }
        }
    }

    private fun reset() {
        _uiState.update {
            it.copy(
                passcode = "",
                confirmPasscode = "",
                isConfirming = false,
                isProcessing = false,
                shouldShake = false
            )
        }
    }

    private fun onCancel() = viewModelScope.launch {
        _uiEvent.send(PasscodeSetupUiEvent.PasscodeCancelled)
    }
}

data class PasscodeSetupUiState(
    val passcode: String = "",
    val confirmPasscode: String = "",
    val passcodeLength: Int,
    val isConfirming: Boolean = false,
    val isProcessing: Boolean = false,
    val shouldShake: Boolean = false,
)

sealed class PasscodeSetupUiAction {
    data class OnNumberClick(val number: String) : PasscodeSetupUiAction()
    data object OnBackspaceClick : PasscodeSetupUiAction()
    data object OnCancel : PasscodeSetupUiAction()
    data object OnSubmit: PasscodeSetupUiAction()
}

sealed class PasscodeSetupUiEvent {
    data object PasscodeSet : PasscodeSetupUiEvent()
    data object PasscodeDoNotMatch : PasscodeSetupUiEvent()
    data object PasscodeCancelled : PasscodeSetupUiEvent()
}