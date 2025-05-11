package net.opendasharchive.openarchive.services.snowbird

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.db.FileUploadResult
import net.opendasharchive.openarchive.db.SnowbirdError
import net.opendasharchive.openarchive.db.SnowbirdFileItem
import net.opendasharchive.openarchive.util.BaseViewModel
import net.opendasharchive.openarchive.util.trackProcessingWithTimeout
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

class SnowbirdFileViewModel(
    private val application: Application,
    private val repository: ISnowbirdFileRepository
) : BaseViewModel(application) {

    sealed class State {
        data object Idle : State()
        data object Loading : State()
        data class DownloadSuccess(val uri: Uri) : State()
        data class FetchSuccess(val files: List<SnowbirdFileItem>, var isRefresh: Boolean) : State()
        data class UploadSuccess(val result: FileUploadResult) : State()
        data class Error(val error: SnowbirdError) : State()
    }

    private val _mediaState = MutableStateFlow<State>(State.Idle)
    val mediaState: StateFlow<State> = _mediaState.asStateFlow()

    fun downloadFile(groupKey: String, repoKey: String, filename: String) {
        viewModelScope.launch {
            _mediaState.value = State.Loading
            try {
                val result = processingTracker.trackProcessingWithTimeout(30_000, "download_file") {
                    repository.downloadFile(groupKey, repoKey, filename)
                }

                _mediaState.value = when (result) {
                    is SnowbirdResult.Success -> onDownload(result.value, filename)
                    is SnowbirdResult.Error -> State.Error(result.error)
                }
            } catch (e: TimeoutCancellationException) {
                _mediaState.value = State.Error(SnowbirdError.TimedOut)
            }
        }
    }

    fun fetchFiles(groupKey: String, repoKey: String, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _mediaState.value = State.Loading
            try {
                val result = processingTracker.trackProcessingWithTimeout(60_000, "fetch_files") {
                    repository.fetchFiles(groupKey, repoKey, forceRefresh)
                }

                _mediaState.value = when (result) {
                    is SnowbirdResult.Success -> State.FetchSuccess(result.value, forceRefresh)
                    is SnowbirdResult.Error -> State.Error(result.error)
                }
            } catch (e: TimeoutCancellationException) {
                _mediaState.value = State.Error(SnowbirdError.TimedOut)
            }
        }
    }

    // Example reponse:
    //    {
    //        "updated_collection_hash": "7dkgeko3oeyyr5xympsg2mhbicb2k2ba4wqen6lpt6qs7mgza7vq"
    //    }
    //
    fun uploadFile(groupKey: String, repoKey: String, uri: Uri) {
        viewModelScope.launch {
            _mediaState.value = State.Loading
            try {
                val result = processingTracker.trackProcessingWithTimeout(30_000, "upload_file") {
                    repository.uploadFile(groupKey, repoKey, uri)
                }

                _mediaState.value = when (result) {
                    is SnowbirdResult.Success -> State.UploadSuccess(result.value)
                    is SnowbirdResult.Error -> State.Error(result.error)
                }
            } catch (e: TimeoutCancellationException) {
                _mediaState.value = State.Error(SnowbirdError.TimedOut)
            }
        }
    }

    private suspend fun onDownload(bytes: ByteArray, filename: String): State {
        Timber.d("Downloaded ${bytes.size} bytes")

        val internalUri = saveByteArrayToFile(application.applicationContext, bytes, filename)
            .getOrElse { throw it }

        val galleryUri = runCatching {
            saveImageToGallery(application.applicationContext, bytes, filename)
        }.getOrNull()

        return if (galleryUri != null) {
            State.DownloadSuccess(internalUri).also {
                Timber.d("Saved to gallery: $galleryUri")
            }
        } else {
            // if gallery write failed, treat as download success anyway
            State.DownloadSuccess(internalUri)
        }

//        return saveByteArrayToFile(application.applicationContext, bytes, filename).fold(
//            onSuccess = { uri -> State.DownloadSuccess(uri) },
//            onFailure = { error -> State.Error(SnowbirdError.GeneralError("Error saving file: ${error.message}")) }
//        )
    }

    private suspend fun saveByteArrayToFile(context: Context, byteArray: ByteArray, filename: String): Result<Uri> =
        withContext(Dispatchers.IO) {
            runCatching {
                val directory = File(context.filesDir, "files").apply { mkdirs() }
                val file = File(directory, filename)

                file.outputStream().use { it.write(byteArray) }

                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    file
                )
            }
        }

    suspend fun saveImageToGallery(
        context: Context,
        imageBytes: ByteArray,
        displayName: String  // e.g. "photo1.jpg"
    ): Uri? = withContext(Dispatchers.IO) {
        // 1) for Q+ use MediaStore:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                // put it in Pictures/YourApp (no extra permission)
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/YourApp")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return@withContext null

            resolver.openOutputStream(uri)?.use { it.write(imageBytes) }
            // release the "pending" flag so it shows up
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)

            return@withContext uri
        }

        // 2) for Pre-Q: still possible, but you need WRITE_EXTERNAL_STORAGE
        val imagesDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "YourApp"
        ).apply { if (!exists()) mkdirs() }

        val file = File(imagesDir, displayName)
        FileOutputStream(file).use { it.write(imageBytes) }

        // tell MediaStore about it
        MediaScannerConnection.scanFile(
            context,
            arrayOf(file.absolutePath),
            arrayOf("image/jpeg"),
            null
        )
        return@withContext Uri.fromFile(file)
    }
}