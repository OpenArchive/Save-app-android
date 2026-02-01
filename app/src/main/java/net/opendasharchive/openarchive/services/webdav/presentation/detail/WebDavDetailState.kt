package net.opendasharchive.openarchive.services.webdav.presentation.detail

import androidx.compose.runtime.Immutable
import net.opendasharchive.openarchive.features.core.UiText

@Immutable
data class WebDavDetailState(

    val spaceId: Long,

    // Form fields
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val name: String = "",

    // UI state
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null,
    val isNameChanged: Boolean = false,
    val originalName: String = "",

    // Creative Commons License state (for edit mode)
    val ccEnabled: Boolean = false,
    val allowRemix: Boolean = false,
    val requireShareAlike: Boolean = false,
    val allowCommercial: Boolean = false,
    val cc0Enabled: Boolean = false,
    val licenseUrl: String? = null,

    // Original license values (for tracking changes)
    val originalLicenseUrl: String? = null
) {

    val hasUnsavedChanges: Boolean
        get() = isNameChanged || licenseUrl != originalLicenseUrl
}

sealed interface WebDavDetailAction {

    // Form updates
    data class UpdateName(val name: String) : WebDavDetailAction

    // Authentication
    data object Cancel : WebDavDetailAction

    // Edit mode actions
    data object SaveChanges : WebDavDetailAction
    data object RemoveSpace : WebDavDetailAction
    data object ConfirmRemoveSpace : WebDavDetailAction
    data object NavigateBack : WebDavDetailAction

    // Creative Commons License actions
    data class UpdateCcEnabled(val enabled: Boolean) : WebDavDetailAction
    data class UpdateAllowRemix(val allowed: Boolean) : WebDavDetailAction
    data class UpdateRequireShareAlike(val required: Boolean) : WebDavDetailAction
    data class UpdateAllowCommercial(val allowed: Boolean) : WebDavDetailAction
    data class UpdateCc0Enabled(val enabled: Boolean) : WebDavDetailAction
}

sealed interface WebDavDetailEvent {

}
