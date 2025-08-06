package net.opendasharchive.openarchive.services.storacha.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.services.storacha.model.SessionValidationResponse
import net.opendasharchive.openarchive.services.storacha.service.StorachaApiService

class StorachaEmailVerificationSentViewModel(
    private val apiService: StorachaApiService,
    private val sessionId: String
) : ViewModel() {
    private val _navigateNext = MutableLiveData<Unit>()
    val navigateNext: LiveData<Unit> = _navigateNext

    init {
        pollVerificationStatus()
    }

    private fun pollVerificationStatus() {
        viewModelScope.launch {
            while (true) {
                try {
                    val response: SessionValidationResponse = apiService.validateSession(sessionId)
                    if (response.valid && response.verified == 1) {
                        _navigateNext.postValue(Unit)
                        break
                    }
                } catch (_: Exception) {
                    // Optional: log error
                }
                delay(2000)
            }
        }
    }
}
