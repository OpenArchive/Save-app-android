package net.opendasharchive.openarchive.services.internetarchive.presentation.details

import androidx.compose.runtime.Immutable

@Immutable
data class InternetArchiveDetailsState(
    val spaceId: Long,

    val userName: String = "",
    val screenName: String = "",
    val email: String = "",
    val license: String? = null,

    val isLoading: Boolean = false,

    // Creative Commons License state
    val ccEnabled: Boolean = false,
    val allowRemix: Boolean = false,
    val requireShareAlike: Boolean = false,
    val allowCommercial: Boolean = false,
    val cc0Enabled: Boolean = false,
    val licenseUrl: String? = null
)

sealed interface InternetArchiveDetailsAction {
    data object RemoveSpace : InternetArchiveDetailsAction

    data class UpdateLicense(val license: String?) : InternetArchiveDetailsAction
    // Creative Commons License actions
    data class UpdateCcEnabled(val enabled: Boolean) : InternetArchiveDetailsAction
    data class UpdateAllowRemix(val allowed: Boolean) : InternetArchiveDetailsAction
    data class UpdateRequireShareAlike(val required: Boolean) : InternetArchiveDetailsAction
    data class UpdateAllowCommercial(val allowed: Boolean) : InternetArchiveDetailsAction
    data class UpdateCc0Enabled(val enabled: Boolean) : InternetArchiveDetailsAction

    data object ShowRemoveSpaceDialog : InternetArchiveDetailsAction
}

sealed interface InternetArchiveDetailsEvent {
    data object ShowRemoveSpaceDialog : InternetArchiveDetailsEvent
}
