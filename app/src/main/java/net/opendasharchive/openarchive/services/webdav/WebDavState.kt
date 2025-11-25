package net.opendasharchive.openarchive.services.webdav

import androidx.compose.runtime.Immutable
import net.opendasharchive.openarchive.features.core.UiText

@Immutable
data class WebDavState(
    // Form fields
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val name: String = "",

    // Mode flags
    val isEditMode: Boolean = false,
    val spaceId: Long = -1L,

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

    // Creative Commons License state (for edit mode)
    val ccEnabled: Boolean = false,
    val allowRemix: Boolean = false,
    val requireShareAlike: Boolean = false,
    val allowCommercial: Boolean = false,
    val cc0Enabled: Boolean = false,
    val licenseUrl: String? = null
) {
    val isFormValid: Boolean
        get() = serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()

    val hasUnsavedChanges: Boolean
        get() = isEditMode && isNameChanged
}

sealed interface WebDavAction {
    // Form updates
    data class UpdateServerUrl(val url: String) : WebDavAction
    data class UpdateUsername(val username: String) : WebDavAction
    data class UpdatePassword(val password: String) : WebDavAction
    data class UpdateName(val name: String) : WebDavAction
    data object FixServerUrl : WebDavAction

    // UI actions
    data object TogglePasswordVisibility : WebDavAction
    data object ClearError : WebDavAction

    // Authentication
    data object Authenticate : WebDavAction
    data object Cancel : WebDavAction

    // Edit mode actions
    data object SaveChanges : WebDavAction
    data object RemoveSpace : WebDavAction
    data object ConfirmRemoveSpace : WebDavAction
    data object DiscardChanges : WebDavAction

    // Creative Commons License actions
    data class UpdateCcEnabled(val enabled: Boolean) : WebDavAction
    data class UpdateAllowRemix(val allowed: Boolean) : WebDavAction
    data class UpdateRequireShareAlike(val required: Boolean) : WebDavAction
    data class UpdateAllowCommercial(val allowed: Boolean) : WebDavAction
    data class UpdateCc0Enabled(val enabled: Boolean) : WebDavAction
}

sealed interface WebDavEvent {
    data class NavigateToLicenseSetup(val spaceId: Long) : WebDavEvent
    data object NavigateBack : WebDavEvent
    data object ShowUnsavedChangesDialog : WebDavEvent
    data object ShowRemoveConfirmationDialog : WebDavEvent
    data object ShowSuccessDialog : WebDavEvent
    data class ShowError(val message: UiText) : WebDavEvent
}
