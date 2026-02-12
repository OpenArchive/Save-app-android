package net.opendasharchive.openarchive.services.snowbird.presentation.file

import android.Manifest
import android.app.Activity
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
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.dialog.DialogType
import net.opendasharchive.openarchive.features.core.dialog.showDialog
import net.opendasharchive.openarchive.features.media.AddMediaType
import net.opendasharchive.openarchive.features.media.ContentPickerFragment
import net.opendasharchive.openarchive.features.media.Picker
import net.opendasharchive.openarchive.features.media.camera.CameraActivity
import net.opendasharchive.openarchive.features.media.camera.CameraConfig
import net.opendasharchive.openarchive.features.settings.passcode.AppConfig
import net.opendasharchive.openarchive.services.snowbird.presentation.base.BaseSnowbirdFragment
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.io.File

class SnowbirdFileListFragment : BaseSnowbirdFragment() {

    private val viewModel: SnowbirdFileViewModel by viewModel()
    private val appConfig: AppConfig by inject()
    private lateinit var groupKey: String
    private lateinit var repoKey: String
    private var archiveId: Long = 0
    private var currentPhotoUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            groupKey = it.getString(RESULT_VAL_RAVEN_GROUP_KEY, "")
            repoKey = it.getString(RESULT_VAL_RAVEN_REPO_KEY, "")
            archiveId = it.getLong("archive_id", 0L)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SnowbirdFileListScreen(
                    viewModel = viewModel,
                    archiveId = archiveId,
                    groupKey = groupKey,
                    repoKey = repoKey
                )
            }
        }
    }

    // Permission launcher for camera
    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) launchCamera()
        }

    // Modern visual media picker for gallery
    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris: List<Uri>? ->
            uris?.forEach { handleMedia(it) }
        }

    // Document picker for file browser
    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
            uris.forEach { handleMedia(it) }
        }

    // Modern camera launcher
    private val modernCameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) currentPhotoUri?.let { handleMedia(it) }
            currentPhotoUri = null
        }

    // Custom camera launcher
    private val customCameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val capturedUris = result.data?.getStringArrayListExtra(CameraActivity.Companion.EXTRA_CAPTURED_URIS)
                capturedUris?.forEach { handleMedia(Uri.parse(it)) }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMenu()
        observeEvents()
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_snowbird, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_add -> {
                        openContentPickerSheet()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun handleMedia(uri: Uri) {
        viewModel.onAction(SnowbirdFileAction.UploadFile(uri))
    }

    private fun openContentPickerSheet() {
        val contentPickerSheet = ContentPickerFragment { mediaType ->
            when (mediaType) {
                AddMediaType.CAMERA -> openCamera()
                AddMediaType.GALLERY -> galleryLauncher.launch(
                    PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageAndVideo
                    )
                )

                AddMediaType.FILES -> filePickerLauncher.launch(
                    arrayOf(
                        "image/*",
                        "video/*",
                        "audio/*"
                    )
                )
            }
        }
        contentPickerSheet.show(parentFragmentManager, ContentPickerFragment.Companion.TAG)
    }

    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        if (appConfig.useCustomCamera) {
            val cameraConfig = CameraConfig(
                allowVideoCapture = true,
                allowPhotoCapture = true,
                allowMultipleCapture = false,
                enablePreview = true,
                showFlashToggle = true,
                showGridToggle = true,
                showCameraSwitch = true
            )
            Picker.launchCustomCamera(requireActivity(), customCameraLauncher, cameraConfig)
        } else {
            val photoFile =
                File(requireContext().cacheDir, "camera_${System.currentTimeMillis()}.jpg")
            currentPhotoUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", photoFile)
            modernCameraLauncher.launch(currentPhotoUri)
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is SnowbirdFileEvent.FileDownloaded -> {
                            dialogManager.showDialog(dialogManager.requireResourceProvider()) {
                                type = DialogType.Success
                                title = UiText.Resource(R.string.label_success_title)
                                message = UiText.Dynamic("File successfully downloaded")
                                positiveButton {
                                    text = UiText.Dynamic("Open")
                                    action = { openDownloadedFile(event.uri) }
                                }
                                neutralButton { text = UiText.Resource(R.string.lbl_ok) }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun openDownloadedFile(uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, requireContext().contentResolver.getType(uri) ?: "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Timber.Forest.e(e, "Failed to open downloaded file")
            dialogManager.showDialog(dialogManager.requireResourceProvider()) {
                type = DialogType.Error
                title = UiText.Dynamic("Error")
                message = UiText.Dynamic("Could not open file: ${e.message}")
                positiveButton {
                    text = UiText.Resource(R.string.lbl_ok)
                }
            }
        }
    }

    override fun getToolbarTitle(): String = "My Files"

    companion object {
        const val RESULT_VAL_RAVEN_GROUP_KEY = "dweb_group_key"
        const val RESULT_VAL_RAVEN_REPO_KEY = "dweb_repo_key"
    }
}