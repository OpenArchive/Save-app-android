package net.opendasharchive.openarchive.services.snowbird

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
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentSnowbirdListMediaBinding
import net.opendasharchive.openarchive.db.FileUploadResult
import net.opendasharchive.openarchive.db.SnowbirdError
import net.opendasharchive.openarchive.db.SnowbirdFileItem
import net.opendasharchive.openarchive.extensions.androidViewModel
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.dialog.DialogType
import net.opendasharchive.openarchive.features.core.dialog.showDialog
import net.opendasharchive.openarchive.util.SpacingItemDecoration
import timber.log.Timber

class SnowbirdFileListFragment : BaseFragment() {

    private val snowbirdFileViewModel: SnowbirdFileViewModel by androidViewModel()
    private lateinit var viewBinding: FragmentSnowbirdListMediaBinding
    private lateinit var adapter: SnowbirdFileListAdapter
    private lateinit var groupKey: String
    private lateinit var repoKey: String

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
                        Timber.d("Adde!")
                        openFilePicker()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private val getMultipleContentsLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
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
        Timber.d("Going to upload file")
        snowbirdFileViewModel.uploadFile(groupKey, repoKey, uri)
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

    private fun setupRecyclerView() {
        adapter = SnowbirdFileListAdapter(
            onClickListener = { onClick(it) }
        )

        val spacingInPixels = resources.getDimensionPixelSize(R.dimen.list_item_spacing)
        viewBinding.snowbirdMediaRecyclerView.addItemDecoration(SpacingItemDecoration(spacingInPixels))

        viewBinding.snowbirdMediaRecyclerView.setEmptyView(R.layout.view_empty_state)
        // viewBinding.snowbirdMediaRecyclerView.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        viewBinding.snowbirdMediaRecyclerView.layoutManager = LinearLayoutManager(requireContext())
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
                text = UiText.StringResource(R.string.label_got_it)
            }
        }
    }

    private fun onFileUploaded(result: FileUploadResult) {
        handleLoadingStatus(false)
        Timber.d("File successfully uploaded: $result")
        SnowbirdFileItem(
            name = result.name,
            hash = result.updatedCollectionHash,
            groupKey = groupKey,
            repoKey = repoKey,
            isDownloaded = true
        ).save()
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

        @JvmStatic
        fun newInstance(groupKey: String, repoKey: String) =
            SnowbirdFileListFragment().apply {
                arguments = Bundle().apply {
                    putString(RESULT_VAL_RAVEN_GROUP_KEY, groupKey)
                    putString(RESULT_VAL_RAVEN_REPO_KEY, repoKey)
                }
            }
    }
}