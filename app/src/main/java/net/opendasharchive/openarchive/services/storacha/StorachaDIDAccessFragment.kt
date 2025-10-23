package net.opendasharchive.openarchive.services.storacha

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.findNavController
import com.journeyapps.barcodescanner.CaptureActivity
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import net.opendasharchive.openarchive.databinding.FragmentStorachaDidAccessBinding
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.services.storacha.util.Ed25519Utils
import net.opendasharchive.openarchive.services.storacha.viewModel.StorachaDIDAccessViewModel
import net.opendasharchive.openarchive.util.extensions.applyEdgeToEdgeInsets
import org.koin.androidx.viewmodel.ext.android.viewModel

class StorachaDIDAccessFragment : BaseFragment() {
    private lateinit var binding: FragmentStorachaDidAccessBinding
    private lateinit var qrLauncher: ActivityResultLauncher<ScanOptions>
    private val viewModel: StorachaDIDAccessViewModel by viewModel()

    private val spaceId: String by lazy { arguments?.getString("space_id") ?: "" }
    private val sessionId: String by lazy { arguments?.getString("session_id") ?: "" }
    private val existingDids: Array<String> by lazy {
        arguments?.getStringArray("existing_dids") ?: emptyArray()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentStorachaDidAccessBinding.inflate(layoutInflater)

        // Register the QR result launcher
        qrLauncher =
            registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
                if (result.contents != null) {
                    val scannedText = result.contents.trim()
                    binding.tvDid.setText(scannedText)

                    // Validate the scanned DID
                    when {
                        !Ed25519Utils.isValidDid(scannedText) -> {
                            Toast
                                .makeText(
                                    requireContext(),
                                    "Invalid DID format. Please scan a valid DID key (format: did:key:z...)",
                                    Toast.LENGTH_LONG,
                                ).show()
//                            binding.tvDid.error = "Invalid DID format"
                        }

                        existingDids.contains(scannedText) -> {
                            Toast
                                .makeText(
                                    requireContext(),
                                    "DID already added",
                                    Toast.LENGTH_LONG,
                                ).show()
//                            binding.tvDid.error = "DID already added"
                        }

                        else -> {
//                            binding.tvDid.error = null
                        }
                    }
                }
            }

        binding.btOk.setOnClickListener {
            val didText =
                binding.tvDid.text
                    .toString()
                    .trim()

            when {
                didText.isEmpty() -> {
                    Toast
                        .makeText(requireContext(), "Please enter a DID", Toast.LENGTH_SHORT)
                        .show()
//                    binding.tvDid.error = "DID is required"
                }

                !Ed25519Utils.isValidDid(didText) -> {
                    Toast
                        .makeText(
                            requireContext(),
                            "Invalid DID format. Please enter a valid DID key (format: did:key:z...)",
                            Toast.LENGTH_LONG,
                        ).show()
//                    binding.tvDid.error = "Invalid DID format"
                }

                existingDids.contains(didText) -> {
                    Toast
                        .makeText(
                            requireContext(),
                            "DID already added",
                            Toast.LENGTH_LONG,
                        ).show()
//                    binding.tvDid.error = "DID already added"
                }

                else -> {
//                    binding.tvDid.error = null
                    viewModel.createDelegation(
                        sessionId = sessionId,
                        userDid = didText,
                        spaceDid = spaceId,
                    )
                }
            }
        }

        binding.ivQrScanner.setOnClickListener {
            val options = ScanOptions()
            options.setOrientationLocked(true)
            options.setPrompt("Scan DID Key")
            options.setBeepEnabled(true)
            options.setCaptureActivity(PortraitCaptureActivity::class.java)
            qrLauncher.launch(options)
        }
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonBar.applyEdgeToEdgeInsets(
            typeMask = WindowInsetsCompat.Type.tappableElement(),
        ) { insets ->
            bottomMargin = insets.bottom
        }

        setupObservers()
    }

    private fun setupObservers() {
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.btOk.isEnabled = !isLoading
        }

        viewModel.success.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast
                    .makeText(
                        requireContext(),
                        "DID access granted successfully",
                        Toast.LENGTH_SHORT,
                    ).show()
                findNavController().navigateUp()
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            if (!errorMessage.isNullOrEmpty()) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun getToolbarTitle() = "Add DID"

    override fun getToolbarSubtitle(): String? = null

    override fun shouldShowBackButton() = true
}

class PortraitCaptureActivity : CaptureActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        super.onCreate(savedInstanceState)
    }
}
