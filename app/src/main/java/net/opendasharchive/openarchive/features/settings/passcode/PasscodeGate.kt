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

    private val _locked = MutableStateFlow(Prefs.passcodeEnabled)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    override fun onStart(owner: LifecycleOwner) {
        _locked.value = shouldLockNow()
    }

    override fun onPause(owner: LifecycleOwner) {
        _locked.value = Prefs.passcodeEnabled
    }

    override fun onStop(owner: LifecycleOwner) {
        _locked.value = Prefs.passcodeEnabled
    }

    fun unlock() {
        _locked.value = false
    }

    private fun shouldLockNow(): Boolean {
        if (!Prefs.passcodeEnabled) return false
        if (flowState.isPasscodeFlowActive.value) return false
        return true
    }
}

class PasscodeFlowState {
    private val _isPasscodeFlowActive = MutableStateFlow(false)
    val isPasscodeFlowActive = _isPasscodeFlowActive.asStateFlow()

    fun setActive(active: Boolean) {
        _isPasscodeFlowActive.value = active
    }
}
