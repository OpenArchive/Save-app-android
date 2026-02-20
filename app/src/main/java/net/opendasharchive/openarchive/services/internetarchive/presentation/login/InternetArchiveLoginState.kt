package net.opendasharchive.openarchive.services.internetarchive.presentation.login

import androidx.compose.runtime.Immutable

@Immutable
data class InternetArchiveLoginState(
    val username: String = "",
    val password: String = "",
    val isUsernameError: Boolean = false,
    val isPasswordError: Boolean = false,
    val isLoginError: Boolean = false,
    val isBusy: Boolean = false,
    val isValid: Boolean = false,
)

sealed interface InternetArchiveLoginAction {
    data class UpdateUsername(val username: String) : InternetArchiveLoginAction
    data class UpdatePassword(val password: String) : InternetArchiveLoginAction
    data object Login : InternetArchiveLoginAction
    data object Cancel : InternetArchiveLoginAction
    data object ErrorClear : InternetArchiveLoginAction
}

sealed interface InternetArchiveLoginEvent {
    data class LoginError(val error: Throwable) : InternetArchiveLoginEvent
}
