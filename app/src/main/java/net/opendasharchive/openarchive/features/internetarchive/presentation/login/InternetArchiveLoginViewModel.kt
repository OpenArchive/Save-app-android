package net.opendasharchive.openarchive.features.internetarchive.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.core.domain.VaultType
import net.opendasharchive.openarchive.features.internetarchive.domain.usecase.InternetArchiveLoginUseCase
import net.opendasharchive.openarchive.features.internetarchive.domain.usecase.ValidateLoginCredentialsUseCase
import net.opendasharchive.openarchive.features.main.ui.AppRoute
import net.opendasharchive.openarchive.features.main.ui.Navigator
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class InternetArchiveLoginViewModel(
    private val route: AppRoute.IALoginRoute,
    private val navigator: Navigator,
    private val validateLoginCredentials: ValidateLoginCredentialsUseCase,
) : ViewModel(), KoinComponent {

    private val loginUseCase: InternetArchiveLoginUseCase by inject()

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
                    navigator.navigateBack()
                }
            }

            is InternetArchiveLoginAction.ErrorClear -> {
                _uiState.update { it.copy(isLoginError = false, isUsernameError = false, isPasswordError = false) }
            }
        }
    }

    private fun performLogin() {
        _uiState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            val currentState = _uiState.value
            loginUseCase.invoke(currentState.username, currentState.password)
                .onSuccess { vaultId ->
                    _uiState.update { it.copy(isBusy = false) }

                    navigator.navigateTo(AppRoute.SetupLicenseRoute(spaceId = vaultId, spaceType = VaultType.INTERNET_ARCHIVE))
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoginError = true, isUsernameError = true, isPasswordError = true, isBusy = false) }
                    _events.send(InternetArchiveLoginEvent.LoginError(error))
                }
        }
    }
}
