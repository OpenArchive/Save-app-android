package net.opendasharchive.openarchive.services.storacha.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.services.storacha.model.DelegationRequest
import net.opendasharchive.openarchive.services.storacha.service.StorachaApiService
import retrofit2.HttpException

class StorachaDIDAccessViewModel(
    private val apiService: StorachaApiService,
) : ViewModel() {
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    private val _success = MutableLiveData<Boolean>()
    val success: LiveData<Boolean> get() = _success

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    private val _sessionExpired = MutableLiveData<Boolean>()
    val sessionExpired: LiveData<Boolean> = _sessionExpired

    fun createDelegation(
        sessionId: String,
        userDid: String,
        spaceDid: String,
        expiresIn: Int = 24,
    ) {
        _loading.value = true
        _error.value = null
        _success.value = false

        viewModelScope.launch {
            try {
                val request =
                    DelegationRequest(
                        userDid = userDid,
                        spaceDid = spaceDid,
                        expiresIn = expiresIn,
                    )

                apiService.createDelegation(sessionId, request)
                _success.value = true
            } catch (e: HttpException) {
                if (e.code() == 401) {
                    _sessionExpired.value = true
                }
                _error.value = e.message ?: "Failed to create delegation"
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to create delegation"
            } finally {
                _loading.value = false
            }
        }
    }
}
