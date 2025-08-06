package net.opendasharchive.openarchive.services.storacha.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.services.storacha.model.UploadEntry
import net.opendasharchive.openarchive.services.storacha.model.UploadResponse
import net.opendasharchive.openarchive.services.storacha.service.StorachaApiService
import net.opendasharchive.openarchive.services.storacha.util.BridgeUploader

class StorachaMediaViewModel(
    private val apiService: StorachaApiService,
    private val bridgeUploader: BridgeUploader = BridgeUploader(),
) : ViewModel() {
    private val _media = MutableLiveData<List<UploadEntry>>(emptyList())
    val media: LiveData<List<UploadEntry>> get() = _media

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    private var currentCursor: String? = null
    private var hasMoreData: Boolean = true
    private var isLoading = false

    private val _uploadResult = MutableLiveData<Result<UploadResponse>>()
    val uploadResult: LiveData<Result<UploadResponse>> get() = _uploadResult

    fun reset() {
        currentCursor = null
        hasMoreData = true
        _media.value = emptyList()
    }

    fun loadMoreMediaEntries(
        userDid: String,
        spaceDid: String,
    ) {
        if (isLoading || !hasMoreData) return

        _loading.value = true
        isLoading = true

        viewModelScope.launch {
            try {
                val response =
                    apiService.listUploads(
                        userDid = userDid,
                        spaceDid = spaceDid,
                        cursor = currentCursor,
                        size = 20, // adjust page size as needed
                    )

                val newEntries = response.uploads
                val currentEntries = _media.value ?: emptyList()
                _media.value = currentEntries + newEntries

                currentCursor = response.cursor
                hasMoreData = response.hasMore
            } catch (e: Exception) {
                // optionally handle error
            } finally {
                _loading.value = false
                isLoading = false
            }
        }
    }

    fun uploadFile(
        userDid: String,
        spaceDid: String,
        carData: ByteArray,
    ) {
        viewModelScope.launch {
            try {
                val authHeaders = bridgeUploader.fetchBridgeTokens(userDid, spaceDid)
                val response = bridgeUploader.uploadCarFile(carData, authHeaders)
                val uploadResponse =
                    UploadResponse(
                        success = true,
                        cid = response.optString("cid", ""),
                        size = carData.size.toLong(),
                    )
                _uploadResult.value = Result.success(uploadResponse)
            } catch (e: Exception) {
                _uploadResult.value = Result.failure(e)
            }
        }
    }
}
