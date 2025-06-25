package net.opendasharchive.openarchive.services.storacha.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import net.opendasharchive.openarchive.services.storacha.model.LoginRequest
import net.opendasharchive.openarchive.services.storacha.model.LoginResponse
import net.opendasharchive.openarchive.services.storacha.service.StorachaApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class StorachaLoginViewModel(
    private val apiService: StorachaApiService,
) : ViewModel() {
    private val _loginResult = MutableLiveData<Result<LoginResponse>>()
    val loginResult: LiveData<Result<LoginResponse>> = _loginResult

    private var currentSessionId: String? = null

    fun login(
        email: String,
        did: String,
    ) {
        val request = LoginRequest(email = email, did = did)
        apiService.login(request).enqueue(
            object : Callback<LoginResponse> {
                override fun onResponse(
                    call: Call<LoginResponse>,
                    response: Response<LoginResponse>,
                ) {
                    if (response.isSuccessful) {
                        val body = response.body()!!
                        currentSessionId = body.sessionId
                        _loginResult.value = Result.success(body)
                    } else {
                        _loginResult.value =
                            Result.failure(Exception("Login failed: ${response.code()}"))
                    }
                }

                override fun onFailure(
                    call: Call<LoginResponse>,
                    t: Throwable,
                ) {
                    _loginResult.value = Result.failure(t)
                }
            },
        )
    }

    fun getSessionIdOrNull(): String? = currentSessionId
}
