package net.opendasharchive.openarchive.services.storacha.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.services.storacha.model.SpaceInfo
import net.opendasharchive.openarchive.services.storacha.service.StorachaApiService
import retrofit2.HttpException

class StorachaBrowseSpacesViewModel(
    private val apiService: StorachaApiService,
) : ViewModel() {
    private val _spaces = MutableLiveData<List<SpaceInfo>>()
    val spaces: LiveData<List<SpaceInfo>> get() = _spaces

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    private val _sessionExpired = MutableLiveData<Boolean>()
    val sessionExpired: LiveData<Boolean> get() = _sessionExpired

    fun clearSessionExpired() {
        _sessionExpired.value = false
    }

    fun loadSpaces(
        userDid: String,
        sessionId: String,
    ) {
        _loading.value = true
        viewModelScope.launch {
            try {
                val spaceInfos = apiService.listSpaces(userDid, sessionId)
                _spaces.value = spaceInfos
            } catch (e: HttpException) {
                if (e.code() == 401) {
                    _sessionExpired.value = true
                }
                _spaces.value = emptyList()
            } catch (e: Exception) {
                _spaces.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }
}
