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
import retrofit2.HttpException
import timber.log.Timber
import java.io.File

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
                val bridgeResult =
                    bridgeUploader.uploadFile(
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

                refreshFromStart()
                loadMoreMediaEntries(userDid, spaceDid, sessionId)
            } catch (e: HttpException) {
                if (e.code() == HTTP_UNAUTHORIZED) {
                    _sessionExpired.value = true
                }
                _uploadResult.value = Result.failure(e)
            } catch (e: Exception) {
                _uploadResult.value = Result.failure(e)
            } finally {
                _loading.value = false
                _loadingState.value = LoadingState.NONE
            }
        }
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
        paginationState =
            paginationState.copy(
                isRefreshing = false,
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
