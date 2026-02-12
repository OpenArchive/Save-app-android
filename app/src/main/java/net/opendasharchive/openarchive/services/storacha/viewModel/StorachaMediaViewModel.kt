package net.opendasharchive.openarchive.services.storacha.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.services.storacha.model.UploadEntry
import net.opendasharchive.openarchive.services.storacha.model.UploadResponse
import net.opendasharchive.openarchive.services.storacha.service.StorachaApiService
import net.opendasharchive.openarchive.services.storacha.util.BridgeUploader
import retrofit2.HttpException
import timber.log.Timber
import java.io.File
import java.io.IOException

enum class LoadingState {
    NONE,
    LOADING_FILES,
    LOADING_MORE,
    UPLOADING,
}

class StorachaMediaViewModel(
    private val apiService: StorachaApiService,
    private val bridgeUploader: BridgeUploader,
) : ViewModel() {
    companion object {
        private const val PAGE_SIZE = 20
        private const val HTTP_UNAUTHORIZED = 401
    }

    private val _media = MutableLiveData<List<UploadEntry>>(emptyList())
    val media: LiveData<List<UploadEntry>> get() = _media

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    private val _loadingState = MutableLiveData(LoadingState.NONE)
    val loadingState: LiveData<LoadingState> get() = _loadingState

    private val _isEmpty = MutableLiveData<Boolean>()
    val isEmpty: LiveData<Boolean> get() = _isEmpty

    private val _uploadResult = MutableLiveData<Result<UploadResponse>?>()
    val uploadResult: LiveData<Result<UploadResponse>?> get() = _uploadResult

    private val _sessionExpired = MutableLiveData<Boolean>()
    val sessionExpired: LiveData<Boolean> get() = _sessionExpired

    private var paginationState = PaginationState()

    var onLoadComplete: (() -> Unit)? = null

    fun reset() {
        paginationState = PaginationState()
        _media.value = emptyList()
        _isEmpty.value = false
    }

    fun refreshFromStart() {
        paginationState =
            paginationState.copy(
                cursor = null,
                hasMoreData = true,
                isFirstLoad = true,
                isRefreshing = true,
            )
    }

    fun clearUploadResult() {
        _uploadResult.value = null
    }

    fun loadMoreMediaEntries(
        userDid: String,
        spaceDid: String,
        sessionId: String?,
    ) {
        if (paginationState.isLoading || !paginationState.hasMoreData) return

        _loading.value = true
        paginationState =
            paginationState.copy(
                isLoading = true,
                loadingState = if (paginationState.isFirstLoad) LoadingState.LOADING_FILES else LoadingState.LOADING_MORE,
            )
        _loadingState.value = paginationState.loadingState

        viewModelScope.launch {
            try {
                Timber.d("Loading page: cursor=${paginationState.cursor?.take(15)}...")

                val response =
                    apiService.listUploads(
                        userDid = userDid,
                        spaceDid = spaceDid,
                        cursor = paginationState.cursor,
                        size = PAGE_SIZE,
                        sessionId = sessionId?.takeIf { it.isNotEmpty() },
                    )

                handleLoadSuccess(response.uploads, response.hasMore)
            } catch (e: HttpException) {
                handleLoadError(e)
            } catch (e: Exception) {
                handleLoadError(e)
            } finally {
                _loading.value = false
                paginationState =
                    paginationState.copy(
                        isLoading = false,
                        loadingState = LoadingState.NONE,
                    )
                _loadingState.value = LoadingState.NONE

                if (paginationState.hasMoreData) {
                    onLoadComplete?.invoke()
                }
            }
        }
    }

    /**
     * Uploads a file to Storacha via the backend server.
     *
     * The backend handles CAR file generation and the complex blob workflow,
     * so we just need to send the original file and filename.
     *
     * @param file The file to upload
     * @param fileName The original filename (preserved in UnixFS directory structure)
     * @param userDid The user's DID
     * @param spaceDid The target space DID
     * @param sessionId The session ID (for admin users)
     * @param isAdmin Whether this is an admin upload
     */
    fun uploadFile(
        file: File,
        fileName: String,
        userDid: String,
        spaceDid: String,
        sessionId: String?,
        isAdmin: Boolean = false,
    ) {
        viewModelScope.launch {
            _loading.value = true
            _loadingState.value = LoadingState.UPLOADING
            try {
                // Upload with retry logic for transient failures
                val bridgeResult = uploadWithRetry(
                    file = file,
                    fileName = fileName,
                    spaceDid = spaceDid,
                    userDid = userDid,
                    sessionId = sessionId,
                    isAdmin = isAdmin,
                )

                _uploadResult.value =
                    Result.success(
                        UploadResponse(
                            success = true,
                            cid = bridgeResult.rootCid,
                            size = bridgeResult.size,
                        ),
                    )

                // Optimistically add the new upload to the list immediately
                // This gives instant feedback without waiting for server sync
                addOptimisticUpload(bridgeResult.rootCid, bridgeResult.size)

                // Clear loading state immediately - user sees the result
                _loading.value = false
                _loadingState.value = LoadingState.NONE

                // Sync with server in background to ensure consistency
                // This runs silently without affecting the UI
                syncWithServerInBackground(userDid, spaceDid, sessionId)

            } catch (e: HttpException) {
                if (e.code() == HTTP_UNAUTHORIZED) {
                    _sessionExpired.value = true
                }
                _uploadResult.value = Result.failure(e)
                _loading.value = false
                _loadingState.value = LoadingState.NONE
            } catch (e: Exception) {
                _uploadResult.value = Result.failure(e)
                _loading.value = false
                _loadingState.value = LoadingState.NONE
            }
        }
    }

    /**
     * Optimistically adds a newly uploaded item to the list.
     * This provides instant feedback without waiting for server sync.
     */
    private fun addOptimisticUpload(cid: String, size: Long) {
        val currentTime = java.time.Instant.now().toString()
        val newEntry = net.opendasharchive.openarchive.services.storacha.model.UploadEntry(
            cid = cid,
            size = size,
            created = currentTime,
            insertedAt = currentTime,
            updatedAt = currentTime,
            gatewayUrl = "https://$cid.ipfs.dweb.link/",
        )

        val currentList = _media.value ?: emptyList()
        // Add to the beginning of the list (most recent first)
        _media.value = listOf(newEntry) + currentList
        _isEmpty.value = false
    }

    /**
     * Silently syncs with the server in the background.
     * Updates the list if there are changes, but doesn't show loading indicators.
     */
    private fun syncWithServerInBackground(
        userDid: String,
        spaceDid: String,
        sessionId: String?,
    ) {
        viewModelScope.launch {
            try {
                val response = apiService.listUploads(
                    userDid = userDid,
                    spaceDid = spaceDid,
                    cursor = null,
                    size = PAGE_SIZE,
                    sessionId = sessionId?.takeIf { it.isNotEmpty() },
                )

                // Only update if we got data - merge with optimistic updates
                if (response.uploads.isNotEmpty()) {
                    val currentList = _media.value ?: emptyList()
                    val serverCids = response.uploads.map { it.cid }.toSet()

                    // Keep optimistic entries that aren't in server response yet,
                    // plus all server entries
                    val optimisticEntries = currentList.filterNot { it.cid in serverCids }
                    _media.value = optimisticEntries + response.uploads

                    paginationState = paginationState.copy(
                        cursor = response.uploads.lastOrNull()?.cid,
                        hasMoreData = response.hasMore,
                        isFirstLoad = false,
                        isRefreshing = false,
                    )
                }
            } catch (e: Exception) {
                // Silently ignore background sync errors - optimistic data is still shown
                Timber.d("Background sync failed (non-critical): ${e.message}")
            }
        }
    }

    /**
     * Upload with retry logic for transient network failures.
     */
    private suspend fun uploadWithRetry(
        file: File,
        fileName: String,
        spaceDid: String,
        userDid: String?,
        sessionId: String?,
        isAdmin: Boolean,
        maxRetries: Int = 3,
    ): net.opendasharchive.openarchive.services.storacha.model.BridgeUploadResult {
        var lastException: Exception? = null

        repeat(maxRetries) { attempt ->
            try {
                return bridgeUploader.uploadFile(
                    file = file,
                    fileName = fileName,
                    spaceDid = spaceDid,
                    userDid = userDid,
                    sessionId = sessionId,
                    isAdmin = isAdmin,
                )
            } catch (e: IOException) {
                // Retry on network errors
                lastException = e
                Timber.w("Upload attempt ${attempt + 1} failed with IO error: ${e.message}")
                if (attempt < maxRetries - 1) {
                    delay(1000L * (attempt + 1)) // Exponential backoff: 1s, 2s, 3s
                }
            } catch (e: HttpException) {
                // Retry on 5xx server errors, but not on 4xx client errors
                if (e.code() in 500..599) {
                    lastException = e
                    Timber.w("Upload attempt ${attempt + 1} failed with server error: ${e.code()}")
                    if (attempt < maxRetries - 1) {
                        delay(1000L * (attempt + 1))
                    }
                } else {
                    throw e // Don't retry client errors
                }
            }
        }

        throw lastException ?: Exception("Upload failed after $maxRetries attempts")
    }

    private fun handleLoadSuccess(
        newEntries: List<UploadEntry>,
        hasMore: Boolean,
    ) {
        val currentEntries = _media.value ?: emptyList()
        val isReplacingList =
            paginationState.isRefreshing || (paginationState.isFirstLoad && currentEntries.isEmpty())

        val updatedEntries =
            if (isReplacingList) {
                newEntries
            } else {
                val existingCids = currentEntries.map { it.cid }.toSet()
                currentEntries + newEntries.filterNot { existingCids.contains(it.cid) }
            }

        _media.value = updatedEntries

        val addedCount =
            if (isReplacingList) {
                newEntries.size
            } else {
                updatedEntries.size - currentEntries.size
            }

        Timber.d("Loaded: total=${updatedEntries.size}, added=$addedCount, hasMore=$hasMore")

        if (paginationState.isFirstLoad) {
            _isEmpty.value = updatedEntries.isEmpty()
        }

        // Use last item's CID as cursor for next page (workaround for server pagination bug)
        val nextCursor = newEntries.lastOrNull()?.cid
        val actualHasMore = newEntries.isNotEmpty() && addedCount > 0 && hasMore

        paginationState =
            paginationState.copy(
                cursor = nextCursor,
                hasMoreData = actualHasMore,
                isFirstLoad = false,
                isRefreshing = false,
            )
    }

    private fun handleLoadError(error: Exception) {
        if (error is HttpException && error.code() == HTTP_UNAUTHORIZED) {
            _sessionExpired.value = true
        }
        Timber.e(error, "Failed to load page")

        // Only set isEmpty if this was the first load AND we have no existing data
        // This prevents showing "no media" when a refresh fails but we have cached data
        if (paginationState.isFirstLoad && (_media.value.isNullOrEmpty())) {
            _isEmpty.value = true
        }

        paginationState =
            paginationState.copy(
                isRefreshing = false,
                isLoading = false,
            )
    }

    private data class PaginationState(
        val cursor: String? = null,
        val hasMoreData: Boolean = true,
        val isLoading: Boolean = false,
        val isFirstLoad: Boolean = true,
        val isRefreshing: Boolean = false,
        val loadingState: LoadingState = LoadingState.NONE,
    )
}
