package net.opendasharchive.openarchive.services.storacha

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.CaptureActivity
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentStorachaDidAccessBinding
import net.opendasharchive.openarchive.features.core.BaseFragment

class StorachaDIDAccessFragment : BaseFragment() {
    private lateinit var binding: FragmentStorachaDidAccessBinding
    private lateinit var qrLauncher: ActivityResultLauncher<Intent>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentStorachaDidAccessBinding.inflate(layoutInflater)

        // Register the QR result launcher
        qrLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val intentResult = IntentIntegrator.parseActivityResult(result.resultCode, result.data)
                    val scanned = intentResult?.contents
                    if (scanned.isNullOrEmpty() == false) binding.tvDid.setText(scanned)
                }
            }

        binding.btOk.setOnClickListener {
        }

        binding.btBack.setOnClickListener {
        }

        binding.ivQrScanner.setOnClickListener {
            val integrator = IntentIntegrator(requireActivity())
            integrator.setOrientationLocked(true)
            integrator.setPrompt("Scan DID Key")
            integrator.setBeepEnabled(true)
            integrator.captureActivity = PortraitCaptureActivity::class.java // Use default
            qrLauncher.launch(integrator.createScanIntent())
        }
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
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
