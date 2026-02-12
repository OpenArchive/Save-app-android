package net.opendasharchive.openarchive.services.snowbird

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.services.snowbird.presentation.group.SnowbirdCreateGroupScreen
import org.koin.androidx.viewmodel.ext.android.viewModel

class SnowbirdCreateGroupFragment : BaseSnowbirdFragment() {

    private val snowbirdGroupViewModel: SnowbirdGroupViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                SnowbirdCreateGroupScreen(
                    viewModel = snowbirdGroupViewModel,
                    onCancel = { findNavController().popBackStack() }
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                snowbirdGroupViewModel.events.collect { event ->
                    when (event) {
                        is SnowbirdGroupEvent.NavigateToSuccess -> {
                            val action = SnowbirdCreateGroupFragmentDirections
                                .actionFragmentSnowbirdCreateGroupToFragmentSnowbirdSetupSuccess(
                                    message = event.message,
                                    dwebGroupKey = event.groupKey
                                )
                            findNavController().navigate(action)
                        }
                        is SnowbirdGroupEvent.GoBack -> {
                            findNavController().popBackStack()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    override fun getToolbarTitle(): String {
        return "Create DWeb Storage Group"
    }
}