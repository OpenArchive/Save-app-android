package net.opendasharchive.openarchive.services.snowbird

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentSnowbirdListMediaBinding
import net.opendasharchive.openarchive.db.FileUploadResult
import net.opendasharchive.openarchive.db.SnowbirdError
import net.opendasharchive.openarchive.db.SnowbirdFileItem
import net.opendasharchive.openarchive.extensions.androidViewModel
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.dialog.DialogType
import net.opendasharchive.openarchive.features.core.dialog.showDialog
import net.opendasharchive.openarchive.features.media.AddMediaType
import net.opendasharchive.openarchive.features.media.ContentPickerFragment
import net.opendasharchive.openarchive.features.media.Picker
import net.opendasharchive.openarchive.features.media.camera.CameraActivity
import net.opendasharchive.openarchive.features.media.camera.CameraConfig
import net.opendasharchive.openarchive.features.settings.passcode.AppConfig
import net.opendasharchive.openarchive.util.SpacingItemDecoration
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.io.File

class SnowbirdFileListFragment : BaseSnowbirdFragment() {

    private val snowbirdFileViewModel: SnowbirdFileViewModel by androidViewModel()
    private val appConfig: AppConfig by inject()
    private lateinit var viewBinding: FragmentSnowbirdListMediaBinding
    private lateinit var adapter: SnowbirdFileListAdapter
    private lateinit var groupKey: String
    private lateinit var repoKey: String
    private var currentPhotoUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            groupKey = it.getString(RESULT_VAL_RAVEN_GROUP_KEY, "")
            repoKey = it.getString(RESULT_VAL_RAVEN_REPO_KEY, "")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewBinding = FragmentSnowbirdListMediaBinding.inflate(inflater)

