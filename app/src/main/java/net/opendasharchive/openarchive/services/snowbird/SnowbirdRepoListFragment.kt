package net.opendasharchive.openarchive.services.snowbird

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.bundle.bundleOf
import androidx.core.view.MenuProvider
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.databinding.FragmentSnowbirdListReposBinding
import net.opendasharchive.openarchive.db.SnowbirdError
import net.opendasharchive.openarchive.db.SnowbirdRepo
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.dialog.DialogType
import net.opendasharchive.openarchive.features.core.dialog.showDialog
import net.opendasharchive.openarchive.util.SpacingItemDecoration
import timber.log.Timber

class SnowbirdRepoListFragment: BaseFragment() {

    private lateinit var viewBinding: FragmentSnowbirdListReposBinding
    private lateinit var adapter: SnowbirdRepoListAdapter
    private lateinit var groupKey: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            groupKey = it.getString(RESULT_VAL_RAVEN_GROUP_KEY, "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewBinding = FragmentSnowbirdListReposBinding.inflate(inflater)

        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMenu()
        setupSwipeRefresh()
        setupViewModel()
        initializeViewModelObservers()
    }

    private fun handleRepoStateUpdate(state: SnowbirdRepoViewModel.RepoState) {
        when (state) {
            is SnowbirdRepoViewModel.RepoState.Loading -> handleLoadingStatus(true)
            is SnowbirdRepoViewModel.RepoState.RepoFetchSuccess -> handleRepoUpdate(
                state.repos,
                state.isRefresh
            )

            is SnowbirdRepoViewModel.RepoState.Error -> handleError(state.error)
            else -> Unit
        }
    }

    private fun initializeViewModelObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    snowbirdRepoViewModel.repoState.collect { state ->
                        handleRepoStateUpdate(
                            state
                        )
                    }
                }
                launch { snowbirdRepoViewModel.fetchRepos(groupKey, forceRefresh = false) }
            }
        }
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_snowbird, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_add -> {
                        dialogManager.showDialog(dialogManager.requireResourceProvider()) {
                            type = DialogType.Warning
                            title = UiText.DynamicString("Oops!")
                            message = UiText.DynamicString("Feature not implemented yet.")
                            positiveButton {
                                text = UiText.StringResource(R.string.lbl_ok)
                            }
                        }
                        true
                    }

                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupViewModel() {

        adapter = SnowbirdRepoListAdapter { repoKey ->
            AppLogger.d("Click!!")
            //findNavController().navigate(SnowbirdRepoListFragmentDirections.navigateToSnowbirdListFilesScreen(groupKey, repoKey))
            if (isJetpackNavigation) {
                val action =
                    SnowbirdRepoListFragmentDirections.actionFragmentSnowbirdListReposToFragmentSnowbirdListMedia(
                        dwebGroupKey = groupKey,
                        dwebRepoKey = repoKey
                    )
                findNavController().navigate(action)
            } else {
                setFragmentResult(
                    RESULT_REQUEST_KEY,
                    bundleOf(
                        RESULT_VAL_RAVEN_GROUP_KEY to groupKey,
                        RESULT_VAL_RAVEN_REPO_KEY to repoKey
                    )
                )
            }
        }

        val spacingInPixels = resources.getDimensionPixelSize(R.dimen.list_item_spacing)
        viewBinding.repoList.addItemDecoration(SpacingItemDecoration(spacingInPixels))

        viewBinding.repoList.layoutManager = LinearLayoutManager(requireContext())
        viewBinding.repoList.adapter = adapter

        viewBinding.repoList.setEmptyView(R.layout.view_empty_state)
    }

    private fun handleRepoUpdate(repos: List<SnowbirdRepo>, isRefresh: Boolean) {
        handleLoadingStatus(false)

        if (isRefresh) {
            Timber.d("Clearing SnowbirdRepos for group $groupKey")
            SnowbirdRepo.clear(groupKey)
            saveRepos(repos)
        }

        adapter.submitList(repos)

        if (isRefresh && repos.isEmpty()) {
            dialogManager.showDialog(dialogManager.requireResourceProvider()) {
                type = DialogType.Info
                title = UiText.StringResource(R.string.label_info_title)
                message = UiText.DynamicString("No new repositories found.")
                positiveButton {
                    text = UiText.StringResource(R.string.label_got_it)
                    action = {
                        parentFragmentManager.popBackStack()
                    }
                }
            }
        }
    }

    override fun handleError(error: SnowbirdError) {
        handleLoadingStatus(false)
        viewBinding.swipeRefreshLayout.isRefreshing = false
        super.handleError(error)
    }

    override fun handleLoadingStatus(isLoading: Boolean) {
        super.handleLoadingStatus(isLoading)
        viewBinding.swipeRefreshLayout.isRefreshing = false
    }

    private fun saveRepos(repos: List<SnowbirdRepo>) {
        repos.forEach { repo ->
            repo.groupKey = groupKey
            repo.save()
        }
    }

    private fun setupSwipeRefresh() {
        viewBinding.swipeRefreshLayout.setOnRefreshListener {
            lifecycleScope.launch {
                snowbirdRepoViewModel.fetchRepos(groupKey, forceRefresh = true)
            }
        }

        viewBinding.swipeRefreshLayout.setColorSchemeResources(
            R.color.colorPrimary, R.color.colorPrimaryDark
        )
    }

    override fun getToolbarTitle(): String {
        return "Repositories"
    }


    companion object {

        const val RESULT_REQUEST_KEY = "raven_fragment_repo_list_result"
        const val RESULT_VAL_RAVEN_GROUP_KEY = "dweb_group_key"
        const val RESULT_VAL_RAVEN_REPO_KEY = "dweb_repo_key"

        @JvmStatic
        fun newInstance(groupKey: String) =
            SnowbirdRepoListFragment().apply {
                arguments = Bundle().apply {
                    putString(RESULT_VAL_RAVEN_GROUP_KEY, groupKey)
                }
            }
    }
}