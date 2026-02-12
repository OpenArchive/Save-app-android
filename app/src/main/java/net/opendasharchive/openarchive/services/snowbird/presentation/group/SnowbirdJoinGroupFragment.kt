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

class SnowbirdJoinGroupFragment: BaseSnowbirdFragment() {

    private val snowbirdGroupViewModel: SnowbirdGroupViewModel by viewModel()

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
                // Observe QR Scanner result
                val navController = findNavController()
                navController.currentBackStackEntry?.savedStateHandle?.getLiveData<String>(SnowbirdQRScannerFragment.QR_RESULT_KEY)
                    ?.observe(viewLifecycleOwner) { result ->
                        if (result != null) {
                            snowbirdGroupViewModel.onAction(SnowbirdGroupAction.UpdateJoinUri(result))
                            navController.currentBackStackEntry?.savedStateHandle?.remove<String>(SnowbirdQRScannerFragment.QR_RESULT_KEY)
                        }
                    }

                SnowbirdJoinGroupScreen(
                    viewModel = snowbirdGroupViewModel,
                    initialUri = uriString,
                    onScanQr = {
                        val action = SnowbirdJoinGroupFragmentDirections.Companion.actionFragmentSnowbirdJoinGroupToSnowbirdQrScanner()
                        findNavController().navigate(action)
                    },
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
                            val action = SnowbirdJoinGroupFragmentDirections.Companion
                                .actionFragmentSnowbirdJoinGroupToFragmentSnowbirdSetupSuccess(
                                    message = event.message,
                                    dwebGroupKey = event.groupKey
                                )
                            findNavController().navigate(action)
                        }
                        is SnowbirdGroupEvent.GoBack -> {
                            findNavController().popBackStack()
                        }
                        is SnowbirdGroupEvent.NavigateToScanner -> {
                            val action = SnowbirdJoinGroupFragmentDirections.Companion.actionFragmentSnowbirdJoinGroupToSnowbirdQrScanner()
                            findNavController().navigate(action)
                        }
                        else -> Unit
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