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
import net.opendasharchive.openarchive.core.domain.Archive
import net.opendasharchive.openarchive.core.domain.Evidence
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.core.repositories.ProjectRepository
import net.opendasharchive.openarchive.core.repositories.MediaRepository
import net.opendasharchive.openarchive.features.main.ui.AppRoute
import net.opendasharchive.openarchive.features.main.ui.Navigator
import org.koin.compose.koinInject
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
    navigator: Navigator? = null,
    useCustomCamera: Boolean = true,
    projectProvider: () -> Archive?,
    onError: (String) -> Unit,
    onMediaImported: (List<Evidence>) -> Unit,
): ContentPickerLaunchers {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val projectRepository: ProjectRepository = koinInject()
    val mediaRepository: MediaRepository = koinInject()

    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Keep last camera URI so we can import it
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    // Debouncing mechanism to prevent multiple rapid picker launches
    var lastPickerLaunchTime by remember { mutableStateOf(0L) }
    val debounceMs = 1000L

    // ---- Gallery picker ----
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult

        scope.launch {
            isProcessing = true
            errorMessage = null
            try {
                val archive = projectProvider() ?: run {
                    onError("Project provider returned null")
                    return@launch
                }
                val submission = projectRepository.getActiveSubmission(archive.id)
                val evidenceList = withContext(Dispatchers.IO) {
                    MediaPicker.import(context, archive, submission.id, uris, generateProof = Prefs.useProofMode)
                }
                evidenceList.forEach { evidence ->
                    mediaRepository.addEvidence(evidence)
                }
                onMediaImported(evidenceList)
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
                val archive = projectProvider() ?: run {
                    onError("Project provider returned null")
                    return@launch
                }
                val submission = projectRepository.getActiveSubmission(archive.id)
                val evidenceList = withContext(Dispatchers.IO) {
                    // single-URI import
                    MediaPicker.import(context, archive, submission.id, uri, generateProof = false)
                        ?.let { listOf(it) }
                        ?: emptyList()
                }
                evidenceList.forEach { evidence ->
                    mediaRepository.addEvidence(evidence)
                }
                onMediaImported(evidenceList)
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
                val archive = projectProvider() ?: run {
                    onError("Project provider returned null")
                    return@launch
                }
                val submission = projectRepository.getActiveSubmission(archive.id)
                val evidenceList = withContext(Dispatchers.IO) {
                    // For in-app capture we pass generateProof = true (same semantics as Picker.register)
                    MediaPicker.import(context, archive, submission.id, finalUri, generateProof = true)
                        ?.let { listOf(it) }
                        ?: emptyList()
                }
                evidenceList.forEach { evidence ->
                    mediaRepository.addEvidence(evidence)
                }
                onMediaImported(evidenceList)
            } catch (e: Exception) {
                errorMessage = "Camera file processing failed: ${e.localizedMessage}"
            } finally {
                isProcessing = false
            }
        }
    }

    // Custom camera launcher is no longer needed here as it's handled via navigation

    // ---- Launchers exposed to caller ----
    val launchGallery: () -> Unit = {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastPickerLaunchTime >= debounceMs) {
            lastPickerLaunchTime = currentTime
            errorMessage = null
            galleryLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
            )
        }
    }

    val launchFilePicker: () -> Unit = {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastPickerLaunchTime >= debounceMs) {
            lastPickerLaunchTime = currentTime
            errorMessage = null
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
    }

    val launchCamera: () -> Unit = launchCamera@{
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastPickerLaunchTime < debounceMs) return@launchCamera
        lastPickerLaunchTime = currentTime

        errorMessage = null
        if (useCustomCamera && navigator != null) {
            val archive = projectProvider()
            if (archive != null) {
                val cameraConfig = CameraConfig(
                    allowVideoCapture = true,
                    allowPhotoCapture = true,
                    allowMultipleCapture = false,
                    enablePreview = true,
                    showFlashToggle = true,
                    showGridToggle = true,
                    showCameraSwitch = true
                )
                navigator.navigateTo(AppRoute.CameraRoute(projectId = archive.id, config = cameraConfig))
            } else {
                errorMessage = "No folder selected"
            }
        } else {
            // TODO: This is a temporary persistent storage solution.
            // Review this when implementing the new Evidence architecture.
            val file: File? = Utility.getOutputMediaFile(
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