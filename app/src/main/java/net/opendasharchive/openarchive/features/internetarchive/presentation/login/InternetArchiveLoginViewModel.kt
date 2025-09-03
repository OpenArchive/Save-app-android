package net.opendasharchive.openarchive.features.internetarchive.presentation.login

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.internetarchive.domain.usecase.InternetArchiveLoginUseCase
import net.opendasharchive.openarchive.features.internetarchive.domain.usecase.ValidateLoginCredentialsUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

class InternetArchiveLoginViewModel(
    private val validateLoginCredentials: ValidateLoginCredentialsUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), KoinComponent {

    private val spaceId: Long = savedStateHandle.get<Long>("space_id") ?: -1L

    val space: Space = if (spaceId == -1L) {
        Space(Space.Type.INTERNET_ARCHIVE)
    } else {
        Space.get(spaceId) ?: Space(Space.Type.INTERNET_ARCHIVE)
    }

    private val loginUseCase: InternetArchiveLoginUseCase by inject {
        parametersOf(space)
    }

    private val _uiState = MutableStateFlow(InternetArchiveLoginState())
    val uiState: StateFlow<InternetArchiveLoginState> = _uiState.asStateFlow()

    private val _events = Channel<InternetArchiveLoginEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: InternetArchiveLoginAction) {
        when (action) {
            is InternetArchiveLoginAction.UpdateUsername -> {
                _uiState.update { currentState ->
                    currentState.copy(
                        username = action.username,
                        isValid = validateLoginCredentials(action.username, currentState.password)
                    )
                }
            }

            is InternetArchiveLoginAction.UpdatePassword -> {
                _uiState.update { currentState ->
                    currentState.copy(
                        password = action.password,
                        isValid = validateLoginCredentials(currentState.username, action.password)
                    )
                }
            }

            is InternetArchiveLoginAction.Login -> {
                performLogin()
            }

            is InternetArchiveLoginAction.Cancel -> {
                viewModelScope.launch {
                    _events.send(InternetArchiveLoginEvent.NavigateBack)
                }
            }

            is InternetArchiveLoginAction.CreateLogin -> {
                viewModelScope.launch {
                    _events.send(InternetArchiveLoginEvent.NavigateToSignup)
                }
            }

            is InternetArchiveLoginAction.ErrorClear -> {
                _uiState.update { it.copy(isLoginError = false) }
            }
        }
    }

    private fun performLogin() {
        _uiState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            val currentState = _uiState.value
            loginUseCase(currentState.username, currentState.password)
                .onSuccess { ia ->
                    _uiState.update { it.copy(isBusy = false) }
                    _events.send(InternetArchiveLoginEvent.LoginSuccess(space.id))
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoginError = true, isBusy = false) }
                    _events.send(InternetArchiveLoginEvent.LoginError(error))
                }
        }
    }
}