        return viewBinding.root
    }

    // Permission launcher for camera
    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                launchCamera()
            }
        }

    // Modern visual media picker for gallery (supports up to 10 items)
    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris: List<Uri>? ->
            if (!uris.isNullOrEmpty()) {
                handleSelectedFiles(uris)
            } else {
                Timber.d("No media selected from gallery")
            }
        }

    // Document picker for file browser (shows actual file manager, not gallery)
    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
            if (!uris.isNullOrEmpty()) {
                // Filter to only allow media files
                val filteredUris = uris.filter { uri ->
                    val mimeType = requireContext().contentResolver.getType(uri)
                    mimeType?.startsWith("image/") == true ||
                    mimeType?.startsWith("video/") == true ||
                    mimeType?.startsWith("audio/") == true
                }

                if (filteredUris.isEmpty()) {
                    dialogManager.showDialog(dialogManager.requireResourceProvider()) {
                        type = DialogType.Warning
                        title = UiText.DynamicString("Invalid File Type")
                        message = UiText.DynamicString("Please select only image, video, or audio files. Other file types are not supported.")
                        positiveButton {
                            text = UiText.StringResource(R.string.lbl_ok)
                        }
                    }
                } else {
                    if (filteredUris.size < uris.size) {
                        // Some files were filtered out
                        Toast.makeText(
                            requireContext(),
                            "Some files were skipped (only images, videos, and audio are supported)",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    handleSelectedFiles(filteredUris)
                }
            }
        }

    // Modern camera launcher using TakePicture contract for photo capture
    private val modernCameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && currentPhotoUri != null) {
                currentPhotoUri?.let { uri ->
                    Timber.d("Processing camera capture from URI: $uri")
                    handleMedia(uri)
                }
                currentPhotoUri = null
            } else {
                Timber.d("Camera capture cancelled or failed")
                currentPhotoUri = null
            }
        }

    // Custom camera launcher for video and photo with multiple capture
    private val customCameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val capturedUris =
                    result.data?.getStringArrayListExtra(CameraActivity.EXTRA_CAPTURED_URIS)
                if (!capturedUris.isNullOrEmpty()) {
                    val uris = capturedUris.map { Uri.parse(it) }
                    handleSelectedFiles(uris)
                } else {
                    Timber.w("No captures returned from custom camera")
                }
            } else {
                Timber.w("Custom camera capture cancelled or failed")
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMenu()
        setupRecyclerView()
        setupSwipeRefresh()
        initializeViewModelObservers()
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_snowbird, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_add -> {
                        Timber.d("Add button clicked!")
                        openContentPickerSheet()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun handleAudio(uri: Uri) {
        handleMedia(uri)
    }

    private fun handleImage(uri: Uri) {
        handleMedia(uri)
    }

    private fun handleVideo(uri: Uri) {
        handleMedia(uri)
    }

    private fun handleMedia(uri: Uri) {
        Timber.d("Going to upload file")
        snowbirdFileViewModel.uploadFile(groupKey, repoKey, uri)
    }

    private fun handleSelectedFiles(uris: List<Uri>) {
        if (uris.isNotEmpty()) {
            var unsupportedCount = 0
            for (uri in uris) {
                val mimeType = requireContext().contentResolver.getType(uri)
                when {
                    mimeType?.startsWith("image/") == true -> handleImage(uri)
                    mimeType?.startsWith("video/") == true -> handleVideo(uri)
                    mimeType?.startsWith("audio/") == true -> handleAudio(uri)
                    else -> {
                        unsupportedCount++
                        Timber.w("Unsupported file type: $mimeType for URI: $uri")
                    }
                }
            }

            if (unsupportedCount > 0) {
                Toast.makeText(
                    requireContext(),
                    "$unsupportedCount file(s) skipped. Only images, videos, and audio are supported.",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            Timber.d("No files selected")
        }
    }

    private fun openFilePicker() {
        // Use OpenMultipleDocuments to show the file browser (not gallery)
        // Only allow media file types (images, videos, audio)
        try {
            filePickerLauncher.launch(arrayOf("image/*", "video/*", "audio/*"))
        } catch (e: Exception) {
            Timber.e(e, "Error launching file picker")
            Toast.makeText(
                requireContext(),
                "Could not open file picker",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun openContentPickerSheet() {
        val contentPickerSheet = ContentPickerFragment { mediaType ->
            handleMediaTypeSelection(mediaType)
        }
        contentPickerSheet.show(parentFragmentManager, ContentPickerFragment.TAG)
    }

    private fun handleMediaTypeSelection(mediaType: AddMediaType) {
        when (mediaType) {
            AddMediaType.CAMERA -> openCamera()
            AddMediaType.GALLERY -> openGallery()
            AddMediaType.FILES -> openFilePicker()
        }
    }

    private fun openCamera() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                launchCamera()
            }

            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                // Show rationale dialog
                dialogManager.showDialog(dialogManager.requireResourceProvider()) {
                    type = DialogType.Warning
                    title = UiText.DynamicString("Camera Permission")
                    message = UiText.DynamicString("Camera access is needed to take pictures. Please grant permission.")
                    positiveButton {
                        text = UiText.DynamicString("Accept")
                        action = {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                    neutralButton {
                        text = UiText.DynamicString("Cancel")
                    }
                }
            }

            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun launchCamera() {
        if (appConfig.useCustomCamera) {
            // Use custom camera with photo and video support
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
                requireActivity(),
                customCameraLauncher,
                cameraConfig
            )
        } else {
            // Use system camera
            val photoFile = File(requireContext().cacheDir, "camera_${System.currentTimeMillis()}.jpg")
            currentPhotoUri = androidx.core.content.FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                photoFile
            )
            modernCameraLauncher.launch(currentPhotoUri)
        }
    }

    private fun openGallery() {
        // PickVisualMedia doesn't require READ_MEDIA permissions on Android 13+
        // The system photo picker handles access internally
        launchGallery()
    }

    private fun launchGallery() {
        val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
        try {
            galleryLauncher.launch(request)
        } catch (e: Exception) {
            Timber.e(e, "Error launching gallery picker")
            // Fallback to file picker if gallery fails
            openFilePicker()
        }
    }

    private fun setupRecyclerView() {
        adapter = SnowbirdFileListAdapter(
            onClickListener = { onClick(it) }
        )

        val spacingInPixels = resources.getDimensionPixelSize(R.dimen.list_item_spacing)
        viewBinding.snowbirdMediaRecyclerView.addItemDecoration(SpacingItemDecoration(spacingInPixels))

        viewBinding.snowbirdMediaRecyclerView.setEmptyView(R.layout.view_empty_state)
        viewBinding.snowbirdMediaRecyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        viewBinding.snowbirdMediaRecyclerView.adapter = adapter
    }

    private fun onClick(item: SnowbirdFileItem) {
//        if (!item.isDownloaded) {
        dialogManager.showDialog(dialogManager.requireResourceProvider()) {
            type = DialogType.Warning
            title = UiText.DynamicString("Download Media?")
            message = UiText.DynamicString("Are you sure you want to download this media?")
            positiveButton {
                text = UiText.DynamicString("Yes")
                action = {
                    snowbirdFileViewModel.downloadFile(groupKey, repoKey, item.name)
                }
            }
            neutralButton {
                text = UiText.DynamicString("No")
            }
        }
//        }
    }

    private fun handleMediaStateUpdate(state: SnowbirdFileViewModel.State) {
        Timber.d("state = $state")
        when (state) {
            is SnowbirdFileViewModel.State.Idle -> { /* Initial state */ }
            is SnowbirdFileViewModel.State.Loading -> onLoading()
            is SnowbirdFileViewModel.State.FetchSuccess -> onFilesFetched(state.files, state.isRefresh)
            is SnowbirdFileViewModel.State.UploadSuccess -> onFileUploaded(state.result)
            is SnowbirdFileViewModel.State.DownloadSuccess -> onFileDownloaded(state.uri)
            is SnowbirdFileViewModel.State.Error -> handleError(state.error)
        }
    }

    override fun handleError(error: SnowbirdError) {
        handleLoadingStatus(false)
        viewBinding.swipeRefreshLayout.isRefreshing = false
        super.handleError(error)
    }

    private fun onLoading() {
        handleLoadingStatus(true)
        viewBinding.swipeRefreshLayout.isRefreshing = false
    }

    private fun onFilesFetched(files: List<SnowbirdFileItem>, isRefresh: Boolean) {
        handleLoadingStatus(false)

        if (isRefresh) {
            Timber.d("Clearing SnowbirdFileItems")
            SnowbirdFileItem.clear()
        }

        saveFiles(files)

        adapter.submitList(files)
    }

    private fun onFileDownloaded(uri: Uri) {
        handleLoadingStatus(false)
        Timber.d("File successfully downloaded: $uri")

        dialogManager.showDialog(dialogManager.requireResourceProvider()) {
            type = DialogType.Success
            title = UiText.StringResource(R.string.label_success_title)
            message = UiText.DynamicString("File successfully downloaded")
            positiveButton {
                text = UiText.DynamicString("Open")
                action = {
                    openDownloadedFile(uri)
                }
            }
            neutralButton {
                text = UiText.StringResource(R.string.lbl_ok)
            }
        }
    }

    private fun openDownloadedFile(uri: Uri) {
        try {
            // The URI is already a FileProvider content URI, we can use it directly
            // Extract filename from the URI to determine MIME type
            val filename = uri.lastPathSegment ?: "file"
            val mimeType = getMimeType(filename) ?: "*/*"

            Timber.d("Opening file with URI: $uri, MIME type: $mimeType")

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Check if there's an app that can handle this intent
            if (intent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(intent)
            } else {
                // Fallback: try to open with file manager using generic MIME type
                val fileManagerIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "*/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                val chooser = Intent.createChooser(fileManagerIntent, "Open file with")
                if (chooser.resolveActivity(requireContext().packageManager) != null) {
                    startActivity(chooser)
                } else {
                    // No app can handle this
                    dialogManager.showDialog(dialogManager.requireResourceProvider()) {
                        type = DialogType.Warning
                        title = UiText.DynamicString("No App Found")
                        message = UiText.DynamicString("No app is available to open this type of file.")
                        positiveButton {
                            text = UiText.StringResource(R.string.lbl_ok)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to open downloaded file")
            dialogManager.showDialog(dialogManager.requireResourceProvider()) {
                type = DialogType.Error
                title = UiText.DynamicString("Error")
                message = UiText.DynamicString("Could not open file: ${e.message}")
                positiveButton {
                    text = UiText.StringResource(R.string.lbl_ok)
                }
            }
        }
    }

    private fun getMimeType(fileName: String): String? {
        val extension = fileName.substringAfterLast(".", "")
        return when (extension.lowercase()) {
            // Images
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "bmp" -> "image/bmp"
            "webp" -> "image/webp"
            "heic", "heif" -> "image/heic"

            // Videos
            "mp4" -> "video/mp4"
            "avi" -> "video/x-msvideo"
            "mkv" -> "video/x-matroska"
            "mov" -> "video/quicktime"
            "wmv" -> "video/x-ms-wmv"
            "flv" -> "video/x-flv"
            "webm" -> "video/webm"
            "3gp" -> "video/3gpp"

            // Audio
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "ogg" -> "audio/ogg"
            "m4a" -> "audio/mp4"
            "flac" -> "audio/flac"
            "aac" -> "audio/aac"
            "wma" -> "audio/x-ms-wma"

            // Documents
            "pdf" -> "application/pdf"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "txt" -> "text/plain"

            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        }
    }

    private fun onFileUploaded(result: FileUploadResult) {
        handleLoadingStatus(false)
        Timber.d("File successfully uploaded: $result")
        val uploadedHash = result.fileHash
        if (!uploadedHash.isNullOrBlank()) {
            SnowbirdFileItem(
                name = result.name,
                hash = uploadedHash,
                groupKey = groupKey,
                repoKey = repoKey,
                isDownloaded = true
            ).save()
        }
        snowbirdFileViewModel.fetchFiles(groupKey, repoKey, forceRefresh = false)
    }

    private fun saveFiles(files: List<SnowbirdFileItem>) {
        files.forEach { file ->
            file.saveWith(groupKey, repoKey)
        }
    }

    private fun initializeViewModelObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { snowbirdFileViewModel.mediaState.collect { state -> handleMediaStateUpdate(state) } }
                launch { snowbirdFileViewModel.fetchFiles(groupKey, repoKey, forceRefresh = false) }
            }
        }
    }

    private fun setupSwipeRefresh() {
        viewBinding.swipeRefreshLayout.setOnRefreshListener {
            lifecycleScope.launch {
                snowbirdFileViewModel.fetchFiles(groupKey, repoKey, forceRefresh = true)
            }
        }

        viewBinding.swipeRefreshLayout.setColorSchemeResources(
            R.color.colorPrimary, R.color.colorPrimaryDark
        )
    }

    override fun getToolbarTitle(): String {
        return "My Files"
    }

    companion object {
        const val RESULT_VAL_RAVEN_GROUP_KEY = "dweb_group_key"
        const val RESULT_VAL_RAVEN_REPO_KEY = "dweb_repo_key"
    }
}
