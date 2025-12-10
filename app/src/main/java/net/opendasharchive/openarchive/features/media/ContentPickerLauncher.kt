package net.opendasharchive.openarchive.features.media

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.features.media.camera.CameraActivity
import net.opendasharchive.openarchive.features.media.camera.CameraConfig
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.Utility
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

data class ContentPickerLaunchers(
    val launch: (AddMediaType) -> Unit,
    val isProcessing: Boolean,
    val errorMessage: String?
)

@Composable
fun rememberContentPickerLaunchers(
    useCustomCamera: Boolean = true,
    projectProvider: () -> Project?,
    onMediaImported: (List<Media>) -> Unit,
): ContentPickerLaunchers {

    val context = LocalContext.current
    val activity = context as Activity
    val scope = rememberCoroutineScope()

    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Keep last camera URI so we can import it
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    // ---- Gallery picker ----
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult

        scope.launch {
            isProcessing = true
            errorMessage = null
            try {
                val project = projectProvider() ?: return@launch
                val mediaList = withContext(Dispatchers.IO) {
                    Picker.import(context, project, uris, generateProof = Prefs.useProofMode)
                }
                onMediaImported(mediaList)
            } catch (e: CancellationException) {
                // ignore
                AppLogger.i("ContentPickerLauncher: Gallery import cancelled", e)
            } catch (e: Exception) {
                errorMessage = "Failed to copy: ${e.localizedMessage}"
            } finally {
                isProcessing = false
            }
        }
    }

    // ---- File picker ----
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        scope.launch {
            isProcessing = true
            errorMessage = null
            try {
                val project = projectProvider() ?: return@launch
                val media = withContext(Dispatchers.IO) {
                    // single-URI import
                    Picker.import(context, project, uri, generateProof = false)
                        ?.let { listOf(it) }
                        ?: emptyList()
                }
                onMediaImported(media)
            } catch (e: Exception) {
                errorMessage = "Failed to copy: ${e.localizedMessage}"
            } finally {
                isProcessing = false
            }
        }
    }

    // ---- Camera (modern TakePicture) ----
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val finalUri = cameraUri
        if (!success || finalUri == null) return@rememberLauncherForActivityResult

        scope.launch {
            isProcessing = true
            errorMessage = null
            try {
                val project = projectProvider() ?: return@launch
                val media = withContext(Dispatchers.IO) {
                    // For in-app capture we pass generateProof = true (same semantics as Picker.register)
                    Picker.import(context, project, finalUri, generateProof = true)
                        ?.let { listOf(it) }
                        ?: emptyList()
                }
                onMediaImported(media)
            } catch (e: Exception) {
                errorMessage = "Camera file processing failed: ${e.localizedMessage}"
            } finally {
                isProcessing = false
            }
        }
    }

    // ---------- CUSTOM CAMERA (CameraActivity) ----------
    val customCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult

        val capturedUris = result.data?.getStringArrayListExtra(CameraActivity.EXTRA_CAPTURED_URIS)
        if (capturedUris.isNullOrEmpty()) return@rememberLauncherForActivityResult

        val uris = capturedUris.map(Uri::parse)

        scope.launch {
            isProcessing = true
            errorMessage = null
            try {
                val project = projectProvider() ?: return@launch
                val media = withContext(Dispatchers.IO) {
                    // Camera capture → generateProof = true (same as Picker.register custom camera)
                    Picker.import(context, project, uris, generateProof = true)
                }
                onMediaImported(media)
            } catch (e: Exception) {
                errorMessage = "Failed to import from camera: ${e.localizedMessage}"
            } finally {
                isProcessing = false
            }
        }
    }

    // ---- Launchers exposed to caller ----
    val launchGallery: () -> Unit = {
        errorMessage = null
        // Equivalent to Picker.pickMedia, but via Compose launcher
        galleryLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
        )
    }

    val launchFilePicker: () -> Unit = {
        errorMessage = null
        // Equivalent MIME setup to Picker.mFilePickerIntent
        filePickerLauncher.launch(
            arrayOf(
                "application/pdf",
                "image/*",
                "video/*",
                "audio/mpeg",
                "audio/mp3"
            )
        )
    }

    val launchCamera: () -> Unit = launchCamera@{
        errorMessage = null
        if (useCustomCamera) {
            val cameraConfig = CameraConfig(
                allowVideoCapture = true,
                allowPhotoCapture = true,
                allowMultipleCapture = false,
                enablePreview = true,
                showFlashToggle = true,
                showGridToggle = true,
                showCameraSwitch = true
            )
            Picker.launchCustomCamera(
                activity = activity,
                launcher = customCameraLauncher,
                config = cameraConfig
            )
        } else {
            val file: File? = Utility.getOutputMediaFileByCache(
                context,
                "IMG_${System.currentTimeMillis()}.jpg"
            )
            if (file == null) {
                errorMessage = "Failed to prepare camera file"
                return@launchCamera
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            cameraUri = uri
            cameraLauncher.launch(uri)
        }
    }

    val launch: (AddMediaType) -> Unit = { type ->
        when (type) {
            AddMediaType.GALLERY -> launchGallery()
            AddMediaType.FILES -> launchFilePicker()
            AddMediaType.CAMERA -> launchCamera()
        }
    }

    return ContentPickerLaunchers(
        launch = launch,
        isProcessing = isProcessing,
        errorMessage = errorMessage
    )
}