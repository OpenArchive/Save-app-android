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
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentStorachaMediaBinding
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.services.storacha.model.UploadEntry
import net.opendasharchive.openarchive.services.storacha.util.DidManager
import net.opendasharchive.openarchive.services.storacha.viewModel.StorachaMediaViewModel
import net.opendasharchive.openarchive.util.extensions.toggle
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

class StorachaMediaFragment :
    BaseFragment(),
    MenuProvider {
    private lateinit var mBinding: FragmentStorachaMediaBinding
    private val viewModel: StorachaMediaViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        mBinding = FragmentStorachaMediaBinding.inflate(layoutInflater)
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

        val spaceDid = arguments?.getString("spaceDid") ?: return
        val userDid = DidManager(requireContext()).getOrCreateDid()
        viewModel.reset()
        viewModel.loadMoreMediaEntries(userDid, spaceDid)

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

        mBinding.rvMediaList.addOnScrollListener(
            object :
                androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
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
                            viewModel.loadMoreMediaEntries(userDid, spaceDid)
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
        addMenuItem?.isVisible = true
        addMenuItem?.title = "Manage Access"
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
        when (menuItem.itemId) {
            R.id.action_add -> {
                val action =
                    StorachaMediaFragmentDirections.actionFragmentStorachaMediaToFragmentStorachaViewDids()
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
        val spaceDid = arguments?.getString("spaceDid") ?: return

        val carFile = File(requireContext().cacheDir, "upload-${System.currentTimeMillis()}.car")
        requireContext().contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(carFile).use { output ->
                input.copyTo(output)
            }
        }

        val requestFile = carFile.asRequestBody("application/vnd.ipld.car".toMediaTypeOrNull())
        val filePart = MultipartBody.Part.createFormData("file", carFile.name, requestFile)
        val spaceRequestBody = spaceDid.toRequestBody("text/plain".toMediaTypeOrNull())

        viewModel.uploadFile(userDid, spaceRequestBody, filePart)
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

    override fun getToolbarTitle(): String {
        return arguments?.getString("spaceName") ?: getString(R.string.browse_files)
    }
}
