package net.opendasharchive.openarchive.core.config

data class AppConfig(
    val passcodeLength: Int = 6,
    val enableHapticFeedback: Boolean = true,
    val maxRetryLimitEnabled: Boolean = false,
    val biometricAuthEnabled: Boolean = false,
    val maxFailedAttempts: Int = 5,
    val isDwebEnabled: Boolean = false,
    val multipleProjectSelectionMode: Boolean = false,
    val useCustomCamera: Boolean = false,
    val useComposeUploadManager: Boolean = true,
    val autoVerifyPasscode: Boolean = false,
    val useMocks: Boolean = false,
    val simulateErrors: Boolean = false,
    val mockDelayMs: Long = 500L,
)