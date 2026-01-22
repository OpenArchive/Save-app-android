package net.opendasharchive.openarchive.services.webdav.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.domain.Vault
import net.opendasharchive.openarchive.features.core.UiImage
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.dialog.ButtonData
import net.opendasharchive.openarchive.features.core.dialog.DialogConfig
import net.opendasharchive.openarchive.features.core.dialog.DialogStateManager
import net.opendasharchive.openarchive.features.core.dialog.DialogType
import net.opendasharchive.openarchive.features.core.dialog.showDialog
import net.opendasharchive.openarchive.features.core.dialog.showWarningDialog
import net.opendasharchive.openarchive.core.repositories.SpaceRepository
import net.opendasharchive.openarchive.features.main.ui.AppRoute
import net.opendasharchive.openarchive.features.main.ui.Navigator
import net.opendasharchive.openarchive.features.settings.CreativeCommonsLicenseManager

class WebDavDetailViewModel(
    private val route: AppRoute.WebDavDetailRoute,
    private val navigator: Navigator,
    private val spaceRepository: SpaceRepository,
    private val dialogManager: DialogStateManager,
) : ViewModel() {

    private lateinit var vault: Vault

    private val _uiState = MutableStateFlow(
        WebDavDetailState(
            spaceId = route.spaceId,
        )
    )
    val uiState: StateFlow<WebDavDetailState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<WebDavDetailEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        loadSpaceData()
    }

    private fun loadSpaceData() = viewModelScope.launch {
        vault = spaceRepository.getSpaceById(route.spaceId) ?: run {
            navigator.navigateBack()
            return@launch
        }

        _uiState.update { currentState ->
            val newState = currentState.copy(
                serverUrl = vault.host,
                username = vault.username,
                password = vault.password,
                name = vault.name,
                originalName = vault.name,
                originalLicenseUrl = vault.licenseUrl
            )

            initializeLicenseState(newState, vault.licenseUrl)
        }
    }

    fun onAction(action: WebDavDetailAction) {

        when (action) {

            is WebDavDetailAction.UpdateName -> {
                val isChanged = action.name.trim() != _uiState.value.originalName
                _uiState.update { it.copy(name = action.name, isNameChanged = isChanged) }
            }

            is WebDavDetailAction.Cancel -> {
                viewModelScope.launch {
                    if (_uiState.value.hasUnsavedChanges) {
                        showUnsavedChangeWarning()
                    } else {
                        navigator.navigateBack()
                    }
                }
            }

            is WebDavDetailAction.SaveChanges -> {
                saveChanges()
            }

            is WebDavDetailAction.RemoveSpace -> removeConfirmDialog()

            is WebDavDetailAction.ConfirmRemoveSpace -> removeSpace()

            is WebDavDetailAction.NavigateBack -> {
                viewModelScope.launch {
                    navigator.navigateBack()
                }
            }

            // Creative Commons License actions
            is WebDavDetailAction.UpdateCcEnabled -> {
                _uiState.update { currentState ->
                    if (action.enabled) {
                        currentState.copy(
                            ccEnabled = true,
                            cc0Enabled = false,
                            allowRemix = false,
                            requireShareAlike = false,
                            allowCommercial = false,
                            licenseUrl = null
                        )
                    } else {
                        currentState.copy(
                            ccEnabled = false,
                            allowRemix = false,
                            requireShareAlike = false,
                            allowCommercial = false,
                            cc0Enabled = false,
                            licenseUrl = null
                        )
                    }
                }
                generateAndUpdateLicense()
            }

            is WebDavDetailAction.UpdateAllowRemix -> {
                _uiState.update { currentState ->
                    currentState.copy(
                        allowRemix = action.allowed,
                        cc0Enabled = if (action.allowed) false else currentState.cc0Enabled,
                        requireShareAlike = if (!action.allowed) false else currentState.requireShareAlike
                    )
                }
                generateAndUpdateLicense()
            }

            is WebDavDetailAction.UpdateRequireShareAlike -> {
                _uiState.update { currentState ->
                    currentState.copy(
                        requireShareAlike = action.required,
                        cc0Enabled = if (action.required) false else currentState.cc0Enabled
                    )
                }
                generateAndUpdateLicense()
            }

            is WebDavDetailAction.UpdateAllowCommercial -> {
                _uiState.update { currentState ->
                    currentState.copy(
                        allowCommercial = action.allowed,
                        cc0Enabled = if (action.allowed) false else currentState.cc0Enabled
                    )
                }
                generateAndUpdateLicense()
            }

            is WebDavDetailAction.UpdateCc0Enabled -> {
                _uiState.update { currentState ->
                    if (action.enabled) {
                        currentState.copy(
                            cc0Enabled = true,
                            allowRemix = false,
                            requireShareAlike = false,
                            allowCommercial = false
                        )
                    } else {
                        currentState.copy(cc0Enabled = false)
                    }
                }
                generateAndUpdateLicense()
            }
        }
    }

    private fun saveChanges() = viewModelScope.launch {
        val currentState = _uiState.value
        val enteredName = currentState.name.trim()

        // Update both name and license (license is already set in generateAndUpdateLicense)
        val updatedVault = vault.copy(name = enteredName)
        spaceRepository.updateSpace(route.spaceId, updatedVault)

        _uiState.update {
            it.copy(
                originalName = enteredName,
                isNameChanged = false,
                originalLicenseUrl = currentState.licenseUrl
            )
        }

        dialogManager.showDialog(dialogManager.requireResourceProvider()) {
            type = DialogType.Success
            title = UiText.Resource(R.string.label_success_title)
            message = UiText.Resource(R.string.msg_edit_server_success)
            icon = UiImage.DrawableResource(R.drawable.ic_done)
            positiveButton {
                text = UiText.Resource(R.string.lbl_got_it)
                action = {
                    navigator.navigateBack()
                }
            }
        }
    }

    private fun removeSpace() {
        viewModelScope.launch {
            val isSuccess = spaceRepository.deleteSpace(route.spaceId)
            if (isSuccess) {
                navigator.navigateBack()
            }
        }
    }

    private fun initializeLicenseState(
        currentState: WebDavDetailState,
        currentLicense: String?
    ): WebDavDetailState {
        val isCc0 = currentLicense?.contains("publicdomain/zero", true) ?: false
        val isCC = currentLicense?.contains("creativecommons.org/licenses", true) ?: false

        return if (isCc0) {
            currentState.copy(
                ccEnabled = true,
                cc0Enabled = true,
                allowRemix = false,
                allowCommercial = false,
                requireShareAlike = false,
                licenseUrl = currentLicense
            )
        } else if (isCC) {
            currentState.copy(
                ccEnabled = true,
                cc0Enabled = false,
                allowRemix = !(currentLicense.contains("-nd", true)),
                allowCommercial = !(currentLicense.contains("-nc", true)),
                requireShareAlike = !(currentLicense.contains(
                    "-nd",
                    true
                )) && currentLicense.contains("-sa", true),
                licenseUrl = currentLicense
            )
        } else {
            currentState.copy(
                ccEnabled = false,
                cc0Enabled = false,
                allowRemix = false,
                allowCommercial = false,
                requireShareAlike = false,
                licenseUrl = null
            )
        }
    }

    private fun generateAndUpdateLicense() {
        val currentState = _uiState.value
        val newLicense = CreativeCommonsLicenseManager.generateLicenseUrl(
            ccEnabled = currentState.ccEnabled,
            allowRemix = currentState.allowRemix,
            requireShareAlike = currentState.requireShareAlike,
            allowCommercial = currentState.allowCommercial,
            cc0Enabled = currentState.cc0Enabled
        )

        _uiState.update { it.copy(licenseUrl = newLicense) }

        // Don't save immediately - let saveChanges() handle persistence
        // This prevents duplicate space records when both name and license are changed

        vault = vault.copy(licenseUrl = newLicense)

        viewModelScope.launch {
            spaceRepository.updateSpace(route.spaceId, vault)
        }
    }

    private fun showUnsavedChangeWarning() {
        dialogManager.showWarningDialog(
            title = UiText.Resource(R.string.unsaved_changes),
            message = UiText.Resource(R.string.do_you_want_to_save),
            onDone = {
                saveChanges()
            },
            onCancel = {
                navigator.navigateBack()
            }
        )
    }

    private fun removeConfirmDialog() {
        val config = DialogConfig(
            type = DialogType.Warning,
            title = UiText.Resource(R.string.remove_from_app),
            message = UiText.Resource(R.string.are_you_sure_you_want_to_remove_this_server_from_the_app),
            icon = UiImage.DrawableResource(R.drawable.ic_trash),
            destructiveButton = ButtonData(
                text = UiText.Resource(R.string.lbl_remove),
                action = { removeSpace() }
            ),
            neutralButton = ButtonData(
                text = UiText.Resource(R.string.lbl_Cancel),
                action = {}
            )
        )

        dialogManager.showDialog(config)
    }
}
