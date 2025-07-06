package net.opendasharchive.openarchive.services.storacha.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.services.storacha.model.SpaceInfo
import net.opendasharchive.openarchive.services.storacha.service.StorachaApiService

class StorachaBrowseSpacesViewModel(
    private val apiService: StorachaApiService,
) : ViewModel() {
    private val _spaces = MutableLiveData<List<SpaceInfo>>()
    val spaces: LiveData<List<SpaceInfo>> get() = _spaces

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    fun loadSpaces(userDid: String) {
        _loading.value = true
        viewModelScope.launch {
            try {
                val spaceInfos = apiService.listSpaces(userDid)
                _spaces.value = spaceInfos
            } catch (e: Exception) {
                _spaces.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }
}
