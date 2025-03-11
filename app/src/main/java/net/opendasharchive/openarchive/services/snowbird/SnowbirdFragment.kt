package net.opendasharchive.openarchive.services.snowbird

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentSnowbirdBinding
import net.opendasharchive.openarchive.db.SnowbirdGroup
import net.opendasharchive.openarchive.extensions.getQueryParameter
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.dialog.DialogType
import net.opendasharchive.openarchive.features.core.dialog.showDialog
import net.opendasharchive.openarchive.features.main.QRScannerActivity
import timber.log.Timber

class SnowbirdFragment : BaseFragment() {
    private val CANNED_URI =
        "save+dweb::?dht=82fd345d484393a96b6e0c5d5e17a85a61c9184cc5a3311ab069d6efa0bf1410&enc=6fa27396fe298f92c91013ac54d8f316c2d45dc3bed0edec73078040aa10feed&pk=f4b404d294817cf11ea7f8ef7231626e03b74f6fafe3271b53918608afa82d12&sk=5482a8f490081be684fbadb8bde7f0a99bab8acdcf1ec094826f0f18e327e399"
    private lateinit var viewBinding: FragmentSnowbirdBinding
    private var canNavigate = false

    private val qrCodeLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val scanResult = IntentIntegrator.parseActivityResult(result.resultCode, result.data)
        if (scanResult != null) {
            if (scanResult.contents != null) {
                processScannedData(scanResult.contents)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewBinding = FragmentSnowbirdBinding.inflate(inflater)

        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewBinding.joinGroupButton.setOnClickListener {
            startQRScanner()
        }

        viewBinding.myGroupsButton.setOnClickListener {

            if (isJetpackNavigation) {
                val action =
                    SnowbirdFragmentDirections.actionFragmentSnowbirdToFragmentSnowbirdGroupList()
                findNavController().navigate(action)
            } else {
                setFragmentResult(
                    RESULT_REQUEST_KEY,
                    bundleOf(RESULT_BUNDLE_KEY to RESULT_VAL_RAVEN_MY_GROUPS)
                )
            }
        }

        viewBinding.createGroupButton.setOnClickListener {
            if (isJetpackNavigation) {
                val action =
                    SnowbirdFragmentDirections.actionFragmentSnowbirdToFragmentSnowbirdCreateGroup()
                findNavController().navigate(action)
            } else {
                setFragmentResult(
                    RESULT_REQUEST_KEY,
                    bundleOf(RESULT_BUNDLE_KEY to RESULT_VAL_RAVEN_CREATE_GROUP)
                )
            }
        }

        initializeViewModelObservers()
    }

    private fun initializeViewModelObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch {
                    snowbirdGroupViewModel.groupState.collect { state ->
                        handleGroupStateUpdate(
                            state
                        )
                    }
                }
            }
        }
    }

    private fun handleGroupStateUpdate(state: SnowbirdGroupViewModel.GroupState) {
        handleLoadingStatus(false)
        Timber.d("group state = $state")
        when (state) {
            is SnowbirdGroupViewModel.GroupState.Loading -> handleLoadingStatus(true)
            is SnowbirdGroupViewModel.GroupState.Error -> handleError(state.error)
            else -> Unit
        }
    }

    private fun startQRScanner() {
        val integrator = IntentIntegrator(requireActivity())
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setPrompt("Scan QR Code")
        integrator.setCameraId(0)  // Use the rear camera
        integrator.setBeepEnabled(false)
        integrator.setBarcodeImageEnabled(true)
        integrator.setCaptureActivity(QRScannerActivity::class.java)

        val scanningIntent = integrator.createScanIntent()

        qrCodeLauncher.launch(scanningIntent)
    }

    private fun processScannedData(uriString: String) {
        val name = uriString.getQueryParameter("name")

        if (name == null) {
            dialogManager.showDialog(dialogManager.requireResourceProvider()) {
                type = DialogType.Warning
                title = UiText.DynamicString("Oops!")
                message = UiText.DynamicString("Unable to determine group name from QR code.")
                positiveButton {
                    text = UiText.StringResource(R.string.lbl_ok)
                }
            }
            return
        }

        if (SnowbirdGroup.exists(name)) {
            dialogManager.showDialog(dialogManager.requireResourceProvider()) {
                type = DialogType.Warning
                title = UiText.DynamicString("Oops!")
                message = UiText.DynamicString("You have already joined this group.")
                positiveButton {
                    text = UiText.StringResource(R.string.lbl_ok)
                }
            }
            return
        }

        if (isJetpackNavigation) {
            val action =
                SnowbirdFragmentDirections.actionFragmentSnowbirdToFragmentSnowbirdJoinGroup(
                    uriString
                )
            findNavController().navigate(action)
        } else {

            setFragmentResult(
                RESULT_REQUEST_KEY,
                bundleOf(
                    RESULT_BUNDLE_KEY to RESULT_VAL_RAVEN_JOIN_GROUPS,
                    RESULT_VAL_RAVEN_JOIN_GROUPS_ARG to uriString
                )
            )
        }
    }

    companion object {
        const val RESULT_REQUEST_KEY = "raven_fragment_result"
        const val RESULT_BUNDLE_KEY = "raven_fragment_result_key"
        const val RESULT_VAL_RAVEN_MY_GROUPS = "raven_my_group"
        const val RESULT_VAL_RAVEN_JOIN_GROUPS = "raven_join_group"
        const val RESULT_VAL_RAVEN_JOIN_GROUPS_ARG = "raven_join_group_argument_uri"
        const val RESULT_VAL_RAVEN_CREATE_GROUP = "raven_create_group"

        @JvmStatic
        fun newInstance() = SnowbirdFragment()
    }

    override fun getToolbarTitle(): String {
        return "Raven"
    }
}