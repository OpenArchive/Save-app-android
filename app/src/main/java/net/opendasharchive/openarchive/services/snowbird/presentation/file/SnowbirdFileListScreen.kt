package net.opendasharchive.openarchive.services.snowbird.presentation.file

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.domain.Evidence
import net.opendasharchive.openarchive.core.presentation.theme.DefaultScaffoldPreview
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.core.presentation.theme.SaveTextStyles
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.dialog.DialogStateManager
import net.opendasharchive.openarchive.features.core.dialog.DialogType
import net.opendasharchive.openarchive.features.core.dialog.showDialog
import net.opendasharchive.openarchive.features.media.AddMediaType
import net.opendasharchive.openarchive.features.media.ContentPickerSheet
import net.opendasharchive.openarchive.core.navigation.NavigationResultKeys
import net.opendasharchive.openarchive.core.navigation.ResultEffect
import net.opendasharchive.openarchive.features.main.ui.CameraCaptureResult
import net.opendasharchive.openarchive.core.config.AppConfig
import net.opendasharchive.openarchive.util.Utility
import org.koin.compose.koinInject
import java.io.File

@Composable
fun SnowbirdFileListScreen(
    viewModel: SnowbirdFileViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as Activity
    val dialogManager: DialogStateManager = koinInject()
    val appConfig: AppConfig = koinInject()

    // 1. Camera Launcher (Photo only for now as per system contract)
    var currentPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            currentPhotoUri?.let { viewModel.onAction(SnowbirdFileAction.UploadFile(it)) }
        }
    }

    // Receive camera capture results from CameraScreen via ResultEventBus
    ResultEffect<CameraCaptureResult>(resultKey = NavigationResultKeys.SNOWBIRD_CAMERA_RESULT) { result ->
        if (result.projectId == state.archiveId) {
            result.capturedUris.forEach { uri ->
                viewModel.onAction(SnowbirdFileAction.UploadFile(uri))
            }
        }
    }

    // 2. Gallery Launcher (Multiple selection - 10)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris ->
        uris.forEach { viewModel.onAction(SnowbirdFileAction.UploadFile(it)) }
    }

    // 3. File Launcher (Open multiple documents with filtering)
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val contentResolver = context.contentResolver
            val filteredUris = uris.filter { uri ->
                val mimeType = contentResolver.getType(uri)
                mimeType?.startsWith("image/") == true ||
                        mimeType?.startsWith("video/") == true ||
                        mimeType?.startsWith("audio/") == true
            }

            if (filteredUris.isEmpty()) {
                dialogManager.showDialog(dialogManager.requireResourceProvider()) {
                    type = DialogType.Warning
                    title = UiText.Dynamic("Invalid File Type")
                    message = UiText.Dynamic("Please select only image, video, or audio files.")
                    positiveButton { text = UiText.Resource(R.string.lbl_ok) }
                }
            } else {
                if (filteredUris.size < uris.size) {
                    Toast.makeText(
                        context,
                        "Some files were skipped (unsupported types)",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                filteredUris.forEach { viewModel.onAction(SnowbirdFileAction.UploadFile(it)) }
            }
        }
    }

    // Handle Side Effects
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is SnowbirdFileEvent.FileDownloaded -> {
                    dialogManager.showDialog(dialogManager.requireResourceProvider()) {
                        type = DialogType.Success
                        title = UiText.Resource(R.string.label_success_title)
                        message = UiText.Dynamic("File successfully downloaded")
                        positiveButton {
                            text = UiText.Dynamic("Open")
                            action = { openFile(context, event.uri, dialogManager) }
                        }
                        neutralButton {
                            text = UiText.Resource(R.string.lbl_ok)
                        }
                    }
                }
                is SnowbirdFileEvent.LaunchPicker -> {
                    when (event.type) {
                        AddMediaType.CAMERA -> {
                            if (appConfig.useCustomCamera) {
                                viewModel.onAction(SnowbirdFileAction.NavigateToCamera)
                            } else {
                                val photoFile = Utility.getOutputMediaFile(context, "camera_${System.currentTimeMillis()}.jpg")
                                if (photoFile != null) {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.provider",
                                        photoFile
                                    )
                                    currentPhotoUri = uri
                                    cameraLauncher.launch(uri)
                                } else {
                                    Toast.makeText(context, "Failed to prepare camera", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        AddMediaType.GALLERY -> {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        }
                        AddMediaType.FILES -> {
                            fileLauncher.launch(arrayOf("image/*", "video/*", "audio/*"))
                        }
                    }
                }
            }
        }
    }

    // Screen Body
    Box(modifier = Modifier.fillMaxSize()) {
        SnowbirdFileListContent(
            state = state,
            onAction = { snowbirdAction ->
                if (snowbirdAction is SnowbirdFileAction.DownloadFile) {
                    dialogManager.showDialog(dialogManager.requireResourceProvider()) {
                        type = DialogType.Warning
                        title = UiText.Dynamic("Download Media?")
                        message = UiText.Dynamic("Are you sure you want to download this media?")
                        positiveButton {
                            text = UiText.Dynamic("Yes")
                            action = { viewModel.onAction(snowbirdAction) }
                        }
                        neutralButton {
                            text = UiText.Dynamic("No")
                        }
                    }
                } else {
                    viewModel.onAction(snowbirdAction)
                }
            }
        )

        if (state.showContentPicker) {
            ContentPickerSheet(
                onDismiss = { viewModel.onAction(SnowbirdFileAction.ContentPickerDismissed) },
                onMediaTypeSelected = { type -> viewModel.onAction(SnowbirdFileAction.OnMediaTypeSelected(type)) },
            )
        }
    }
}

