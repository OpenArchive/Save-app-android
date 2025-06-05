package net.opendasharchive.openarchive.features.settings.passcode

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

enum class AppHapticFeedbackType {
    KeyPress,
    Error,
}

class HapticManager(
    private val appConfig: AppConfig
) {
    private var hapticFeedback: HapticFeedback? = null

    fun init(hapticFeedback: HapticFeedback) {
        this.hapticFeedback = hapticFeedback
    }

    fun performHapticFeedback(type: AppHapticFeedbackType) {
        if (!appConfig.enableHapticFeedback) return

        hapticFeedback?.let {
            // Using Compose HapticFeedback
            when (type) {
                AppHapticFeedbackType.KeyPress -> it.performHapticFeedback(HapticFeedbackType.LongPress)
                AppHapticFeedbackType.Error -> it.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }

    fun clear() {
        hapticFeedback = null
    }
}