package net.opendasharchive.openarchive.features.internetarchive.presentation.details

import androidx.compose.runtime.Immutable

@Immutable
data class InternetArchiveDetailsState(
    val userName: String = "",
    val screenName: String = "",
    val email: String = "",
    val license: String? = null,
    val isLoading: Boolean = false,
    // Creative Commons License state
    val ccEnabled: Boolean = false,
    val allowRemix: Boolean = true,
    val requireShareAlike: Boolean = false,
    val allowCommercial: Boolean = false,
    val licenseUrl: String? = null
)

sealed interface InternetArchiveDetailsAction {
    data object Remove : InternetArchiveDetailsAction
    data object Cancel : InternetArchiveDetailsAction
    data class UpdateLicense(val license: String?) : InternetArchiveDetailsAction
    // Creative Commons License actions
    data class UpdateCcEnabled(val enabled: Boolean) : InternetArchiveDetailsAction
    data class UpdateAllowRemix(val allowed: Boolean) : InternetArchiveDetailsAction
    data class UpdateRequireShareAlike(val required: Boolean) : InternetArchiveDetailsAction
    data class UpdateAllowCommercial(val allowed: Boolean) : InternetArchiveDetailsAction
}

sealed interface InternetArchiveDetailsEvent {
    data object NavigateBack : InternetArchiveDetailsEvent
}
