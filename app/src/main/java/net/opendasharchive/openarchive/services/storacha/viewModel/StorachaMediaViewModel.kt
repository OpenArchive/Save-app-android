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
import retrofit2.HttpException
import timber.log.Timber
import java.io.File

enum class LoadingState {
    NONE, LOADING_FILES, LOADING_MORE, UPLOADING
}

class StorachaMediaViewModel(
    private val apiService: StorachaApiService,
    private val bridgeUploader: BridgeUploader,
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
    private var isRefreshing = false

    private val _uploadResult = MutableLiveData<Result<UploadResponse>?>()
    val uploadResult: LiveData<Result<UploadResponse>?> get() = _uploadResult

    private val _sessionExpired = MutableLiveData<Boolean>()
    val sessionExpired: LiveData<Boolean> get() = _sessionExpired

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
        isRefreshing = true
        // Don't clear _media.value to avoid flickering
    }

    fun clearUploadResult() {
        _uploadResult.value = null
    }

    fun loadMoreMediaEntries(
        userDid: String,
        spaceDid: String,
        sessionId: String?,
    ) {
        if (isLoading || !hasMoreData) {
            Timber.d("loadMoreMediaEntries blocked: isLoading=$isLoading, hasMoreData=$hasMoreData")
            return
        }

        _loading.value = true
        isLoading = true

        // Set appropriate loading state
        _loadingState.value = if (isFirstLoad) LoadingState.LOADING_FILES else LoadingState.LOADING_MORE

        viewModelScope.launch {
            try {
                Timber.d("Making API call with cursor: $currentCursor")
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

                Timber.d("API Response: newEntries=${newEntries.size}, currentEntries=${currentEntries.size}, cursor=${response.cursor}, hasMore=${response.hasMore}")

                val updatedEntries = if (isRefreshing || (isFirstLoad && currentEntries.isEmpty())) {
                    // For refresh or genuine first load, replace existing data
                    Timber.d("Replacing data (refresh or first load)")
                    newEntries
                } else {
                    // For pagination, prevent duplicates and append
                    val filteredNewEntries = newEntries.filter { newEntry ->
                        val isDuplicate = currentEntries.any { existingEntry -> existingEntry.cid == newEntry.cid }
                        if (isDuplicate) {
                            Timber.d("Duplicate entry found: ${newEntry.cid}")
                        }
                        !isDuplicate
                    }
                    Timber.d("Appending data: newEntries=${newEntries.size}, filtered=${filteredNewEntries.size}")
                    if (filteredNewEntries.isEmpty() && newEntries.isNotEmpty()) {
                        Timber.w("All ${newEntries.size} entries were duplicates!")
                    }
                    currentEntries + filteredNewEntries
                }
                _media.value = updatedEntries

                // Check if we got any new items for pagination analysis
                val addedCount = updatedEntries.size - currentEntries.size

                // Update empty state - only consider empty if first load and no entries
                if (isFirstLoad) {
                    _isEmpty.value = updatedEntries.isEmpty()
                    isFirstLoad = false
                }

                // Reset refresh flag after completion
                isRefreshing = false

                currentCursor = response.cursor
                hasMoreData = response.hasMore
                Timber.d("Updated pagination: cursor=$currentCursor, hasMoreData=$hasMoreData")

                // If no new items were added but API says there's more, log a warning
                if (addedCount == 0 && newEntries.isNotEmpty() && hasMoreData) {
                    Timber.w("Potential pagination issue: no new items added but hasMore=true")
                }
            } catch (e: HttpException) {
                if (e.code() == 401) {
                    _sessionExpired.value = true
                }
                isRefreshing = false
            } catch (e: Exception) {
                // optionally handle error
                isRefreshing = false // Reset refresh flag on error too
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
        isAdmin: Boolean = false,
    ) {
        viewModelScope.launch {
            _loading.value = true
            _loadingState.value = LoadingState.UPLOADING
            try {
                // Use the complete bridge workflow with CAR files
                val bridgeResult =
                    bridgeUploader.uploadFile(
                        carFile = carResult.carFile,
                        carCid = carResult.carCid,
                        rootCid = carResult.rootCid,
                        spaceDid = spaceDid,
                        userDid = userDid,
                        sessionId = sessionId,
                        isAdmin = isAdmin,
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
            } catch (e: HttpException) {
                if (e.code() == 401) {
                    _sessionExpired.value = true
                }
                _uploadResult.value = Result.failure(e)
            } catch (e: Exception) {
                _uploadResult.value = Result.failure(e)
            } finally {
                // Clean up temporary CAR file
                try {
                    if (carResult.carFile.exists()) {
                        carResult.carFile.delete()
                        Timber.d("Deleted temporary CAR file: ${carResult.carFile.name}")
                    }
                } catch (e: Exception) {
                    Timber.e("Failed to delete CAR file: ${e.message}")
                }

                _loading.value = false
                _loadingState.value = LoadingState.NONE
            }
        }
    }
}
