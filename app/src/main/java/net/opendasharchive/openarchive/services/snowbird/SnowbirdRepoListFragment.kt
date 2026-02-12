package net.opendasharchive.openarchive.services.snowbird

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.services.snowbird.SnowbirdRepoListFragmentArgs
import net.opendasharchive.openarchive.services.snowbird.presentation.repo.SnowbirdRepoListScreen
import org.koin.androidx.viewmodel.ext.android.viewModel

class SnowbirdRepoListFragment : BaseSnowbirdFragment() {

    private val snowbirdRepoViewModel: SnowbirdRepoViewModel by viewModel()
    private val args: SnowbirdRepoListFragmentArgs by navArgs()
    private var vaultId: Long = 0
    private lateinit var groupKey: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            groupKey = it.getString(RESULT_VAL_RAVEN_GROUP_KEY, "")
            vaultId = it.getLong("vault_id", 0L)
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
                SnowbirdRepoListScreen(
                    viewModel = snowbirdRepoViewModel,
                    vaultId = vaultId,
                    groupKey = groupKey
                )
            }
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
                        // TODO: Implement create repo dialog
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                snowbirdRepoViewModel.events.collect { event ->
                    when (event) {
                        is SnowbirdRepoEvent.NavigateToFileList -> {
                            val action = SnowbirdRepoListFragmentDirections.actionFragmentSnowbirdListReposToFragmentSnowbirdListMedia(
                                dwebGroupKey = groupKey,
                                dwebRepoKey = event.repoKey,
                                archiveId = event.archiveId
                            )
                            findNavController().navigate(action)
                        }
                    }
                }
            }
        }
    }

    override fun getToolbarTitle(): String = "Repositories"

    companion object {
        const val RESULT_VAL_RAVEN_GROUP_KEY = "dweb_group_key"
    }
}