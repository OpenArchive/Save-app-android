package net.opendasharchive.openarchive.services.storacha.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.services.storacha.Account
import net.opendasharchive.openarchive.services.storacha.service.StorachaApiService

class StorachaViewDIDsViewModel(
    private val apiService: StorachaApiService,
) : ViewModel() {
    private val _dids = MutableLiveData<List<Account>>()
    val dids: LiveData<List<Account>> get() = _dids

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

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
                val didAccounts = response.users?.map { Account(it) } ?: emptyList()
                _dids.value = didAccounts
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
        account: Account,
    ) {
        viewModelScope.launch {
            try {
                val request =
                    net.opendasharchive.openarchive.services.storacha.model.DelegationRevokeRequest(
                        userDid = account.email, // email field contains the DID
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
