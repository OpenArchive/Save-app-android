package net.opendasharchive.openarchive.services.storacha

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.findNavController
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentStorachaSpaceSetupSuccessBinding
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.util.extensions.applyEdgeToEdgeInsets

class StorachaSpaceSetupSuccessFragment : BaseFragment() {
    private lateinit var mBinding: FragmentStorachaSpaceSetupSuccessBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        mBinding = FragmentStorachaSpaceSetupSuccessBinding.inflate(inflater)

        mBinding.btAuthenticate.applyEdgeToEdgeInsets(
            typeMask = WindowInsetsCompat.Type.navigationBars(),
        ) { insets ->
            bottomMargin = insets.bottom
        }

        mBinding.btAuthenticate.setOnClickListener { _ ->
            val action =
                StorachaSpaceSetupSuccessFragmentDirections
                    .actionFragmentStorachaSpaceSetupSuccessToFragmentStorachaBrowseSpaces()
            findNavController().navigate(action)
        }

        return mBinding.root
    }

    override fun getToolbarTitle() = getString(R.string.space_setup_success_title)

    override fun shouldShowBackButton() = false
}
