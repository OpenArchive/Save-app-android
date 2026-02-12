package net.opendasharchive.openarchive.services.snowbird.presentation.group

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.services.snowbird.presentation.base.BaseSnowbirdFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

// Legacy Fragment - commented out during Navigation3 migration.
// Navigation is now handled via SaveNavGraph.kt entries.
/*
class SnowbirdJoinGroupFragment: BaseSnowbirdFragment() {

    private val snowbirdJoinGroupViewModel: SnowbirdJoinGroupViewModel by viewModel()

    private val args: SnowbirdJoinGroupFragmentArgs by navArgs()

    private var uriString: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        uriString = args.dwebGroupKey
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                SnowbirdJoinGroupScreen(
                    viewModel = snowbirdJoinGroupViewModel,
                    initialUri = uriString,
                    onCancel = { findNavController().popBackStack() }
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                snowbirdJoinGroupViewModel.events.collect { event ->
                    when (event) {
                        is SnowbirdJoinGroupEvent.NavigateToSuccess -> {
                            val action = SnowbirdJoinGroupFragmentDirections.Companion
                                .actionFragmentSnowbirdJoinGroupToFragmentSnowbirdSetupSuccess(
                                    message = event.message,
                                    dwebGroupKey = event.groupKey
                                )
                            findNavController().navigate(action)
                        }
                        is SnowbirdJoinGroupEvent.GoBack -> {
                            findNavController().popBackStack()
                        }
                    }
                }
            }
        }
    }

    override fun getToolbarTitle(): String {
        return "Join Group"
    }

    companion object {
        const val DWEB_GROUP_KEY = "dweb_group_key"
    }
}
*/