package net.opendasharchive.openarchive.features.settings.passcode

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import net.opendasharchive.openarchive.features.core.BaseActivity

class BiometricAuthenticator(
    private val activity: BaseActivity, private val config: AppConfig
) {

    private val biometricManager = BiometricManager.from(activity)
    private var biometricPrompt: BiometricPrompt? = null

    fun isBiometricAvailable(): Boolean {
        return config.biometricAuthEnabled && biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticate(
        onSuccess: () -> Unit,
        onFailure: (errorMessage: String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onFailure("Biometric authentication failed: $errString")
                }

                override fun onAuthenticationFailed() {
                    onFailure("Biometric authentication failed")
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Authentication")
            .setSubtitle("Use your fingerprint or face to unlock")
            .setNegativeButtonText("Use Passcode")
            .build()

        biometricPrompt?.authenticate(promptInfo)
    }

    fun cancelAuthentication() {
        biometricPrompt?.cancelAuthentication()
    }
}