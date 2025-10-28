package net.opendasharchive.openarchive.services.storacha.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.services.storacha.service.StorachaApiService
import retrofit2.HttpException

data class DidAccount(
    val did: String,
)

class StorachaViewDIDsViewModel(
    private val apiService: StorachaApiService,
) : ViewModel() {
    private val _dids = MutableLiveData<List<DidAccount>>()
    val dids: LiveData<List<DidAccount>> get() = _dids

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    private val _sessionExpired = MutableLiveData<Boolean>()
    val sessionExpired: LiveData<Boolean> = _sessionExpired

    fun loadDIDs(
        sessionId: String,
        spaceDid: String,
    ) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val response =
                    apiService.listDelegationsBySpace(
                        sessionId = sessionId,
                        spaceDid = spaceDid,
                    )
                val didAccounts = response.users?.map { DidAccount(it) } ?: emptyList()
                _dids.value = didAccounts
            } catch (e: HttpException) {
                if (e.code() == 401) {
                    _sessionExpired.value = true
                }
                _error.value = e.message ?: "Failed to load DIDs"
                _dids.value = emptyList()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load DIDs"
                _dids.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }

    fun revokeDID(
        sessionId: String,
        spaceDid: String,
        account: DidAccount,
    ) {
        viewModelScope.launch {
            try {
                val request =
                    net.opendasharchive.openarchive.services.storacha.model.DelegationRevokeRequest(
                        userDid = account.did,
                        spaceDid = spaceDid,
                    )
                apiService.revokeDelegation(sessionId, request)

                // Remove from local list on success
                val currentDids = _dids.value?.toMutableList() ?: mutableListOf()
                currentDids.remove(account)
                _dids.value = currentDids
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to revoke DID"
            }
        }
    }
}
