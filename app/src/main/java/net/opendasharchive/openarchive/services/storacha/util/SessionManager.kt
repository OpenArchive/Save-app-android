package net.opendasharchive.openarchive.services.storacha.util

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.opendasharchive.openarchive.services.storacha.model.LoginRequest
import net.opendasharchive.openarchive.services.storacha.model.VerifyRequest
import net.opendasharchive.openarchive.services.storacha.service.StorachaApiService
import timber.log.Timber

class SessionManager(
    private val apiService: StorachaApiService,
    private val accountManager: StorachaAccountManager,
    private val secureStorage: SecureStorage,
) {
    companion object {
        private const val TAG = "SessionManager"
    }

    // Mutex to prevent concurrent refresh attempts
    private val refreshMutex = Mutex()

    // Track if a refresh is currently in progress
    private var isRefreshing = false

    /**
     * Validation result containing session status details
     */
    data class ValidationResult(
        val isValid: Boolean,
        val isVerified: Boolean,
        val needsEmailVerification: Boolean = false,
    )

    /**
     * Validates the current session by calling GET /auth/session.
     * Returns a ValidationResult with detailed session status.
     */
    suspend fun validateSessionDetailed(): ValidationResult {
        val currentAccount = accountManager.getCurrentAccount()
        if (currentAccount == null) {
            Timber.tag(TAG).d("No current account found")
            return ValidationResult(isValid = false, isVerified = false)
        }

        return try {
            val response = apiService.validateSession(currentAccount.sessionId)
            val isVerified = response.verified == 1
            val needsEmailVerification = response.valid && !isVerified

            Timber
                .tag(TAG)
                .d("Session validation result: valid=${response.valid}, verified=${response.verified}")

            ValidationResult(
                isValid = response.valid && isVerified,
                isVerified = isVerified,
                needsEmailVerification = needsEmailVerification,
            )
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error validating session")
            ValidationResult(isValid = false, isVerified = false)
        }
    }

    /**
     * Validates the current session by calling GET /auth/session.
     * Returns true if the session is valid and verified, false otherwise.
     */
    suspend fun validateSession(): Boolean = validateSessionDetailed().isValid

    /**
     * Attempts to refresh the session using the stored DID and challenge-response flow.
     * Returns Result.success with new session ID on success, or Result.failure on error.
     *
     * This method uses a mutex to prevent concurrent refresh attempts.
     */
    suspend fun refreshSession(): Result<String> =
        refreshMutex.withLock {
            if (isRefreshing) {
                Timber.tag(TAG).d("Refresh already in progress, skipping")
                return Result.failure(Exception("Refresh already in progress"))
            }

            isRefreshing = true
            try {
                performRefresh()
            } finally {
                isRefreshing = false
            }
        }

    /**
     * Internal method to perform the actual session refresh.
     */
    private suspend fun performRefresh(): Result<String> {
        val currentAccount = accountManager.getCurrentAccount()
        if (currentAccount == null) {
            Timber.tag(TAG).e("No current account found for refresh")
            return Result.failure(Exception("No current account"))
        }

        val did = currentAccount.did
        val email = currentAccount.email

        if (did.isNullOrBlank()) {
            Timber.tag(TAG).e("No DID found in current account")
            return Result.failure(Exception("No DID found"))
        }

        return try {
            // Step 1: Call login to get challenge
            Timber.tag(TAG).d("Calling login to get challenge for refresh")
            val loginResponse = apiService.login(LoginRequest(email, did))

            val challenge = loginResponse.challenge
            val challengeId = loginResponse.challengeId

            if (challenge.isNullOrBlank() || challengeId.isNullOrBlank()) {
                Timber.tag(TAG).e("No challenge received from login")
                return Result.failure(Exception("No challenge received"))
            }

            // Step 2: Sign the challenge
            Timber.tag(TAG).d("Signing challenge for verification")
            val signature = signChallenge(challenge)

            // Step 3: Verify the signature
            Timber.tag(TAG).d("Verifying signature")
            val verifyResponse =
                apiService.verify(
                    VerifyRequest(
                        did = did,
                        challengeId = challengeId,
                        signature = signature,
                        sessionId = loginResponse.sessionId,
                        email = email,
                    ),
                )

            val newSessionId = verifyResponse.sessionId
            Timber.tag(TAG).d("Session refresh successful, new session ID obtained")

            // Step 4: Update the account with new session ID
            updateSessionId(email, newSessionId)

            Result.success(newSessionId)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error refreshing session")
            Result.failure(e)
        }
    }

    /**
     * Signs a challenge using the stored Ed25519 private key.
     */
    private fun signChallenge(challenge: String): String {
        val privateKey =
            secureStorage.getPrivateKey()
                ?: throw IllegalStateException("No private key found in secure storage")
        return Ed25519Utils.signChallenge(challenge, privateKey)
    }

    /**
     * Updates the session ID for the given account email.
     */
    private fun updateSessionId(
        email: String,
        newSessionId: String,
    ) {
        val account = accountManager.getAccount(email)

        if (account != null) {
            // Use addAccount which will update existing account
            accountManager.addAccount(
                email = email,
                sessionId = newSessionId,
                isVerified = account.isVerified,
                did = account.did,
            )

            Timber.tag(TAG).d("Session ID updated for account: $email")
        } else {
            Timber.tag(TAG).e("Account not found for session ID update: $email")
        }
    }

    /**
     * Removes the current account from the accounts list (used when session expires and cannot be refreshed).
     * This is different from just clearing the current account pointer - it actually deletes the account.
     */
    fun removeCurrentAccount() {
        val currentAccount = accountManager.getCurrentAccount()
        if (currentAccount != null) {
            accountManager.removeAccount(currentAccount.email)
            Timber.tag(TAG).d("Invalid account removed: ${currentAccount.email}")
        } else {
            accountManager.clearCurrentAccount()
            Timber.tag(TAG).d("Current account pointer cleared")
        }
    }
}
