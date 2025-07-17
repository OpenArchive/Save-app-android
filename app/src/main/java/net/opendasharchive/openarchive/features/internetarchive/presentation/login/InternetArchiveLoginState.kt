package net.opendasharchive.openarchive.features.internetarchive.presentation.login

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Immutable
import net.opendasharchive.openarchive.features.internetarchive.domain.model.InternetArchive

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
    data object Login : InternetArchiveLoginAction

    data object Cancel : InternetArchiveLoginAction

    data class LoginSuccess(val value: InternetArchive) : InternetArchiveLoginAction

    data class LoginError(val value: Throwable) : InternetArchiveLoginAction

    data object ErrorClear : InternetArchiveLoginAction

    data object CreateLogin : InternetArchiveLoginAction {
        const val URI = "https://archive.org/account/signup"
    }

    data class UpdateUsername(val value: String) : InternetArchiveLoginAction
    data class UpdatePassword(val value: String) : InternetArchiveLoginAction
}
