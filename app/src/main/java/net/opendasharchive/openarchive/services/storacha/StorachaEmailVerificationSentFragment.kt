package net.opendasharchive.openarchive.services.storacha

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentStorachaEmailVerificationSentBinding
import net.opendasharchive.openarchive.features.core.BaseFragment

class StorachaEmailVerificationSentFragment : BaseFragment() {
    private lateinit var mBinding: FragmentStorachaEmailVerificationSentBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        mBinding = FragmentStorachaEmailVerificationSentBinding.inflate(inflater)
        return mBinding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        Handler(Looper.getMainLooper()).postDelayed({
            val action =
                StorachaEmailVerificationSentFragmentDirections
                    .actionFragmentStorachaEmailVerificationSentToFragmentStorachaSpaceSetupSuccess()
            findNavController().navigate(action)
        }, 2000)
    }

    override fun getToolbarTitle() = getString(R.string.email_verification)

    override fun shouldShowBackButton() = false
}
