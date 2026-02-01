package net.opendasharchive.openarchive.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.features.main.ui.AppRoute
import net.opendasharchive.openarchive.features.main.ui.Navigator
import net.opendasharchive.openarchive.util.Hbks
import net.opendasharchive.openarchive.util.Prefs

data class ProofModeSettingsUiState(
    val isProofModeEnabled: Boolean = Prefs.getBoolean(Prefs.USE_PROOFMODE, false),
    val isKeyEncryptionEnabled: Boolean = Prefs.useProofModeKeyEncryption,
    val biometricTitleResId: Int = R.string.prefs_proofmode_key_encryption_title_all,
    val isLoading: Boolean = false
)

sealed interface ProofModeSettingsAction {
    data class ToggleProofMode(val enabled: Boolean) : ProofModeSettingsAction
    data object NavigateBack : ProofModeSettingsAction
    data class OpenUrl(val url: String) : ProofModeSettingsAction
}

sealed interface ProofModeSettingsEvent {
    data class ShowToast(val messageResId: Int) : ProofModeSettingsEvent
    data object RequestPermissions : ProofModeSettingsEvent
    data class EnrollBiometrics(val intent: android.content.Intent) : ProofModeSettingsEvent
}

class ProofModeSettingsViewModel(
    private val navigator: Navigator,
    private val route: AppRoute.ProofModeSettings
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProofModeSettingsUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<ProofModeSettingsEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        updateBiometricTitle()
    }

    private fun updateBiometricTitle() {
        // We'll need access to context or a way to determine device availability.
        // For now, mirroring the logic from ProofModeSettingsActivity.Fragment
        // This might need context from the screen or a service.
    }

    fun onAction(action: ProofModeSettingsAction) {
        when (action) {
            is ProofModeSettingsAction.ToggleProofMode -> handleToggleProofMode(action.enabled)
            is ProofModeSettingsAction.NavigateBack -> navigator.navigateBack()
            is ProofModeSettingsAction.OpenUrl -> { /* Handled in Screen with LocalUriHandler or similar */ }
        }
    }

    private fun handleToggleProofMode(enabled: Boolean) {
        if (enabled) {
            viewModelScope.launch {
                _uiEvent.emit(ProofModeSettingsEvent.RequestPermissions)
            }
        } else {
            Prefs.putBoolean(Prefs.USE_PROOFMODE, false)
            _uiState.update { it.copy(isProofModeEnabled = false) }
        }
    }

    fun onPermissionsResult(granted: Boolean) {
        if (granted) {
            Prefs.putBoolean(Prefs.USE_PROOFMODE, true)
            _uiState.update { it.copy(isProofModeEnabled = true) }
        } else {
            Prefs.putBoolean(Prefs.USE_PROOFMODE, false)
            _uiState.update { it.copy(isProofModeEnabled = false) }
            viewModelScope.launch {
                _uiEvent.emit(ProofModeSettingsEvent.ShowToast(R.string.phone_permission_required))
            }
        }
    }

    fun setBiometricTitle(resId: Int) {
        _uiState.update { it.copy(biometricTitleResId = resId) }
    }
    
    fun setKeyEncryptionEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isKeyEncryptionEnabled = enabled) }
    }
}
