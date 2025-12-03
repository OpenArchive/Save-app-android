package net.opendasharchive.openarchive.services.storacha.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.services.storacha.model.LoginRequest
import net.opendasharchive.openarchive.services.storacha.model.LoginResponse
import net.opendasharchive.openarchive.services.storacha.model.VerifyRequest
import net.opendasharchive.openarchive.services.storacha.model.VerifyResponse
import net.opendasharchive.openarchive.services.storacha.service.StorachaApiService
import net.opendasharchive.openarchive.services.storacha.util.Ed25519Utils
import net.opendasharchive.openarchive.services.storacha.util.SecureStorage

class StorachaLoginViewModel(
    application: Application,
    private val apiService: StorachaApiService,
) : AndroidViewModel(application) {
    private val _loginResult = MutableLiveData<Result<LoginResponse>>()
    val loginResult: LiveData<Result<LoginResponse>> = _loginResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val verifyResult = MutableLiveData<Result<VerifyResponse>>()
    private var currentSessionId: String? = null
    private var currentChallenge: String? = null
    private var currentChallengeId: String? = null
    private val secureStorage = SecureStorage(application, "storacha_did_keys")

    fun login(
        email: String,
        did: String,
    ) {
        // Prevent duplicate requests
        if (_isLoading.value == true) {
            return
        }

        _isLoading.value = true
        val request = LoginRequest(email = email, did = did)
        viewModelScope.launch {
            try {
                val response = apiService.login(request)
                currentSessionId = response.sessionId
                currentChallenge = response.challenge
                currentChallengeId = response.challengeId

                // If we have a challenge, automatically sign and verify it
                if (response.challenge != null && response.challengeId != null) {
                    signAndVerifyChallenge(
                        email,
                        did,
                        response.challenge,
                        response.challengeId,
                        response.sessionId,
                        response,
                    )
                } else {
                    // No challenge needed (subsequent login)
                    _loginResult.value = Result.success(response)
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _loginResult.value = Result.failure(e)
                _isLoading.value = false
            }
        }
    }

    private fun signAndVerifyChallenge(
        email: String,
        did: String,
        challenge: String,
        challengeId: String,
        sessionId: String,
        originalResponse: LoginResponse,
    ) {
        viewModelScope.launch {
            try {
                // Get the private key from secure storage
                val privateKey = secureStorage.getPrivateKey()
                if (privateKey == null) {
                    _loginResult.value =
                        Result.failure(Exception("No private key found. Please regenerate your DID."))
                    _isLoading.value = false
                    return@launch
                }

                // Sign the challenge
                val signature = Ed25519Utils.signChallenge(challenge, privateKey)

                // Send verification request
                val verifyRequest =
                    VerifyRequest(
                        did = did,
                        challengeId = challengeId,
                        signature = signature,
                        sessionId = sessionId,
                        email = email,
                    )

                val verifyResponse = apiService.verify(verifyRequest)
                verifyResult.value = Result.success(verifyResponse)

                // After successful verification, use the original response
                // The server now considers this session verified
                _loginResult.value = Result.success(originalResponse)
                _isLoading.value = false
            } catch (e: Exception) {
                _loginResult.value =
                    Result.failure(Exception("Challenge verification failed: ${e.message}"))
                _isLoading.value = false
            }
        }
    }
}
