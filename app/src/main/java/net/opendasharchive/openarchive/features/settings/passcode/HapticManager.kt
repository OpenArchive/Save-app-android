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
    fun perform(haptic: HapticFeedback, type: AppHapticFeedbackType) {
        if (!appConfig.enableHapticFeedback) return

        val composeType = when (type) {
            AppHapticFeedbackType.KeyPress -> HapticFeedbackType.TextHandleMove
            AppHapticFeedbackType.Error -> HapticFeedbackType.LongPress
        }
        haptic.performHapticFeedback(composeType)
    }
}
