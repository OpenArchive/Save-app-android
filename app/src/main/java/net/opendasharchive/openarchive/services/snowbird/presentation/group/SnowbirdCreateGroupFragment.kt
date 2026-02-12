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
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.services.snowbird.presentation.base.BaseSnowbirdFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

// Legacy Fragment - commented out during Navigation3 migration.
// Navigation is now handled via SaveNavGraph.kt entries.
/*
class SnowbirdCreateGroupFragment : BaseSnowbirdFragment() {

    private val viewModel: SnowbirdCreateGroupViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                SaveAppTheme {
                    SnowbirdCreateGroupScreen(
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is SnowbirdCreateGroupEvent.NavigateToSuccess -> {
                            val action = SnowbirdCreateGroupFragmentDirections.Companion
                                .actionFragmentSnowbirdCreateGroupToFragmentSnowbirdSetupSuccess(
                                    message = event.message,
                                    dwebGroupKey = event.groupKey
                                )
                            findNavController().navigate(action)
                        }

                        is SnowbirdCreateGroupEvent.GoBack -> {
                            findNavController().popBackStack()
                        }
                    }
                }
            }
        }
    }

    override fun getToolbarTitle(): String {
        return "Create DWeb Storage Group"
    }
}
*/