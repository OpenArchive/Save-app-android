package net.opendasharchive.openarchive.services.snowbird.presentation.group

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
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.services.snowbird.presentation.base.BaseSnowbirdFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

// Legacy Fragment - commented out during Navigation3 migration.
// Navigation is now handled via SaveNavGraph.kt entries.
/*
class SnowbirdGroupListFragment : BaseSnowbirdFragment() {

    private val viewModel: SnowbirdGroupListViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SnowbirdGroupListScreen(viewModel = viewModel)
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
                        val action = SnowbirdGroupListFragmentDirections.Companion.actionFragmentSnowbirdGroupListToFragmentSnowbirdCreateGroup()
                        findNavController().navigate(action)
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
                viewModel.events.collect { event ->
                    when (event) {
                        is SnowbirdGroupListEvent.NavigateToRepo -> {
                            val action = SnowbirdGroupListFragmentDirections.Companion.actionFragmentSnowbirdGroupListToFragmentSnowbirdListRepos(
                                dwebGroupKey = event.groupKey,
                                vaultId = event.vaultId
                            )
                            findNavController().navigate(action)
                        }
                        is SnowbirdGroupListEvent.NavigateToShare -> {
                            val action = SnowbirdGroupListFragmentDirections.Companion.actionFragmentSnowbirdGroupListToFragmentSnowbirdShareGroup(
                                dwebGroupKey = event.groupKey
                            )
                            findNavController().navigate(action)
                        }
                    }
                }
            }
        }
    }

    override fun getToolbarTitle(): String = "My Groups"
}
*/