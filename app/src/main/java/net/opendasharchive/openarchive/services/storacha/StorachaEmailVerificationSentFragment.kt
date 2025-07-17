package net.opendasharchive.openarchive.services.storacha

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentStorachaEmailVerificationSentBinding
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.services.storacha.viewModel.StorachaEmailVerificationSentViewModel

class StorachaEmailVerificationSentFragment : BaseFragment() {
    private lateinit var mBinding: FragmentStorachaEmailVerificationSentBinding
    private val viewModel: StorachaEmailVerificationSentViewModel by viewModel {
        val prefs = requireContext().getSharedPreferences("storacha_prefs", android.content.Context.MODE_PRIVATE)
        val sessionId = prefs.getString("session_id", "") ?: ""
        parametersOf(sessionId)
    }

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
        viewModel.navigateNext.observe(
            viewLifecycleOwner,
            Observer {
                val action =
                    StorachaEmailVerificationSentFragmentDirections
                        .actionFragmentStorachaEmailVerificationSentToFragmentStorachaSpaceSetupSuccess()
                findNavController().navigate(action)
            },
        )
    }

    override fun getToolbarTitle() = getString(R.string.email_verification)

    override fun shouldShowBackButton() = false
}
