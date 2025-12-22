package net.opendasharchive.openarchive.services.webdav.login

import androidx.core.net.toUri
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
import net.opendasharchive.openarchive.analytics.api.AnalyticsManager
import net.opendasharchive.openarchive.features.main.ui.AppRoute
import net.opendasharchive.openarchive.features.main.ui.Navigator
import net.opendasharchive.openarchive.services.webdav.WebDavRepository
import java.io.IOException

class WebDavLoginViewModel(
    private val navigator: Navigator,
    private val repository: WebDavRepository,
    private val analyticsManager: AnalyticsManager
) : ViewModel() {

    companion object Companion {
        const val ARG_VAL_NEW_SPACE = -1L
        private const val REMOTE_PHP_ADDRESS = "/remote.php/webdav/"
    }

    private var space: Space = Space(Space.Type.WEBDAV)

    private val _uiState = MutableStateFlow(WebDavLoginState())
    val uiState: StateFlow<WebDavLoginState> = _uiState.asStateFlow()

    private val _events = Channel<WebDavLoginEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: WebDavLoginAction) {
        when (action) {
            is WebDavLoginAction.UpdateServerUrl -> {
                _uiState.update {
                    it.copy(
                        serverUrl = action.url,
                        serverError = null,
                        isCredentialsError = false
                    )
                }
            }

            is WebDavLoginAction.FixServerUrl -> {
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

            is WebDavLoginAction.UpdateUsername -> {
                _uiState.update {
                    it.copy(
                        username = action.username,
                        usernameError = null,
                        isCredentialsError = false
                    )
                }
            }

            is WebDavLoginAction.UpdatePassword -> {
                _uiState.update {
                    it.copy(
                        password = action.password,
                        passwordError = null,
                        isCredentialsError = false
                    )
                }
            }

            is WebDavLoginAction.UpdateName -> {
                val isChanged = action.name.trim() != _uiState.value.originalName
                _uiState.update { it.copy(name = action.name, isNameChanged = isChanged) }
            }

            is WebDavLoginAction.TogglePasswordVisibility -> {
                _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            }

            is WebDavLoginAction.ClearError -> {
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

            is WebDavLoginAction.Authenticate -> {
                performAuthentication()
            }

            is WebDavLoginAction.Cancel -> {
                viewModelScope.launch {
                    navigator.navigateBack()
                }
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
            updatedState = updatedState.copy(serverError = UiText.Resource(R.string.error_field_required))
            hasError = true
        }

        if (currentState.username.isBlank()) {
            updatedState = updatedState.copy(usernameError = UiText.Resource(R.string.error_field_required))
            hasError = true
        }

        if (currentState.password.isBlank()) {
            updatedState = updatedState.copy(passwordError = UiText.Resource(R.string.error_field_required))
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
                _events.send(WebDavLoginEvent.ShowError(UiText.Resource(R.string.you_already_have_a_server_with_these_credentials)))
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, serverUrl = space.host) }

        viewModelScope.launch {
            try {
                repository.testConnection(space)

                // Check if this is a new backend or existing one
                val isNewBackend = space.id == null || space.id == 0L

                space.save()
                Space.current = space

                // Track backend configuration
                analyticsManager.trackBackendConfigured(
                    backendType = Space.Type.WEBDAV.friendlyName,
                    isNew = isNewBackend
                )

                _uiState.update { it.copy(isLoading = false) }

                // Navigate to Setup License
                navigator.navigateTo(AppRoute.SetupLicenseRoute(spaceId = space.id, spaceType = Space.Type.WEBDAV))

            } catch (e: IOException) {
                _uiState.update { it.copy(isLoading = false) }
                e.printStackTrace()
                when {
                    e.message?.startsWith("401") == true -> {
                        _uiState.update {
                            it.copy(
                                isCredentialsError = true,
                                usernameError = UiText.Dynamic(" "),
                                passwordError = UiText.Dynamic(" ")
                            )
                        }
                    }

                    // Invalid server URL errors (unable to resolve, 404, 400, etc.)
                    e.message?.contains("Unable to resolve host", ignoreCase = true) == true ||
                    e.message?.startsWith("404") == true ||
                    e.message?.startsWith("400") == true ||
                    e.message?.startsWith("403") == true -> {
                        _uiState.update { it.copy(serverError = UiText.Dynamic(" ")) }
                        _events.send(WebDavLoginEvent.ShowError(UiText.Resource(R.string.web_dav_host_error)))
                    }

                    else -> {
                        // Other server errors (500, etc.)
                        _uiState.update { it.copy(serverError = UiText.Dynamic(" ")) }
                        _events.send(WebDavLoginEvent.ShowError(UiText.Dynamic(e.localizedMessage ?: "An error occurred")))
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

}
