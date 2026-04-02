package net.opendasharchive.openarchive.features.settings.passcode

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.opendasharchive.openarchive.util.Prefs

class PasscodeGate(
    private val flowState: PasscodeFlowState
) : DefaultLifecycleObserver {

    /**
     * Tracks whether the user has successfully authenticated in the current process session.
     * Cleared on onStop so the app re-locks whenever it is fully backgrounded.
     */
    private var isAuthenticated = false

    private val _locked = MutableStateFlow(Prefs.passcodeEnabled)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    override fun onStart(owner: LifecycleOwner) {
        _locked.value = shouldLockNow()
    }

    // Intentionally no onPause override — do NOT re-lock mid-session when the app loses focus
    // briefly (e.g. system dialog, notification shade). Only re-lock on full background (onStop).

    override fun onStop(owner: LifecycleOwner) {
        if (Prefs.passcodeEnabled) {
            isAuthenticated = false
            _locked.value = true
        }
    }

    /** Call this once the user has successfully entered their passcode. */
    fun unlock() {
        isAuthenticated = true
        _locked.value = false
    }

    private fun shouldLockNow(): Boolean {
        if (!Prefs.passcodeEnabled) return false
        if (flowState.isPasscodeFlowActive.value) return false
        return !isAuthenticated
    }
}

class PasscodeFlowState {
    private val _isPasscodeFlowActive = MutableStateFlow(false)
    val isPasscodeFlowActive = _isPasscodeFlowActive.asStateFlow()

    fun setActive(active: Boolean) {
        _isPasscodeFlowActive.value = active
    }
}
