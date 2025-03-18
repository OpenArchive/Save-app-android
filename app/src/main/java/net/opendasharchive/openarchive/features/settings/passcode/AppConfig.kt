package net.opendasharchive.openarchive.features.settings.passcode

data class AppConfig(
    val passcodeLength: Int = 6,
    val enableHapticFeedback: Boolean = true,
    val maxRetryLimitEnabled: Boolean = false,
    val biometricAuthEnabled: Boolean = false,
    val maxFailedAttempts: Int = 5,
    val isDwebEnabled: Boolean = false,
    val multipleProjectSelectionMode: Boolean = false
)