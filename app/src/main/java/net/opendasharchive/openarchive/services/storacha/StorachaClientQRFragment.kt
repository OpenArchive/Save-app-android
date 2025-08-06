package net.opendasharchive.openarchive.services.storacha

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentStorachaClientQrBinding
import net.opendasharchive.openarchive.extensions.asQRCode
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.services.storacha.util.DidManager

class StorachaClientQRFragment : BaseFragment() {
    private lateinit var viewBinding: FragmentStorachaClientQrBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        viewBinding = FragmentStorachaClientQrBinding.inflate(inflater)
        return viewBinding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        // Generate and display DID as QR code
        val userDid = DidManager(requireContext()).getOrCreateDid()
        val qrCode = userDid.asQRCode(size = 1024)
        viewBinding.qrCode.setImageBitmap(qrCode)

        // Display the DID text for easy copying
        viewBinding.tvDid.text = userDid

        // Set up button click listener to navigate to spaces
        viewBinding.btnContinue
            .setOnClickListener {
                val action =
                    StorachaClientQRFragmentDirections.actionFragmentStorachaClientQrToFragmentStorachaBrowseSpaces()
                findNavController().navigate(action)
            }
    }

    override fun getToolbarTitle(): String = getString(R.string.join_space)
}
