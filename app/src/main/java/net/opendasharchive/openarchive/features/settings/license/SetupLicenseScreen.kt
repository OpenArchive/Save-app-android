package net.opendasharchive.openarchive.features.settings.license

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.opendasharchive.openarchive.R
import androidx.compose.ui.res.colorResource
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.settings.CreativeCommonsLicenseManager
import net.opendasharchive.openarchive.services.webdav.CreativeCommonsLicenseContent
import net.opendasharchive.openarchive.services.webdav.LicenseCallbacks
import net.opendasharchive.openarchive.services.webdav.LicenseState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupLicenseScreen(
    onNext: () -> Unit = {},
    onCancel: () -> Unit = {},
    viewModel: SetupLicenseViewModel = viewModel()
) {

    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SetupLicenseEvent.NavigateNext -> onNext()
                is SetupLicenseEvent.NavigateBack -> onCancel()
            }
        }
    }

    SetupLicenseScreenContent(
        state  = state,
        onAction = viewModel::onAction
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupLicenseScreenContent(
    state: SetupLicenseState,
    onAction: (SetupLicenseAction) -> Unit
) {




    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 48.dp, bottom = 16.dp)
    ) {
        // Content section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Description text (hidden in edit mode)
            if (!state.isEditing) {
                Text(
                    text = stringResource(R.string.name_your_server),
                    modifier = Modifier.padding(24.dp),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Server name input
            OutlinedTextField(
                value = state.serverName,
                onValueChange = { onAction(SetupLicenseAction.UpdateServerName(it)) },
                label = { Text(stringResource(R.string.server_name_optional)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                    focusedLabelColor = MaterialTheme.colorScheme.tertiary
                )
            )

            // Creative Commons License Section
            CreativeCommonsLicenseContent(
                licenseState = LicenseState(
                    ccEnabled = state.ccEnabled,
                    allowRemix = state.allowRemix,
                    requireShareAlike = state.requireShareAlike,
                    allowCommercial = state.allowCommercial,
                    cc0Enabled = state.cc0Enabled,
                    licenseUrl = state.licenseUrl
                ),
                licenseCallbacks = object :
                    LicenseCallbacks {
                    override fun onCcEnabledChange(enabled: Boolean) {
                        onAction(SetupLicenseAction.UpdateCcEnabled(enabled))
                    }

                    override fun onAllowRemixChange(allowed: Boolean) {
                        onAction(SetupLicenseAction.UpdateAllowRemix(allowed))
                    }

                    override fun onRequireShareAlikeChange(required: Boolean) {
                        onAction(SetupLicenseAction.UpdateRequireShareAlike(required))
                    }

                    override fun onAllowCommercialChange(allowed: Boolean) {
                        onAction(SetupLicenseAction.UpdateAllowCommercial(allowed))
                    }

                    override fun onCc0EnabledChange(enabled: Boolean) {
                        onAction(SetupLicenseAction.UpdateCc0Enabled(enabled))
                    }
                },
                ccLabelText = stringResource(R.string.set_creative_commons_license_for_all_folders_on_this_server)
            )
        }

        // Button bar (hidden in edit mode)
        if (!state.isEditing) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Cancel button (invisible by default as per original XML)
                OutlinedButton(
                    onClick = { onAction(SetupLicenseAction.Cancel) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(stringResource(R.string.back))
                }

                // Next button
                Button(
                    onClick = { onAction(SetupLicenseAction.Next) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.colorTertiary)
                    )
                ) {
                    Text(
                        text = stringResource(R.string.action_next),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WebDavSetupLicenseScreenPreview() {
    SaveAppTheme {
        SetupLicenseScreenContent(
            state = SetupLicenseState(
                ccEnabled = true
            ),
            onAction = {}
        )
    }
}

class SetupLicenseViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val spaceId: Long = savedStateHandle.get<Long>("spaceId") ?: -1L
    private val isEditing: Boolean = savedStateHandle.get<Boolean>("isEditing") ?: false

    private val _uiState = MutableStateFlow(SetupLicenseState(spaceId = spaceId, isEditing = isEditing))
    val uiState: StateFlow<SetupLicenseState> = _uiState.asStateFlow()

    private val _events = Channel<SetupLicenseEvent>()
    val events = _events.receiveAsFlow()

    private var space: Space? = null

    init {
        loadSpace()
    }

    fun onAction(action: SetupLicenseAction) {
        when (action) {
            is SetupLicenseAction.UpdateServerName -> {
                _uiState.update { it.copy(serverName = action.serverName) }
                updateSpace { space -> 
                    space.name = action.serverName
                    space.save()
                }
            }

            is SetupLicenseAction.Next -> {
                viewModelScope.launch {
                    _events.send(SetupLicenseEvent.NavigateNext)
                }
            }

            is SetupLicenseAction.Cancel -> {
                viewModelScope.launch {
                    _events.send(SetupLicenseEvent.NavigateBack)
                }
            }

            is SetupLicenseAction.UpdateCcEnabled -> {
                _uiState.update { currentState ->
                    if (action.enabled) {
                        // When CC is enabled, start fresh with no options selected
                        currentState.copy(
                            ccEnabled = true,
                            cc0Enabled = false,
                            allowRemix = false,
                            requireShareAlike = false,
                            allowCommercial = false,
                            licenseUrl = null
                        )
                    } else {
                        // When CC is disabled, reset all other CC options
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

            is SetupLicenseAction.UpdateAllowRemix -> {
                _uiState.update { currentState ->
                    currentState.copy(
                        allowRemix = action.allowed,
                        cc0Enabled = if (action.allowed) false else currentState.cc0Enabled,  // Disable CC0 if remix is enabled
                        requireShareAlike = if (!action.allowed) false else currentState.requireShareAlike  // Auto-disable ShareAlike when Remix is disabled
                    )
                }
                generateAndUpdateLicense()
            }

            is SetupLicenseAction.UpdateRequireShareAlike -> {
                _uiState.update { currentState ->
                    currentState.copy(
                        requireShareAlike = action.required,
                        cc0Enabled = if (action.required) false else currentState.cc0Enabled  // Disable CC0 if share alike is enabled
                    )
                }
                generateAndUpdateLicense()
            }

            is SetupLicenseAction.UpdateAllowCommercial -> {
                _uiState.update { currentState ->
                    currentState.copy(
                        allowCommercial = action.allowed,
                        cc0Enabled = if (action.allowed) false else currentState.cc0Enabled  // Disable CC0 if commercial is enabled
                    )
                }
                generateAndUpdateLicense()
            }

            is SetupLicenseAction.UpdateCc0Enabled -> {
                _uiState.update { currentState ->
                    if (action.enabled) {
                        // When CC0 is enabled, disable CC and reset all other options
                        currentState.copy(
                            cc0Enabled = true,
                            ccEnabled = false,
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

    private fun loadSpace() {
        space = if (spaceId == -1L) {
            Space(Space.Type.WEBDAV)
        } else {
            Space.get(spaceId) ?: Space(Space.Type.WEBDAV)
        }

        space?.let { currentSpace ->
            val licenseState = initializeLicenseState(currentSpace.license)
            _uiState.update { currentState ->
                currentState.copy(
                    serverName = currentSpace.name.orEmpty(),
                    ccEnabled = licenseState.ccEnabled,
                    allowRemix = licenseState.allowRemix,
                    requireShareAlike = licenseState.requireShareAlike,
                    allowCommercial = licenseState.allowCommercial,
                    cc0Enabled = licenseState.cc0Enabled,
                    licenseUrl = licenseState.licenseUrl
                )
            }
        }
    }

    private fun updateSpace(action: (Space) -> Unit) {
        space?.let(action)
    }

    private fun initializeLicenseState(currentLicense: String?): SetupLicenseState {
        val isCc0 = currentLicense?.contains("publicdomain/zero", true) ?: false
        val isCC = currentLicense?.contains("creativecommons.org/licenses", true) ?: false
        
        return if (isCc0) {
            // CC0 license detected
            SetupLicenseState(
                ccEnabled = true,
                cc0Enabled = true,
                allowRemix = false,
                allowCommercial = false,
                requireShareAlike = false,
                licenseUrl = currentLicense
            )
        } else if (isCC && currentLicense != null) {
            // Regular CC license detected
            SetupLicenseState(
                ccEnabled = true,
                cc0Enabled = false,
                allowRemix = !(currentLicense.contains("-nd", true)),
                allowCommercial = !(currentLicense.contains("-nc", true)),
                requireShareAlike = !(currentLicense.contains("-nd", true)) && currentLicense.contains("-sa", true),
                licenseUrl = currentLicense
            )
        } else {
            // No license
            SetupLicenseState(
                ccEnabled = false,
                cc0Enabled = false,
                allowRemix = false,  // Changed from true to fix auto-enable bug
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
        updateSpace { space ->
            space.license = newLicense
            space.save()
        }
    }
}

@Immutable
data class SetupLicenseState(
    val serverName: String = "",
    val spaceId: Long = -1L,
    val isEditing: Boolean = false,
    // Creative Commons License state
    val ccEnabled: Boolean = false,
    val allowRemix: Boolean = false,
    val requireShareAlike: Boolean = false,
    val allowCommercial: Boolean = false,
    val cc0Enabled: Boolean = false,
    val licenseUrl: String? = null,
    val isLoading: Boolean = false
)

sealed interface SetupLicenseAction {
    data class UpdateServerName(val serverName: String) : SetupLicenseAction
    data object Next : SetupLicenseAction
    data object Cancel : SetupLicenseAction
    // Creative Commons License actions
    data class UpdateCcEnabled(val enabled: Boolean) : SetupLicenseAction
    data class UpdateAllowRemix(val allowed: Boolean) : SetupLicenseAction
    data class UpdateRequireShareAlike(val required: Boolean) : SetupLicenseAction
    data class UpdateAllowCommercial(val allowed: Boolean) : SetupLicenseAction
    data class UpdateCc0Enabled(val enabled: Boolean) : SetupLicenseAction
}

sealed interface SetupLicenseEvent {
    data object NavigateNext : SetupLicenseEvent
    data object NavigateBack : SetupLicenseEvent
}