package net.opendasharchive.openarchive.services.storacha

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentStorachaClientQrBinding
import net.opendasharchive.openarchive.features.core.BaseFragment

class StorachaClientQRFragment : BaseFragment() {
    private lateinit var viewBinding: FragmentStorachaClientQrBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

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
        Handler(Looper.getMainLooper()).postDelayed({
            val action = StorachaClientQRFragmentDirections.actionFragmentStorachaClientQrToFragmentStorachaBrowseSpaces()
            findNavController().navigate(action)
        }, 2000)
    }

    override fun getToolbarTitle(): String = getString(R.string.join_space)
}
