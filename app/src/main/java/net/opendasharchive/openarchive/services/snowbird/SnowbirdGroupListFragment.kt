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
import net.opendasharchive.openarchive.databinding.FragmentSnowbirdGroupListBinding
import net.opendasharchive.openarchive.db.SnowbirdError
import net.opendasharchive.openarchive.db.SnowbirdGroup
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.dialog.DialogType
import net.opendasharchive.openarchive.features.core.dialog.showDialog
import net.opendasharchive.openarchive.util.SpacingItemDecoration
import timber.log.Timber

class SnowbirdGroupListFragment : BaseFragment() {

    private lateinit var viewBinding: FragmentSnowbirdGroupListBinding
    private lateinit var adapter: SnowbirdGroupsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewBinding = FragmentSnowbirdGroupListBinding.inflate(inflater)

        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMenu()
        setupSwipeRefresh()
        setupRecyclerView()
        initializeViewModelObservers()

        snowbirdGroupViewModel.fetchGroups()
    }

    private fun setupSwipeRefresh() {
        viewBinding.swipeRefreshLayout.setOnRefreshListener {
            snowbirdGroupViewModel.fetchGroups(true)
        }

        viewBinding.swipeRefreshLayout.setColorSchemeResources(
            R.color.colorPrimary,
            R.color.colorPrimaryDark
        )
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_snowbird, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_add -> {
                        if (isJetpackNavigation) {
                            val action =
                                SnowbirdGroupListFragmentDirections.actionFragmentSnowbirdGroupListToFragmentSnowbirdCreateGroup()
                            findNavController().navigate(action)
                        } else {
                            setFragmentResult(
                                RESULT_REQUEST_KEY,
                                bundleOf(RESULT_BUNDLE_NAVIGATION_KEY to RESULT_VAL_RAVEN_CREATE_GROUP_SCREEN)
                            )
                        }
                        true
                    }

                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupRecyclerView() {
        adapter = SnowbirdGroupsAdapter(
            onClickListener = { groupKey ->
                onClick(groupKey)
            },
            onLongPressListener = { groupKey ->
                onLongPress(groupKey)
            }
        )

        val spacingInPixels = resources.getDimensionPixelSize(R.dimen.list_item_spacing)
        viewBinding.groupList.addItemDecoration(SpacingItemDecoration(spacingInPixels))

        viewBinding.groupList.layoutManager = LinearLayoutManager(requireContext())
        viewBinding.groupList.adapter = adapter

        viewBinding.groupList.setEmptyView(R.layout.view_empty_state)
    }

    private fun onClick(groupKey: String) {
        if (isJetpackNavigation) {
            val action = SnowbirdGroupListFragmentDirections.actionFragmentSnowbirdGroupListToFragmentSnowbirdListRepos(groupKey)
            findNavController().navigate(action)
        } else {
            setFragmentResult(
                RESULT_REQUEST_KEY, bundleOf(
                    RESULT_BUNDLE_NAVIGATION_KEY to RESULT_VAL_RAVEN_REPO_LIST_SCREEN,
                    RESULT_BUNDLE_GROUP_KEY to groupKey
                )
            )
        }
    }

    private fun onLongPress(groupKey: String) {
        AppLogger.d("Long press!")
        dialogManager.showDialog(dialogManager.requireResourceProvider()) {
            type = DialogType.Info
            title = UiText.DynamicString("Share Group")
            message = UiText.DynamicString("Would you like to share this group?")
            positiveButton {
                text = UiText.DynamicString("Yes")
                action = {
                    val action = SnowbirdGroupListFragmentDirections.actionFragmentSnowbirdGroupListToFragmentSnowbirdShareGroup(groupKey)
                    findNavController().navigate(action)
                }
            }
            neutralButton {
                text = UiText.DynamicString("No")
            }
        }
    }

    private fun initializeViewModelObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    snowbirdGroupViewModel.groupState.collect { state ->
                        handleGroupStateUpdate(state)
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

    private fun handleGroupStateUpdate(state: SnowbirdGroupViewModel.GroupState) {
        when (state) {
            is SnowbirdGroupViewModel.GroupState.Loading -> onLoading()
            is SnowbirdGroupViewModel.GroupState.MultiGroupSuccess -> onGroupsFetched(
                state.groups,
                state.isRefresh
            )

            is SnowbirdGroupViewModel.GroupState.Error -> handleError(state.error)
            else -> Unit
        }
    }

    private fun onGroupsFetched(groups: List<SnowbirdGroup>, isRefresh: Boolean) {
        handleLoadingStatus(false)

        if (isRefresh) {
            Timber.d("Clearing SnowbirdGroups")
            SnowbirdGroup.clear()
            saveGroups(groups)
        }

        adapter.submitList(groups)
    }

    private fun onLoading() {
        handleLoadingStatus(true)
        viewBinding.swipeRefreshLayout.isRefreshing = false
    }

    private fun saveGroups(groups: List<SnowbirdGroup>) {
        groups.forEach { group ->
            group.save()
        }
    }

    companion object {
        const val RESULT_REQUEST_KEY = "raven_group_list_fragment_result"
        const val RESULT_BUNDLE_NAVIGATION_KEY = "raven_group_list_fragment_bundle_navigation_key"

        const val RESULT_VAL_RAVEN_CREATE_GROUP_SCREEN = "raven_create_group"
        const val RESULT_VAL_RAVEN_REPO_LIST_SCREEN = "raven_repo_list_screen"
        const val RESULT_VAL_RAVEN_SHARE_SCREEN = "raven_share_group_screen"

        const val RESULT_BUNDLE_GROUP_KEY = "dweb_group_key"

        @JvmStatic
        fun newInstance() = SnowbirdGroupListFragment()
    }

    override fun getToolbarTitle(): String {
        return "My Groups"
    }
}