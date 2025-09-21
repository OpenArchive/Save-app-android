package net.opendasharchive.openarchive.services.storacha

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
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
import net.opendasharchive.openarchive.services.storacha.model.UploadEntry
import net.opendasharchive.openarchive.services.storacha.util.CarFileCreator
import net.opendasharchive.openarchive.services.storacha.util.DidManager
import net.opendasharchive.openarchive.services.storacha.viewModel.LoadingState
import net.opendasharchive.openarchive.services.storacha.viewModel.StorachaMediaViewModel
import net.opendasharchive.openarchive.util.extensions.applyEdgeToEdgeInsets
import net.opendasharchive.openarchive.util.extensions.toggle
import okhttp3.OkHttpClient
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

class StorachaMediaFragment :
    BaseFragment(),
    MenuProvider {
    private lateinit var mBinding: FragmentStorachaMediaBinding
    private val viewModel: StorachaMediaViewModel by viewModel()
    private val args: StorachaMediaFragmentArgs by navArgs()
    private val okHttpClient: OkHttpClient by inject()
    private lateinit var mediaAdapter: StorachaMediaGridAdapter
    private lateinit var uploadOverlay: View

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

        // Initialize adapter once
        mediaAdapter = StorachaMediaGridAdapter(okHttpClient) { file ->
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
            openFilePicker()
        }

        val spaceDid = args.spaceId
        val sessionId = args.sessionId
        val userDid = DidManager(requireContext()).getOrCreateDid()
        viewModel.reset()
        viewModel.loadMoreMediaEntries(userDid, spaceDid, sessionId)

        viewModel.loading.observe(viewLifecycleOwner) {
            mBinding.loadingContainer.toggle(it)
        }

        viewModel.loadingState.observe(viewLifecycleOwner) { loadingState ->
            when (loadingState) {
                LoadingState.LOADING_FILES -> {
                    mBinding.loadingText.text = getString(R.string.loading_files)
                    uploadOverlay.toggle(false)
                }
                LoadingState.LOADING_MORE -> {
                    mBinding.loadingText.text = getString(R.string.loading_more_files)
                    uploadOverlay.toggle(false)
                }
                LoadingState.UPLOADING -> {
                    mBinding.loadingText.text = getString(R.string.uploading_files)
                    uploadOverlay.toggle(true)
                }
                LoadingState.NONE -> {
                    // Loading container will be hidden by the loading observer
                    uploadOverlay.toggle(false)
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
            result.fold(
                onSuccess = { uploadResponse ->
                    Timber.d("Upload successful: CID=${uploadResponse.cid}, Size=${uploadResponse.size}")
                    showUploadSuccessDialog(uploadResponse.cid, uploadResponse.size)
                },
                onFailure = { error ->
                    Timber.e(error, "Upload failed")
                    // You could show a toast or snackbar here
                },
            )
        }

        mBinding.rvMediaList.addOnScrollListener(
            object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
                override fun onScrolled(
                    recyclerView: androidx.recyclerview.widget.RecyclerView,
                    dx: Int,
                    dy: Int,
                ) {
                    if (dy > 0) {
                        val layoutManager = recyclerView.layoutManager as GridLayoutManager
                        val lastVisibleItem = layoutManager.findLastVisibleItemPosition()
                        val totalItemCount = layoutManager.itemCount
                        if (lastVisibleItem >= totalItemCount - 1) {
                            viewModel.loadMoreMediaEntries(userDid, spaceDid, sessionId)
                        }
                    }
                }
            },
        )

        activity?.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
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
            addMenuItem?.title = "Manage Access"
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

    private val getMultipleContentsLauncher =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
            handleSelectedFiles(uris)
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
        Timber.d("Going to upload file: $uri")

        val userDid = DidManager(requireContext()).getOrCreateDid()
        val spaceDid = args.spaceId

        // Create temporary file from URI preserving original filename
        val originalName = getFileName(uri) ?: "unknown_${System.currentTimeMillis()}"
        val extension = originalName.substringAfterLast('.', "")

        // Use original filename, but add timestamp if there's no extension to avoid conflicts
        val fileName =
            if (extension.isNotEmpty()) {
                originalName
            } else {
                "${originalName}_${System.currentTimeMillis()}"
            }
        val tempFile = File(requireContext().cacheDir, fileName)
        requireContext().contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }

        // Generate proper CAR file from the temporary file
        val carResult = CarFileCreator.createCarFile(tempFile)

        // Debug: Save CAR data to file for inspection
        val carFileName = "${
            originalName.substringBeforeLast(
                '.',
                originalName,
            )
        }_${System.currentTimeMillis()}.car"
        val carFile = File(requireContext().cacheDir, "car_files/$carFileName")
        carFile.parentFile?.mkdirs()
        carFile.writeBytes(carResult.carData)

        // Clean up temporary file
        // tempFile.delete()
        val sessionId = args.sessionId
        viewModel.uploadFile(tempFile, carResult, userDid, spaceDid, sessionId)
    }

    private fun handleSelectedFiles(uris: List<Uri>) {
        if (uris.isNotEmpty()) {
            for (uri in uris) {
                val mimeType = requireContext().contentResolver.getType(uri)
                when {
                    mimeType?.startsWith("image/") == true -> handleImage(uri)
                    mimeType?.startsWith("video/") == true -> handleVideo(uri)
                    mimeType?.startsWith("audio/") == true -> handleAudio(uri)
                    else -> {
                        Timber.d("Unknown type picked: $mimeType")
                    }
                }
            }
        } else {
            Timber.d("No images selected")
        }
    }

    private fun openFilePicker() {
        getMultipleContentsLauncher.launch("*/*")
    }

    private fun getFileName(uri: Uri): String? =
        requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            cursor.getString(nameIndex)
        }

    private fun openFileInBrowser(file: UploadEntry) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(file.gatewayUrl))
            startActivity(intent)
        } catch (e: Exception) {
            Timber.e(e, "Failed to open file in browser: ${file.cid}")
        }
    }

    override fun getToolbarTitle(): String = arguments?.getString("space_name") ?: getString(R.string.browse_files)

    private fun showUploadSuccessDialog(cid: String, size: Long) {
        dialogManager.showDialog(dialogManager.requireResourceProvider()) {
            type = DialogType.Success
            title = UiText.DynamicString("Success!")
            message = UiText.DynamicString("File uploaded successfully!\nCID:\n$cid\nSize: ${formatFileSize(size)}")
            positiveButton {
                text = UiText.DynamicString("Got it")
                action = { }
            }
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / 1024.0 / 1024.0)
            bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}
