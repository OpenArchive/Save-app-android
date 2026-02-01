package net.opendasharchive.openarchive.services.webdav.presentation.login

import androidx.compose.runtime.Immutable
import net.opendasharchive.openarchive.features.core.UiText

@Immutable
data class WebDavLoginState(
    // Form fields
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
//    val serverUrl: String = "https://nx27277.your-storageshare.de/",
//    val username: String = "Prathieshna",
//    val password: String = "J7wc(ka_4#9!13h&",
    val name: String = "",

    // Field errors
    val serverError: UiText? = null,
    val usernameError: UiText? = null,
    val passwordError: UiText? = null,

    // UI state
    val isLoading: Boolean = false,
    val isCredentialsError: Boolean = false,
    val errorMessage: UiText? = null,
    val isNameChanged: Boolean = false,
    val originalName: String = "",
    val isPasswordVisible: Boolean = false,

    // Original license values (for tracking changes)
    val originalLicenseUrl: String? = null
) {
    val isFormValid: Boolean
        get() = serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()

}

sealed interface WebDavLoginAction {
    // Form updates
    data class UpdateServerUrl(val url: String) : WebDavLoginAction
    data class UpdateUsername(val username: String) : WebDavLoginAction
    data class UpdatePassword(val password: String) : WebDavLoginAction
    data class UpdateName(val name: String) : WebDavLoginAction
    data object FixServerUrl : WebDavLoginAction

    // UI actions
    data object TogglePasswordVisibility : WebDavLoginAction
    data object ClearError : WebDavLoginAction

    // Authentication
    data object Authenticate : WebDavLoginAction
    data object Cancel : WebDavLoginAction
}

sealed interface WebDavLoginEvent {

}
