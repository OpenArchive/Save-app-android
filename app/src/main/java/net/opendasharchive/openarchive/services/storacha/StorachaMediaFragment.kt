package net.opendasharchive.openarchive.services.storacha

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
import androidx.recyclerview.widget.LinearLayoutManager
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentStorachaMediaBinding
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.services.storacha.model.UploadEntry
import net.opendasharchive.openarchive.services.storacha.util.CarFileCreator
import net.opendasharchive.openarchive.services.storacha.util.CarFileResult
import net.opendasharchive.openarchive.services.storacha.util.DidManager
import net.opendasharchive.openarchive.services.storacha.viewModel.StorachaMediaViewModel
import net.opendasharchive.openarchive.util.extensions.applyEdgeToEdgeInsets
import net.opendasharchive.openarchive.util.extensions.toggle
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        mBinding = FragmentStorachaMediaBinding.inflate(layoutInflater)

        mBinding.addButton.applyEdgeToEdgeInsets(
            typeMask = WindowInsetsCompat.Type.navigationBars()
        ) { insets ->
            bottomMargin = insets.bottom
        }

        mBinding.rvMediaList.layoutManager = LinearLayoutManager(requireContext())
        return mBinding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
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
            mBinding.progressBar.toggle(it)
        }

        viewModel.media.observe(viewLifecycleOwner) { mediaList ->
            mBinding.projectsEmpty.toggle(mediaList.isEmpty())
            mBinding.rvMediaList.adapter =
                StorachaBrowseFilesAdapter(mediaList as List<UploadEntry>) { file ->
                    Timber.d("Selected: ${file.cid}")
                }
        }

        viewModel.uploadResult.observe(viewLifecycleOwner) { result ->
            result.fold(
                onSuccess = { uploadResponse ->
                    Timber.d("Upload successful: CID=${uploadResponse.cid}, Size=${uploadResponse.size}")
                },
                onFailure = { error ->
                    Timber.e(error, "Upload failed")
                    // You could show a toast or snackbar here
                }
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
                        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
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

        // Create temporary file from URI with original extension
        val originalName = getFileName(uri) ?: "unknown"
        val extension = originalName.substringAfterLast('.', "")
        val fileName =
            if (extension.isNotEmpty()) "temp-${System.currentTimeMillis()}.$extension" else "temp-${System.currentTimeMillis()}"
        val tempFile = File(requireContext().cacheDir, fileName)
        requireContext().contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }

        // Generate proper CAR file from the temporary file
        val carResult = CarFileCreator.createCarFile(tempFile)

        // Debug: Save CAR data to file for inspection
        val carFile =
            File(requireContext().cacheDir, "car_files/temp-${System.currentTimeMillis()}.car")
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

    override fun getToolbarTitle(): String = arguments?.getString("space_name") ?: getString(R.string.browse_files)
}
