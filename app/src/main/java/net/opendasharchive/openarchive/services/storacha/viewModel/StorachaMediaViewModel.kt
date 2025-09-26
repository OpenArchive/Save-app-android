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
import net.opendasharchive.openarchive.services.storacha.util.CarFileResult
import java.io.File

enum class LoadingState {
    NONE, LOADING_FILES, LOADING_MORE, UPLOADING
}

class StorachaMediaViewModel(
    private val apiService: StorachaApiService,
    private val bridgeUploader: BridgeUploader = BridgeUploader(),
) : ViewModel() {
    private val _media = MutableLiveData<List<UploadEntry>>(emptyList())
    val media: LiveData<List<UploadEntry>> get() = _media

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    private val _loadingState = MutableLiveData<LoadingState>(LoadingState.NONE)
    val loadingState: LiveData<LoadingState> get() = _loadingState

    private val _isEmpty = MutableLiveData<Boolean>()
    val isEmpty: LiveData<Boolean> get() = _isEmpty

    private var currentCursor: String? = null
    private var hasMoreData: Boolean = true
    private var isLoading = false
    private var isFirstLoad = true

    private val _uploadResult = MutableLiveData<Result<UploadResponse>>()
    val uploadResult: LiveData<Result<UploadResponse>> get() = _uploadResult

    fun reset() {
        currentCursor = null
        hasMoreData = true
        isFirstLoad = true
        _media.value = emptyList()
        _isEmpty.value = false
    }

    fun refreshFromStart() {
        // Reset pagination without clearing current data
        currentCursor = null
        hasMoreData = true
        isFirstLoad = true
        // Don't clear _media.value to avoid flickering
    }

    fun loadMoreMediaEntries(
        userDid: String,
        spaceDid: String,
        sessionId: String?,
    ) {
        if (isLoading || !hasMoreData) return

        _loading.value = true
        isLoading = true

        // Set appropriate loading state
        _loadingState.value = if (isFirstLoad) LoadingState.LOADING_FILES else LoadingState.LOADING_MORE

        viewModelScope.launch {
            try {
                val response =
                    apiService.listUploads(
                        userDid = userDid,
                        spaceDid = spaceDid,
                        cursor = currentCursor,
                        size = 20, // adjust page size as needed
                        sessionId = if (sessionId.isNullOrEmpty()) null else sessionId,
                    )

                val newEntries = response.uploads
                val currentEntries = _media.value ?: emptyList()

                val updatedEntries = if (isFirstLoad && currentCursor == null) {
                    // For refresh/first load, replace existing data
                    newEntries
                } else {
                    // For pagination, prevent duplicates and append
                    val filteredNewEntries = newEntries.filter { newEntry ->
                        currentEntries.none { existingEntry -> existingEntry.cid == newEntry.cid }
                    }
                    currentEntries + filteredNewEntries
                }
                _media.value = updatedEntries

                // Update empty state - only consider empty if first load and no entries
                if (isFirstLoad) {
                    _isEmpty.value = updatedEntries.isEmpty()
                    isFirstLoad = false
                }

                currentCursor = response.cursor
                hasMoreData = response.hasMore
            } catch (e: Exception) {
                // optionally handle error
            } finally {
                _loading.value = false
                isLoading = false
                _loadingState.value = LoadingState.NONE
            }
        }
    }

    fun uploadFile(
        file: File,
        carResult: CarFileResult,
        userDid: String,
        spaceDid: String,
        sessionId: String?,
    ) {
        viewModelScope.launch {
            _loading.value = true
            _loadingState.value = LoadingState.UPLOADING
            try {
                // Use the complete bridge workflow with CAR files
                val bridgeResult =
                    bridgeUploader.uploadFile(
                        carData = carResult.carData,
                        carCid = carResult.carCid,
                        rootCid = carResult.rootCid,
                        spaceDid = spaceDid,
                        userDid = userDid,
                        sessionId = sessionId,
                    )

                val uploadResponse =
                    UploadResponse(
                        success = true,
                        cid = bridgeResult.rootCid,
                        size = bridgeResult.size,
                    )
                _uploadResult.value = Result.success(uploadResponse)

                // Refresh the media list after successful upload
                refreshFromStart()
                loadMoreMediaEntries(userDid, spaceDid, sessionId)
            } catch (e: Exception) {
                _uploadResult.value = Result.failure(e)
            } finally {
                _loading.value = false
                _loadingState.value = LoadingState.NONE
            }
        }
    }
}
