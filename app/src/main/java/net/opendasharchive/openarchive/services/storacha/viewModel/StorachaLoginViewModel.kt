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
import net.opendasharchive.openarchive.services.storacha.util.KeyStorage

class StorachaLoginViewModel(
    application: Application,
    private val apiService: StorachaApiService,
) : AndroidViewModel(application) {
    private val _loginResult = MutableLiveData<Result<LoginResponse>>()
    val loginResult: LiveData<Result<LoginResponse>> = _loginResult

    private val _verifyResult = MutableLiveData<Result<VerifyResponse>>()
    val verifyResult: LiveData<Result<VerifyResponse>> = _verifyResult

    private var currentSessionId: String? = null
    private var currentChallenge: String? = null
    private var currentChallengeId: String? = null
    private val keyStorage = KeyStorage(application)

    fun login(
        email: String,
        did: String,
    ) {
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
                }
            } catch (e: Exception) {
                _loginResult.value = Result.failure(e)
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
                val privateKey = keyStorage.getPrivateKey()
                if (privateKey == null) {
                    _loginResult.value =
                        Result.failure(Exception("No private key found. Please regenerate your DID."))
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
                _verifyResult.value = Result.success(verifyResponse)

                // After successful verification, use the original response
                // The server now considers this session verified
                _loginResult.value = Result.success(originalResponse)
            } catch (e: Exception) {
                _loginResult.value =
                    Result.failure(Exception("Challenge verification failed: ${e.message}"))
            }
        }
    }

    fun getSessionIdOrNull(): String? = currentSessionId
}
