package net.opendasharchive.openarchive.services.storacha

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentStorachaSpaceSetupSuccessBinding
import net.opendasharchive.openarchive.features.core.BaseFragment

class StorachaSpaceSetupSuccessFragment : BaseFragment() {
    private lateinit var mBinding: FragmentStorachaSpaceSetupSuccessBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        mBinding = FragmentStorachaSpaceSetupSuccessBinding.inflate(inflater)

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
