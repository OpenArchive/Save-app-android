package net.opendasharchive.openarchive.services.storacha

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
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.MenuProvider
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentStorachaMediaBinding
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.dialog.DialogType
import net.opendasharchive.openarchive.features.core.dialog.showDialog
import net.opendasharchive.openarchive.features.media.Picker
import net.opendasharchive.openarchive.features.media.camera.CameraActivity
import net.opendasharchive.openarchive.features.media.camera.CameraConfig
import net.opendasharchive.openarchive.features.settings.passcode.AppConfig
import net.opendasharchive.openarchive.services.storacha.model.UploadEntry
import net.opendasharchive.openarchive.services.storacha.util.CarFileCreator
import net.opendasharchive.openarchive.services.storacha.util.CarFileResult
import net.opendasharchive.openarchive.services.storacha.util.DidManager
import net.opendasharchive.openarchive.services.storacha.viewModel.LoadingState
import net.opendasharchive.openarchive.services.storacha.viewModel.StorachaMediaViewModel
import net.opendasharchive.openarchive.util.Utility
import net.opendasharchive.openarchive.util.extensions.applyEdgeToEdgeInsets
import net.opendasharchive.openarchive.util.extensions.toggle
import okhttp3.OkHttpClient
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.io.File

class StorachaMediaFragment :
    BaseFragment(),
    MenuProvider {
    private lateinit var mBinding: FragmentStorachaMediaBinding
    private val viewModel: StorachaMediaViewModel by viewModel()
    private val args: StorachaMediaFragmentArgs by navArgs()
    private val okHttpClient: OkHttpClient by inject()
    private val appConfig: AppConfig by inject()
    private lateinit var mediaAdapter: StorachaMediaGridAdapter
    private lateinit var uploadOverlay: View
    private var currentPhotoUri: Uri? = null

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                launchCamera()
            }
        }

    private val getMultipleContentsLauncher =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
            handleSelectedFiles(uris)
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

    // Store the last failed upload details for retry
    private var lastFailedUpload: FailedUploadData? = null

    // Track if we're doing a pull-to-refresh to avoid dual loading indicators
    private var isPullToRefresh = false

    private data class FailedUploadData(
        val uri: Uri,
        val tempFile: File,
        val carFile: File,
        val userDid: String,
        val spaceDid: String,
        val sessionId: String,
        val isAdmin: Boolean,
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        mBinding = FragmentStorachaMediaBinding.inflate(layoutInflater)

        mBinding.addButton.applyEdgeToEdgeInsets(
            typeMask = WindowInsetsCompat.Type.navigationBars(),
        ) { insets ->
            bottomMargin = insets.bottom
        }

        mBinding.rvMediaList.layoutManager = GridLayoutManager(requireContext(), 3)

        // Setup swipe refresh
        mBinding.swipeRefreshLayout.setOnRefreshListener {
            isPullToRefresh = true
            refreshMedia()
        }

        // Initialize adapter once
        mediaAdapter =
            StorachaMediaGridAdapter(okHttpClient) { file ->
                Timber.d("Selected: ${file.cid}")
                openFileInBrowser(file)
            }
        mBinding.rvMediaList.adapter = mediaAdapter

        return mBinding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        uploadOverlay = view.findViewById(R.id.upload_overlay)
        mBinding.progressBar.toggle(true)

        mBinding.addButton.setOnClickListener {
            showContentPicker()
        }

        val spaceDid = args.spaceId
        val sessionId = args.sessionId.ifEmpty { null }
        val userDid = DidManager(requireContext()).getOrCreateDid()
        viewModel.reset()
        viewModel.loadMoreMediaEntries(userDid, spaceDid, sessionId)

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            // Only show center loading if it's not a pull-to-refresh
            mBinding.loadingContainer.toggle(isLoading && !isPullToRefresh)

            // Hide swipe refresh when loading is complete
            if (!isLoading) {
                mBinding.swipeRefreshLayout.isRefreshing = false
                isPullToRefresh = false // Reset the flag
            }
        }

        // Handle back press during upload - initialize callback
        val backPressCallback =
            object : OnBackPressedCallback(false) {
                override fun handleOnBackPressed() {
                    // Do nothing - block back press during upload
                    Timber.d("Back press blocked during upload")
                }
            }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressCallback)

        viewModel.loadingState.observe(viewLifecycleOwner) { loadingState ->
            when (loadingState) {
                LoadingState.LOADING_FILES -> {
                    mBinding.loadingText.text = getString(R.string.loading_files)
                    uploadOverlay.toggle(false)
                    backPressCallback.isEnabled = false
                    activity?.invalidateOptionsMenu()
                }

                LoadingState.LOADING_MORE -> {
                    mBinding.loadingText.text = getString(R.string.loading_more_files)
                    uploadOverlay.toggle(false)
                    backPressCallback.isEnabled = false
                    activity?.invalidateOptionsMenu()
                }

                LoadingState.UPLOADING -> {
                    mBinding.loadingText.text = getString(R.string.uploading_files)
                    uploadOverlay.toggle(true)
                    backPressCallback.isEnabled = true
                    activity?.invalidateOptionsMenu()
                }

                LoadingState.NONE -> {
                    // Loading container will be hidden by the loading observer
                    uploadOverlay.toggle(false)
                    backPressCallback.isEnabled = false
                    activity?.invalidateOptionsMenu()
                }
            }
        }

        viewModel.media.observe(viewLifecycleOwner) { mediaList ->
            mediaAdapter.updateFiles(mediaList)
        }

        viewModel.isEmpty.observe(viewLifecycleOwner) { isEmpty ->
            mBinding.projectsEmpty.toggle(isEmpty)
        }

        viewModel.uploadResult.observe(viewLifecycleOwner) { result ->
            result?.fold(
                onSuccess = { uploadResponse ->
                    Timber.d("Upload successful: CID=${uploadResponse.cid}, Size=${uploadResponse.size}")
                    // Clean up temporary files after successful upload
                    lastFailedUpload?.let { failedUpload ->
                        try {
                            failedUpload.tempFile.delete()
                            failedUpload.carFile.delete()
                            Timber.d("Cleaned up temporary files after successful upload")
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to delete temporary files")
                        }
                    }
                    lastFailedUpload = null // Clear any previous failed upload
                    showUploadSuccessDialog(uploadResponse.cid, uploadResponse.size)
                    // Clear the result immediately after showing the dialog to prevent re-showing
                    viewModel.clearUploadResult()
                },
                onFailure = { error ->
                    Timber.e(error, "Upload failed")

                    // Don't show generic error dialog if we're showing session expired dialog
                    // This is handled by separate observer
                    val isAuthError = viewModel.sessionExpired.value == true

                    if (!isAuthError) {
                        showUploadErrorDialog(error)
                    }

                    // Clear the result immediately after showing the dialog to prevent re-showing
                    viewModel.clearUploadResult()
                },
            )
        }

        // Observe session expiration
        viewModel.sessionExpired.observe(viewLifecycleOwner) { expired ->
            if (expired) {
                showSessionExpiredDialog()
            }
        }

        mBinding.rvMediaList.addOnScrollListener(
            object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
                override fun onScrolled(
                    recyclerView: androidx.recyclerview.widget.RecyclerView,
                    dx: Int,
                    dy: Int,
                ) {
                    if (dy > 0) { // Scrolling down
                        val layoutManager = recyclerView.layoutManager as GridLayoutManager
                        val lastVisibleItem = layoutManager.findLastVisibleItemPosition()
                        val totalItemCount = layoutManager.itemCount
                        val threshold = 3 // Load more when 3 items from the end

                        if (lastVisibleItem >= totalItemCount - threshold && totalItemCount > 0) {
                            Timber.d(
                                "Scroll trigger: lastVisible=$lastVisibleItem, total=$totalItemCount, loading=${viewModel.loading.value}",
                            )
                            viewModel.loadMoreMediaEntries(userDid, spaceDid, sessionId)
                        }
                    }
                }
            },
        )

        activity?.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onResume() {
        super.onResume()
        // Clear upload result when returning from another screen to prevent showing stale success dialog
        viewModel.clearUploadResult()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up any leftover temporary files
        lastFailedUpload?.let { failedUpload ->
            try {
                if (failedUpload.tempFile.exists()) {
                    failedUpload.tempFile.delete()
                    Timber.d("Cleaned up temp file: ${failedUpload.tempFile.name}")
                }
                if (failedUpload.carFile.exists()) {
                    failedUpload.carFile.delete()
                    Timber.d("Cleaned up CAR file: ${failedUpload.carFile.name}")
                }
            } catch (e: Exception) {
                Timber.e("Failed to clean up temp files: ${e.message}")
            }
        }
    }

    override fun onCreateMenu(
        menu: Menu,
        menuInflater: MenuInflater,
    ) {
        menuInflater.inflate(R.menu.menu_browse_folder, menu)
    }

    override fun onPrepareMenu(menu: Menu) {
        super.onPrepareMenu(menu)
        val addMenuItem = menu.findItem(R.id.action_add)
        if (args.isAdmin) {
            addMenuItem?.isVisible = true
            addMenuItem?.title = getString(R.string.manage_access)
            addMenuItem?.isEnabled = viewModel.loadingState.value != LoadingState.UPLOADING
        } else {
            addMenuItem?.isVisible = false
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
        when (menuItem.itemId) {
            R.id.action_add -> {
                val action =
                    StorachaMediaFragmentDirections.actionFragmentStorachaMediaToFragmentStorachaViewDids(
                        spaceId = args.spaceId,
                        sessionId = args.sessionId,
                    )
                findNavController().navigate(action)
                true
            }

            else -> false
        }

    private fun handleMedia(uri: Uri) {
        Timber.d("Going to upload file: $uri")

        // Clean up any previous failed upload before starting a new one
        lastFailedUpload?.let { previousFailedUpload ->
            try {
                if (previousFailedUpload.tempFile.exists()) {
                    previousFailedUpload.tempFile.delete()
                    Timber.d("Cleaned up previous failed upload temp file: ${previousFailedUpload.tempFile.name}")
                }
                if (previousFailedUpload.carFile.exists()) {
                    previousFailedUpload.carFile.delete()
                    Timber.d("Cleaned up previous failed upload CAR file: ${previousFailedUpload.carFile.name}")
                }
            } catch (e: Exception) {
                Timber.e("Failed to clean up previous failed upload: ${e.message}")
            }
            lastFailedUpload = null
        }

        val userDid = DidManager(requireContext()).getOrCreateDid()
        val spaceDid = args.spaceId

        // Check if URI is a FileProvider URI pointing to our cache directory
        val tempFile =
            try {
                if (uri.scheme == "content" && uri.authority == "${requireContext().packageName}.provider") {
                    // This is likely from our camera - try to get the actual file path
                    Timber.d("Camera URI detected: $uri, path: ${uri.path}")
                    val path = uri.path?.removePrefix("/cache/")
                    if (path != null && path != uri.path) {
                        val existingFile = File(requireContext().cacheDir, path)
                        Timber.d(
                            "Checking for existing file at: ${existingFile.absolutePath}, exists: ${existingFile.exists()}, size: ${existingFile.length()}",
                        )
                        if (existingFile.exists() && existingFile.length() > 0) {
                            Timber.d("Using existing camera file: ${existingFile.absolutePath}, size: ${existingFile.length()} bytes")
                            existingFile
                        } else {
                            Timber.w("File does not exist or is empty: ${existingFile.absolutePath}")
                            null
                        }
                    } else {
                        Timber.w("Could not extract path from URI: $uri")
                        null
                    }
                } else {
                    null
                }
            } catch (e: Exception) {
                Timber.e(e, "Error checking for existing file")
                null
            }

        val finalTempFile =
            if (tempFile != null) {
                // Use the existing file from camera
                tempFile
            } else {
                // Need to copy from URI (gallery, etc.)
                val title =
                    Utility.getUriDisplayName(requireContext(), uri)
                        ?: "IMG_${System.currentTimeMillis()}.jpg"
                val newTempFile = Utility.getOutputMediaFileByCacheNoTimestamp(requireContext(), title)

                if (newTempFile == null) {
                    Timber.e("Failed to create temp file for URI: $uri")
                    showError("Failed to create temporary file")
                    return
                }

                // Use the exact same pattern as Picker.kt for file copying
                try {
                    requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                        if (!Utility.writeStreamToFile(inputStream, newTempFile)) {
                            Timber.e("Failed to write stream to file for URI: $uri")
                            showError("Failed to copy image data")
                            return
                        }
                    } ?: run {
                        Timber.e("Failed to open input stream for URI: $uri")
                        showError("Failed to read image")
                        return
                    }
                } catch (e: java.io.FileNotFoundException) {
                    Timber.e(e, "File not found for URI: $uri")
                    showError("File not found")
                    return
                } catch (e: SecurityException) {
                    Timber.e(e, "Permission denied for URI: $uri")
                    showError("Permission denied to read image")
                    return
                } catch (e: java.io.IOException) {
                    Timber.e(e, "IO error reading URI: $uri")
                    showError("Failed to read image: ${e.message}")
                    return
                }

                // Verify file was actually written with content
                if (!newTempFile.exists() || newTempFile.length() == 0L) {
                    Timber.e("Temp file is empty after copy. File exists: ${newTempFile.exists()}, size: ${newTempFile.length()}")
                    showError("Image file is empty (0 bytes). Please try again.")
                    return
                }

                Timber.d("Successfully copied file from URI to temp file: ${newTempFile.absolutePath}, size: ${newTempFile.length()} bytes")
                newTempFile
            }

        // Generate proper CAR file from the temporary file (writes directly to cache dir)
        val carResult = CarFileCreator.createCarFile(finalTempFile, requireContext().cacheDir)

        val uploadSessionId = args.sessionId.ifEmpty { null }

        // Store upload details for potential retry and cleanup
        lastFailedUpload =
            FailedUploadData(
                uri = uri,
                tempFile = finalTempFile,
                carFile = carResult.carFile,
                userDid = userDid,
                spaceDid = spaceDid,
                sessionId = uploadSessionId ?: "",
                isAdmin = args.isAdmin,
            )

        viewModel.uploadFile(
            finalTempFile,
            carResult,
            userDid,
            spaceDid,
            uploadSessionId,
            args.isAdmin,
        )
    }

    private fun handleSelectedFiles(uris: List<Uri>) {
        if (uris.isNotEmpty()) {
            for (uri in uris) {
                handleMedia(uri)
            }
        } else {
            Timber.d("No files selected")
        }
    }

    private fun openFilePicker() {
        getMultipleContentsLauncher.launch("*/*")
    }

    private fun showContentPicker() {
        val contentPicker =
            StorachaContentPickerFragment { contentType ->
                when (contentType) {
                    StorachaContentType.CAMERA -> openCamera()
                    StorachaContentType.FILES -> openFilePicker()
                }
            }
        contentPicker.show(parentFragmentManager, StorachaContentPickerFragment.TAG)
    }

    private fun openCamera() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA,
            ) == PackageManager.PERMISSION_GRANTED -> {
                launchCamera()
            }

            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                // Show rationale dialog
                dialogManager.showDialog(dialogManager.requireResourceProvider()) {
                    type = DialogType.Warning
                    title = UiText.DynamicString("Camera Permission")
                    message =
                        UiText.DynamicString("Camera access is needed to take pictures. Please grant permission.")
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
            val cameraConfig =
                CameraConfig(
                    allowVideoCapture = true,
                    allowPhotoCapture = true,
                    allowMultipleCapture = false,
                    enablePreview = true,
                    showFlashToggle = true,
                    showGridToggle = true,
                    showCameraSwitch = true,
                    useCleanFilenames = true, // Use IMG_123.jpg instead of 20250119_143045.IMG_123.jpg
                )
            Picker.launchCustomCamera(
                requireActivity(),
                customCameraLauncher,
                cameraConfig,
            )
        } else {
            // Use modern camera launcher (photo only) with custom filename format
            // File will be named: IMG_1234567890.jpg (without double timestamp)
            try {
                val fileName = "IMG_${System.currentTimeMillis()}.jpg"
                val file = Utility.getOutputMediaFileByCacheNoTimestamp(requireContext(), fileName)

                file?.let {
                    currentPhotoUri =
                        androidx.core.content.FileProvider.getUriForFile(
                            requireContext(),
                            "${requireContext().packageName}.provider",
                            it,
                        )
                    Timber.d("Launching modern camera with URI: $currentPhotoUri, filename: $fileName")
                    modernCameraLauncher.launch(currentPhotoUri)
                } ?: run {
                    Timber.e("Failed to create temp file for camera")
                    Toast
                        .makeText(requireContext(), "Failed to prepare camera", Toast.LENGTH_SHORT)
                        .show()
                }
            } catch (e: Exception) {
                Timber.e(e, "Error setting up camera")
                Toast.makeText(requireContext(), "Camera setup failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun openFileInBrowser(file: UploadEntry) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, file.gatewayUrl.toUri())
            startActivity(intent)
        } catch (e: Exception) {
            Timber.e(e, "Failed to open file in browser: ${file.cid}")
        }
    }

    override fun getToolbarTitle(): String = arguments?.getString("space_name") ?: getString(R.string.browse_files)

    private fun showUploadSuccessDialog(
        cid: String,
        size: Long,
    ) {
        dialogManager.showDialog(dialogManager.requireResourceProvider()) {
            type = DialogType.Success
            title = UiText.DynamicString("Success!")
            message =
                UiText.DynamicString(
                    "File uploaded successfully!\nCID:\n$cid\nSize: ${
                        formatFileSize(size)
                    }",
                )
            positiveButton {
                text = UiText.DynamicString("Got it")
                action = { }
            }
        }
    }

    private fun showUploadErrorDialog(error: Throwable) {
        val fullErrorMessage = error.message ?: "Unknown error"
        val userFriendlyMessage = parseErrorMessage(fullErrorMessage)

        dialogManager.showDialog(dialogManager.requireResourceProvider()) {
            type = DialogType.Error
            title = UiText.DynamicString("Upload Failed")
            message = UiText.DynamicString(userFriendlyMessage)
            positiveButton {
                text = UiText.DynamicString("Try Again")
                action = { retryLastUpload() }
            }
            neutralButton {
                text = UiText.DynamicString("Cancel")
                action = {
                    lastFailedUpload = null // Clear failed upload data
                }
            }
        }
    }

    private fun parseErrorMessage(fullMessage: String): String =
        when {
            // Session/Authentication issues are now handled by AuthInterceptor + ViewModel observers
            // (showEmailVerificationRequiredDialog / showSessionExpiredDialog)
            // This old code path is removed to avoid duplicate error messages

            // Permission issues - Clear and actionable
            fullMessage.contains("Claim not authorized", ignoreCase = true) ||
                fullMessage.contains(
                    "Access forbidden",
                    ignoreCase = true,
                ) || fullMessage.contains("403") -> {
                getString(R.string.you_don_t_have_permission_to_upload_to_this_space_please_check_with_the_space_owner)
            }

            // Network connectivity issues
            fullMessage.contains("network", ignoreCase = true) ||
                fullMessage.contains(
                    "connection",
                    ignoreCase = true,
                ) || fullMessage.contains("ConnectException", ignoreCase = true) -> {
                getString(R.string.can_t_connect_to_the_server_please_check_your_internet_connection_and_try_again)
            }

            // Service unavailable
            fullMessage.contains(
                "Cannot POST",
                ignoreCase = true,
            ) || fullMessage.contains("503") ||
                fullMessage.contains(
                    "service unavailable",
                    ignoreCase = true,
                )
            -> {
                getString(R.string.the_storage_service_is_temporarily_unavailable_please_try_again_in_a_few_minutes)
            }

            // File too large or timeout
            fullMessage.contains("timeout", ignoreCase = true) ||
                fullMessage.contains(
                    "too large",
                    ignoreCase = true,
                ) || fullMessage.contains("431") -> {
                getString(
                    R.string.the_file_may_be_too_large_or_the_upload_is_taking_too_long_try_with_a_smaller_file_or_check_your_connection,
                )
            }

            // Server overloaded
            fullMessage.contains("429") ||
                fullMessage.contains(
                    "too many requests",
                    ignoreCase = true,
                )
            -> {
                getString(R.string.the_server_is_busy_please_wait_a_moment_and_try_again)
            }

            // Generic server errors
            fullMessage.contains("500") ||
                fullMessage.contains(
                    "server error",
                    ignoreCase = true,
                )
            -> {
                getString(R.string.something_went_wrong_on_the_server_please_try_again)
            }

            // Token/authorization issues (usually auto-retried)
            fullMessage.contains(
                "InvalidToken",
                ignoreCase = true,
            ) ||
                fullMessage.contains(
                    "expired",
                    ignoreCase = true,
                ) || fullMessage.contains("delegation", ignoreCase = true) -> {
                getString(R.string.there_was_an_authentication_issue_the_app_will_try_again_automatically)
            }

            // Storage/Bridge specific issues
            fullMessage.contains(
                "Bridge",
                ignoreCase = true,
            ) ||
                fullMessage.contains(
                    "S3 upload failed",
                    ignoreCase = true,
                ) ||
                fullMessage.contains(
                    "store/add",
                    ignoreCase = true,
                ) || fullMessage.contains("upload/add", ignoreCase = true) -> {
                getString(R.string.there_was_a_problem_with_the_storage_service_please_try_again)
            }

            // Space/storage issues
            fullMessage.contains("space", ignoreCase = true) ||
                fullMessage.contains(
                    "storage",
                    ignoreCase = true,
                )
            -> {
                getString(R.string.there_might_not_be_enough_storage_space_available_please_try_again_or_contact_support)
            }

            // Generic fallback - keep it simple
            else -> {
                getString(R.string.something_went_wrong_with_the_upload_please_try_again)
            }
        }

    private fun retryLastUpload() {
        lastFailedUpload?.let { failedUpload ->
            Timber.d("Retrying upload for file: ${failedUpload.uri}")

            // Clean up old CAR file if it exists
            try {
                if (failedUpload.carFile.exists()) {
                    failedUpload.carFile.delete()
                    Timber.d("Deleted old CAR file before retry: ${failedUpload.carFile.name}")
                }
            } catch (e: Exception) {
                Timber.e("Failed to delete old CAR file: ${e.message}")
            }

            // Regenerate CAR file for retry
            val carResult =
                CarFileCreator.createCarFile(failedUpload.tempFile, requireContext().cacheDir)
            val retrySessionId = failedUpload.sessionId.ifEmpty { null }

            // Update lastFailedUpload with new CAR file
            lastFailedUpload = failedUpload.copy(carFile = carResult.carFile)

            viewModel.uploadFile(
                failedUpload.tempFile,
                carResult,
                failedUpload.userDid,
                failedUpload.spaceDid,
                retrySessionId,
                failedUpload.isAdmin,
            )
        }
    }

    private fun refreshMedia() {
        val spaceDid = args.spaceId
        val sessionId = args.sessionId.ifEmpty { null }
        val userDid = DidManager(requireContext()).getOrCreateDid()

        viewModel.refreshFromStart()
        viewModel.loadMoreMediaEntries(userDid, spaceDid, sessionId)
    }

    private fun formatFileSize(bytes: Long): String =
        when {
            bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / 1024.0 / 1024.0)
            bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
}
