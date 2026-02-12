package net.opendasharchive.openarchive.services.snowbird.presentation.dashboard

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.flow.collectLatest
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.features.media.AddMediaType
import net.opendasharchive.openarchive.features.media.ContentPickerSheet
import net.opendasharchive.openarchive.services.snowbird.presentation.base.BaseSnowbirdFragment
import net.opendasharchive.openarchive.services.snowbird.presentation.qrscanner.SnowbirdQRScannerFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class SnowbirdFragment : BaseSnowbirdFragment() {

    private val viewModel: SnowbirdDashboardViewModel by viewModel()

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.onAction(
                SnowbirdDashboardAction.ImagePickedForQR(
                    it,
                    requireContext()
                )
            )
        }
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.onAction(
                SnowbirdDashboardAction.ImagePickedForQR(
                    it,
                    requireContext()
                )
            )
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
                SaveAppTheme {
                    val state by viewModel.uiState.collectAsStateWithLifecycle()

                    LaunchedEffect(Unit) {
                        viewModel.events.collectLatest { event ->
                            handleDashboardEvent(event)
                        }
                    }

                    // Observe QR Scanner result from navigation
                    val navController = findNavController()
                    navController.currentBackStackEntry?.savedStateHandle?.getLiveData<String>(
                        SnowbirdQRScannerFragment.QR_RESULT_KEY
                    )
                        ?.observe(viewLifecycleOwner) { result ->
                            if (result != null) {
                                viewModel.onAction(SnowbirdDashboardAction.QRResultScanned(result))
                                navController.currentBackStackEntry?.savedStateHandle?.remove<String>(
                                    SnowbirdQRScannerFragment.QR_RESULT_KEY
                                )
                            }
                        }

                    if (state.showContentPicker) {
                        ContentPickerSheet(
                            title = "Scan QR Code",
                            onDismiss = { viewModel.onAction(SnowbirdDashboardAction.ContentPickerDismissed) },
                            onMediaPicked = { type ->
                                when (type) {
                                    AddMediaType.GALLERY -> {
                                        imagePickerLauncher.launch("image/*")
                                        viewModel.onAction(SnowbirdDashboardAction.ContentPickerDismissed)
                                    }

                                    AddMediaType.FILES -> {
                                        viewModel.onAction(SnowbirdDashboardAction.ContentPickerDismissed)
                                        filePickerLauncher.launch(arrayOf("image/*"))
                                    }

                                    AddMediaType.CAMERA -> {
                                        // Use our dedicated QR Scanner for camera
                                        viewModel.onAction(SnowbirdDashboardAction.MediaPicked(type))
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun handleDashboardEvent(event: SnowbirdDashboardEvent) {
//        when (event) {
//            is SnowbirdDashboardEvent.NavigateToCreateGroup -> {
//                val action =
//                    SnowbirdFragmentDirections.Companion.actionFragmentSnowbirdToFragmentSnowbirdCreateGroup()
//                findNavController().navigate(action)
//            }
//
//            is SnowbirdDashboardEvent.NavigateToGroupList -> {
//                val action =
//                    SnowbirdFragmentDirections.Companion.actionFragmentSnowbirdToFragmentSnowbirdGroupList()
//                findNavController().navigate(action)
//            }
//
//            is SnowbirdDashboardEvent.NavigateToJoinGroup -> {
//                val action =
//                    SnowbirdFragmentDirections.Companion.actionFragmentSnowbirdToFragmentSnowbirdJoinGroup(
//                        dwebGroupKey = event.groupKey
//                    )
//                findNavController().navigate(action)
//            }
//
//            is SnowbirdDashboardEvent.NavigateToScanner -> {
//                val action = SnowbirdFragmentDirections.Companion.actionFragmentSnowbirdToSnowbirdQrScanner()
//                findNavController().navigate(action)
//            }
//
//            is SnowbirdDashboardEvent.ShowMessage -> Unit
//            is SnowbirdDashboardEvent.ToggleServer -> {
//                if (event.enabled) {
//                    requireContext().startForegroundService(
//                        Intent(
//                            requireContext(),
//                            SnowbirdService::class.java
//                        )
//                    )
//                } else {
//                    requireContext().stopService(
//                        Intent(
//                            requireContext(),
//                            SnowbirdService::class.java
//                        )
//                    )
//                }
//            }
//        }
    }

    override fun getToolbarTitle(): String = "DWeb Storage"
}


