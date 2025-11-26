package net.opendasharchive.openarchive.services.webdav

import androidx.core.net.toUri
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
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.settings.CreativeCommonsLicenseManager
import java.io.IOException

class WebDavViewModel(
    private val repository: WebDavRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        const val ARG_VAL_NEW_SPACE = -1L
        private const val REMOTE_PHP_ADDRESS = "/remote.php/webdav/"
    }

    private val spaceId: Long = savedStateHandle.get<Long>("space_id") ?: ARG_VAL_NEW_SPACE

    private var space: Space = if (spaceId != ARG_VAL_NEW_SPACE) {
        Space.get(spaceId) ?: Space(Space.Type.WEBDAV)
    } else {
        Space(Space.Type.WEBDAV)
    }

    private val _uiState = MutableStateFlow(WebDavState())
    val uiState: StateFlow<WebDavState> = _uiState.asStateFlow()

    private val _events = Channel<WebDavEvent>()
    val events = _events.receiveAsFlow()

    init {
        loadSpaceData()
    }

    private fun loadSpaceData() {
        val isEditMode = spaceId != ARG_VAL_NEW_SPACE

        if (isEditMode) {
            _uiState.update { currentState ->
                val newState = currentState.copy(
                    isEditMode = true,
                    spaceId = spaceId,
                    serverUrl = space.host,
                    username = space.username,
                    password = space.password,
                    name = space.name,
                    originalName = space.name
                )
                initializeLicenseState(newState, space.license)
            }
        } else {
            _uiState.update { it.copy(isEditMode = false, spaceId = ARG_VAL_NEW_SPACE) }
        }
    }

    fun onAction(action: WebDavAction) {
        when (action) {
            is WebDavAction.UpdateServerUrl -> {
                _uiState.update {
                    it.copy(
                        serverUrl = action.url,
                        serverError = null,
                        isCredentialsError = false
                    )
                }
            }

            is WebDavAction.FixServerUrl -> {
                val currentUrl = _uiState.value.serverUrl
                if (currentUrl.isNotBlank()) {
                    val fixedUrl = fixSpaceUrl(currentUrl)
                    if (fixedUrl != null && fixedUrl.toString() != currentUrl) {
                        _uiState.update {
                            it.copy(
                                serverUrl = fixedUrl.toString(),
                                serverError = null
                            )
                        }
                    }
                }
            }

            is WebDavAction.UpdateUsername -> {
                _uiState.update {
                    it.copy(
                        username = action.username,
                        usernameError = null,
                        isCredentialsError = false
                    )
                }
            }

            is WebDavAction.UpdatePassword -> {
                _uiState.update {
                    it.copy(
                        password = action.password,
                        passwordError = null,
                        isCredentialsError = false
                    )
                }
            }

            is WebDavAction.UpdateName -> {
                val isChanged = action.name.trim() != _uiState.value.originalName
                _uiState.update { it.copy(name = action.name, isNameChanged = isChanged) }
            }

            is WebDavAction.TogglePasswordVisibility -> {
                _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            }

            is WebDavAction.ClearError -> {
                _uiState.update {
                    it.copy(
                        isCredentialsError = false,
                        serverError = null,
                        usernameError = null,
                        passwordError = null,
                        errorMessage = null
                    )
                }
            }

            is WebDavAction.Authenticate -> {
                performAuthentication()
            }

            is WebDavAction.Cancel -> {
                viewModelScope.launch {
                    if (_uiState.value.hasUnsavedChanges) {
                        _events.send(WebDavEvent.ShowUnsavedChangesDialog)
                    } else {
                        _events.send(WebDavEvent.NavigateBack)
                    }
                }
            }

            is WebDavAction.SaveChanges -> {
                saveChanges()
            }

            is WebDavAction.RemoveSpace -> {
                viewModelScope.launch {
                    _events.send(WebDavEvent.ShowRemoveConfirmationDialog)
                }
            }

            is WebDavAction.ConfirmRemoveSpace -> {
                removeSpace()
            }

            is WebDavAction.DiscardChanges -> {
                viewModelScope.launch {
                    _events.send(WebDavEvent.NavigateBack)
                }
            }

            // Creative Commons License actions
            is WebDavAction.UpdateCcEnabled -> {
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

            is WebDavAction.UpdateAllowRemix -> {
                _uiState.update { currentState ->
                    currentState.copy(
                        allowRemix = action.allowed,
                        cc0Enabled = if (action.allowed) false else currentState.cc0Enabled,
                        requireShareAlike = if (!action.allowed) false else currentState.requireShareAlike
                    )
                }
                generateAndUpdateLicense()
            }

            is WebDavAction.UpdateRequireShareAlike -> {
                _uiState.update { currentState ->
                    currentState.copy(
                        requireShareAlike = action.required,
                        cc0Enabled = if (action.required) false else currentState.cc0Enabled
                    )
                }
                generateAndUpdateLicense()
            }

            is WebDavAction.UpdateAllowCommercial -> {
                _uiState.update { currentState ->
                    currentState.copy(
                        allowCommercial = action.allowed,
                        cc0Enabled = if (action.allowed) false else currentState.cc0Enabled
                    )
                }
                generateAndUpdateLicense()
            }

            is WebDavAction.UpdateCc0Enabled -> {
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

    private fun performAuthentication() {
        val currentState = _uiState.value

        // Validate fields
        var hasError = false
        var updatedState = currentState

        val fixedUrl = fixSpaceUrl(currentState.serverUrl)
        if (fixedUrl == null) {
            updatedState = updatedState.copy(serverError = UiText.StringResource(R.string.error_field_required))
            hasError = true
        }

        if (currentState.username.isBlank()) {
            updatedState = updatedState.copy(usernameError = UiText.StringResource(R.string.error_field_required))
            hasError = true
        }

        if (currentState.password.isBlank()) {
            updatedState = updatedState.copy(passwordError = UiText.StringResource(R.string.error_field_required))
            hasError = true
        }

        if (hasError) {
            _uiState.update { updatedState }
            return
        }

        // Update space with form values
        space.host = fixedUrl.toString()
        space.username = currentState.username
        space.password = currentState.password

        // Check for duplicate credentials
        val existing = Space.get(Space.Type.WEBDAV, space.host, space.username)
        if (existing.isNotEmpty() && existing[0].id != space.id) {
            viewModelScope.launch {
                _events.send(WebDavEvent.ShowError(UiText.StringResource(R.string.you_already_have_a_server_with_these_credentials)))
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, serverUrl = space.host) }

        viewModelScope.launch {
            try {
                repository.testConnection(space)
                space.save()
                Space.current = space

                _uiState.update { it.copy(isLoading = false) }
                _events.send(WebDavEvent.NavigateToLicenseSetup(space.id))
            } catch (e: IOException) {
                _uiState.update { it.copy(isLoading = false) }
                e.printStackTrace()
                when {
                    e.message?.startsWith("401") == true -> {
                        _uiState.update {
                            it.copy(
                                isCredentialsError = true,
                                usernameError = UiText.DynamicString(" "),
                                passwordError = UiText.DynamicString(" ")
                            )
                        }
                    }

                    // Invalid server URL errors (unable to resolve, 404, 400, etc.)
                    e.message?.contains("Unable to resolve host", ignoreCase = true) == true ||
                    e.message?.startsWith("404") == true ||
                    e.message?.startsWith("400") == true ||
                    e.message?.startsWith("403") == true -> {
                        _uiState.update { it.copy(serverError = UiText.DynamicString(" ")) }
                        _events.send(WebDavEvent.ShowError(UiText.StringResource(R.string.web_dav_host_error)))
                    }

                    else -> {
                        // Other server errors (500, etc.)
                        _uiState.update { it.copy(serverError = UiText.DynamicString(" ")) }
                        _events.send(WebDavEvent.ShowError(UiText.DynamicString(e.localizedMessage ?: "An error occurred")))
                    }
                }
            }
        }
    }

    private fun fixSpaceUrl(url: String?): android.net.Uri? {
        if (url.isNullOrBlank()) return null

        val uri = url.toUri()
        val builder = uri.buildUpon()

        if (uri.scheme != "https") {
            builder.scheme("https")
        }

        if (uri.authority.isNullOrBlank()) {
            builder.authority(uri.path)
            builder.path(REMOTE_PHP_ADDRESS)
        } else if (uri.path.isNullOrBlank() || uri.path == "/") {
            builder.path(REMOTE_PHP_ADDRESS)
        }

        return builder.build()
    }

    private fun saveChanges() {
        val enteredName = _uiState.value.name.trim()
        space.name = enteredName
        space.save()

        _uiState.update {
            it.copy(
                originalName = enteredName,
                isNameChanged = false
            )
        }

        viewModelScope.launch {
            _events.send(WebDavEvent.ShowSuccessDialog)
        }
    }

    private fun removeSpace() {
        viewModelScope.launch {
            space.delete()
            _events.send(WebDavEvent.NavigateBack)
        }
    }

    private fun initializeLicenseState(currentState: WebDavState, currentLicense: String?): WebDavState {
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
        } else if (isCC && currentLicense != null) {
            currentState.copy(
                ccEnabled = true,
                cc0Enabled = false,
                allowRemix = !(currentLicense.contains("-nd", true)),
                allowCommercial = !(currentLicense.contains("-nc", true)),
                requireShareAlike = !(currentLicense.contains("-nd", true)) && currentLicense.contains("-sa", true),
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

        if (_uiState.value.isEditMode) {
            space.license = newLicense
            space.save()
        }
    }

    fun getToolbarTitle(): String {
        return if (!_uiState.value.isEditMode) {
            "Private Server"
        } else {
            when {
                space.name.isNotBlank() -> space.name
                space.friendlyName.isNotBlank() -> space.friendlyName
                else -> "Private Server"
            }
        }
    }
}
