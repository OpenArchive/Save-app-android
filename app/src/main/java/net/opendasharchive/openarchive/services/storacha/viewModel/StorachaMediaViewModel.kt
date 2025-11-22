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
    private var pendingRefresh: Triple<String, String, String?>? = null

    // Callback for UI to check if more loading is needed
    var onLoadComplete: (() -> Unit)? = null

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
        if (isLoading && isRefreshing) {
            pendingRefresh = Triple(userDid, spaceDid, sessionId)
            return
        }

        if (isLoading || !hasMoreData) return

        _loading.value = true
        isLoading = true
        _loadingState.value = if (isFirstLoad) LoadingState.LOADING_FILES else LoadingState.LOADING_MORE

        viewModelScope.launch {
            try {
                val response = apiService.listUploads(
                    userDid = userDid,
                    spaceDid = spaceDid,
                    cursor = currentCursor,
                    size = 20,
                    sessionId = sessionId?.takeIf { it.isNotEmpty() },
                )

                val newEntries = response.uploads
                val currentEntries = _media.value ?: emptyList()

                val updatedEntries = if (isRefreshing || (isFirstLoad && currentEntries.isEmpty())) {
                    newEntries
                } else {
                    val existingCids = currentEntries.map { it.cid }.toSet()
                    currentEntries + newEntries.filterNot { existingCids.contains(it.cid) }
                }
                _media.value = updatedEntries

                val addedCount = updatedEntries.size - currentEntries.size

                if (isFirstLoad) {
                    _isEmpty.value = updatedEntries.isEmpty()
                    isFirstLoad = false
                }

                isRefreshing = false
                currentCursor = response.cursor
                hasMoreData = response.hasMore && addedCount > 0
            } catch (e: HttpException) {
                if (e.code() == 401) {
                    _sessionExpired.value = true
                }
                isRefreshing = false
            } catch (e: Exception) {
                isRefreshing = false
            } finally {
                _loading.value = false
                isLoading = false
                _loadingState.value = LoadingState.NONE

                pendingRefresh?.let { (pUserDid, pSpaceDid, pSessionId) ->
                    pendingRefresh = null
                    refreshFromStart()
                    loadMoreMediaEntries(pUserDid, pSpaceDid, pSessionId)
                } ?: run {
                    if (hasMoreData) {
                        onLoadComplete?.invoke()
                    }
                }
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
