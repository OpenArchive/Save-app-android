package net.opendasharchive.openarchive.core.security

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.opendasharchive.openarchive.util.Prefs

/**
 * Manages window security requirements (like screenshot prevention) reactively.
 */
class SecurityManager(
    private val sharedPreferences: SharedPreferences
) {
    private val _isSecureRequired = MutableStateFlow(calculateSecureRequirement())
    val isSecureRequired: StateFlow<Boolean> = _isSecureRequired.asStateFlow()

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == Prefs.PASSCODE_ENABLED || key == Prefs.PROHIBIT_SCREENSHOTS) {
            _isSecureRequired.value = calculateSecureRequirement()
        }
    }

    init {
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    private fun calculateSecureRequirement(): Boolean {
        // We use Prefs object for convenience as it logic is already centralized there
        // but we react to the SharedPreferences changes.
        return Prefs.passcodeEnabled || Prefs.prohibitScreenshots
    }

    /**
     * Call this when the manager is no longer needed (e.g. app process termination),
     * though as a singleton it will live for the app lifecycle.
     */
    fun teardown() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
    }
}