private fun openFile(context: Context, uri: Uri, dialogManager: DialogStateManager) {
    try {
        val filename = uri.lastPathSegment ?: "file"
        val extension = filename.substringAfterLast(".", "")
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase()) ?: "*/*"

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            val chooser = Intent.createChooser(intent, "Open file with")
            if (chooser.resolveActivity(context.packageManager) != null) {
                context.startActivity(chooser)
            } else {
                dialogManager.showDialog(dialogManager.requireResourceProvider()) {
                    type = DialogType.Warning
                    title = UiText.Dynamic("No App Found")
                    message = UiText.Dynamic("No app is available to open this type of file.")
                    positiveButton { text = UiText.Resource(R.string.lbl_ok) }
                }
            }
        }
    } catch (e: Exception) {
        dialogManager.showDialog(dialogManager.requireResourceProvider()) {
            type = DialogType.Error
            title = UiText.Dynamic("Error")
            message = UiText.Dynamic("Could not open file: ${e.message}")
            positiveButton { text = UiText.Resource(R.string.lbl_ok) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnowbirdFileListContent(
    state: SnowbirdFileState,
    onAction: (SnowbirdFileAction) -> Unit
) {
    PullToRefreshBox(
        isRefreshing = state.isLoading,
        onRefresh = { onAction(SnowbirdFileAction.RefreshFiles) },
        modifier = Modifier.fillMaxSize()
    ) {
        if (state.files.isEmpty() && !state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "No files found")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.files) { evidence ->
                    SnowbirdFileItem(
                        evidence = evidence,
                        onClick = { onAction(SnowbirdFileAction.DownloadFile(evidence)) }
                    )
                }
            }
        }
    }
}

@Composable
fun SnowbirdFileItem(
    evidence: Evidence,
    onClick: () -> Unit
) {
    val fileExtension = evidence.title.substringAfterLast(".", "").lowercase()
    val iconRes = when {
        isImageFile(fileExtension) -> R.drawable.ic_image
        isVideoFile(fileExtension) -> R.drawable.ic_videocam
        isAudioFile(fileExtension) -> R.drawable.ic_music
        else -> R.drawable.ic_description
    }

    Card(
        modifier = Modifier
            .height(140.dp)
            .fillMaxWidth()
            .padding(4.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Box(
                    modifier = Modifier.size(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    if (!evidence.isDownloaded) {
                        Icon(
                            painter = painterResource(id = R.drawable.outline_cloud_download_24),
                            contentDescription = null,
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.BottomEnd)
                                .padding(2.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = evidence.title,
                    style = SaveTextStyles.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    lineHeight = 12.sp
                )
            }
        }
    }
}

private fun isImageFile(extension: String) = extension in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "heif")
private fun isVideoFile(extension: String) = extension in listOf("mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "3gp")
private fun isAudioFile(extension: String) = extension in listOf("mp3", "wav", "ogg", "m4a", "flac", "aac", "wma")

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SnowbirdFileListScreenPreview() {
    SaveAppTheme {
        SnowbirdFileListContent(
            state = SnowbirdFileState(
                files = listOf(
                    Evidence(title = "photo1.jpg", isDownloaded = true),
                    Evidence(title = "video1.mp4", isDownloaded = false),
                    Evidence(title = "audio1.mp3", isDownloaded = true),
                    Evidence(title = "doc1.pdf", isDownloaded = false),
                    Evidence(title = "photo2.png", isDownloaded = true)
                )
            ),
            onAction = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SnowbirdFileListScreenEmptyPreview() {
    DefaultScaffoldPreview {
        SnowbirdFileListContent(
            state = SnowbirdFileState(files = emptyList()),
            onAction = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SnowbirdFileListScreenLoadingPreview() {
    DefaultScaffoldPreview {
        SnowbirdFileListContent(
            state = SnowbirdFileState(files = emptyList(), isLoading = true),
            onAction = {}
        )
    }
}
