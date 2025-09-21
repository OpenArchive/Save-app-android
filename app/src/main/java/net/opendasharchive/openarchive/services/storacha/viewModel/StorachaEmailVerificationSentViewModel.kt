package net.opendasharchive.openarchive.services.storacha.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.services.storacha.model.SessionValidationResponse
import net.opendasharchive.openarchive.services.storacha.service.StorachaApiService
import net.opendasharchive.openarchive.services.storacha.util.StorachaAccountManager

class StorachaEmailVerificationSentViewModel(
    application: Application,
    private val apiService: StorachaApiService,
    private val sessionId: String,
) : AndroidViewModel(application) {
    private val _navigateNext = MutableLiveData<Unit>()
    val navigateNext: LiveData<Unit> = _navigateNext

    private val _showTimeoutDialog = MutableLiveData<Unit>()
    val showTimeoutDialog: LiveData<Unit> = _showTimeoutDialog

    private val accountManager = StorachaAccountManager(application)
    private var pollingJob: Job? = null
    private var attemptCount = 0
    private val maxAttempts = 30

    init {
        // Check if already verified before starting to poll
        if (!accountManager.isCurrentAccountVerified()) {
            startPollingVerificationStatus()
        } else {
            // Already verified, navigate immediately
            _navigateNext.postValue(Unit)
        }
    }

    private fun startPollingVerificationStatus() {
        pollingJob =
            viewModelScope.launch {
                while (attemptCount < maxAttempts) {
                    try {
                        val response: SessionValidationResponse = apiService.validateSession(sessionId)
                        if (response.valid && response.verified == 1) {
                            // Update account verification status in secure storage
                            val currentAccount = accountManager.getCurrentAccount()
                            currentAccount?.email?.let { email ->
                                accountManager.updateAccountVerification(email, true)
                            }
                            _navigateNext.postValue(Unit)
                            return@launch
                        }
                    } catch (_: Exception) {
                        // Optional: log error
                        // Continue polling even on error
                    }

                    attemptCount++
                    if (attemptCount >= maxAttempts) {
                        _showTimeoutDialog.postValue(Unit)
                        break
                    }

                    delay(2000)
                }
            }
    }

    fun resumePolling() {
        if (pollingJob?.isActive != true && !accountManager.isCurrentAccountVerified()) {
            startPollingVerificationStatus()
        }
    }

    fun pausePolling() {
        pollingJob?.cancel()
    }

    fun tryAgain() {
        // Reset attempt counter and restart polling
        attemptCount = 0
        pausePolling()
        startPollingVerificationStatus()
    }

    fun resetAttemptCounter() {
        attemptCount = 0
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
