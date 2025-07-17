package net.opendasharchive.openarchive.services.storacha.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.services.storacha.model.LoginRequest
import net.opendasharchive.openarchive.services.storacha.model.LoginResponse
import net.opendasharchive.openarchive.services.storacha.service.StorachaApiService

class StorachaLoginViewModel(
    application: Application,
    private val apiService: StorachaApiService,
) : AndroidViewModel(application) {
    private val _loginResult = MutableLiveData<Result<LoginResponse>>()
    val loginResult: LiveData<Result<LoginResponse>> = _loginResult

    private var currentSessionId: String? = null

    fun login(
        email: String,
        did: String,
    ) {
        val request = LoginRequest(email = email, did = did)
        viewModelScope.launch {
            try {
                val response = apiService.login(request)
                currentSessionId = response.sessionId
                _loginResult.value = Result.success(response)
            } catch (e: Exception) {
                _loginResult.value = Result.failure(e)
            }
        }
    }

    fun getSessionIdOrNull(): String? = currentSessionId
}
