package net.opendasharchive.openarchive.services.storacha

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.findNavController
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentStorachaBinding
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.services.storacha.util.StorachaAccountManager
import net.opendasharchive.openarchive.services.storacha.util.StorachaHelper
import net.opendasharchive.openarchive.util.extensions.applyEdgeToEdgeInsets

class StorachaFragment : BaseFragment() {
    private lateinit var viewBinding: FragmentStorachaBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        viewBinding = FragmentStorachaBinding.inflate(inflater)

        return viewBinding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        viewBinding.root.applyEdgeToEdgeInsets(
            typeMask = WindowInsetsCompat.Type.navigationBars(),
        ) { insets ->
            bottomMargin = insets.bottom
        }

        viewBinding.tvStorachaDisclaimer.text =
            HtmlCompat.fromHtml(getString(R.string.storacha_disclaimer), HtmlCompat.FROM_HTML_MODE_LEGACY)
        viewBinding.tvStorachaDisclaimer.movementMethod = LinkMovementMethod.getInstance()

        viewBinding.btnJoinSpaces.setOnClickListener {
            val action =
                StorachaFragmentDirections.actionFragmentStorachaToFragmentStorachaClientQr("Test")
            findNavController().navigate(action)
        }

        viewBinding.btnMySpaces.setOnClickListener {
            val action =
                StorachaFragmentDirections.actionFragmentStorachaToFragmentStorachaBrowseSpaces()
            findNavController().navigate(action)
        }

        viewBinding.btnManageAccounts.setOnClickListener {
            navigateToAccountManagement()
        }

        updateButtonStates()
    }

    override fun onResume() {
        super.onResume()
        // Update button states when returning to this fragment
        updateButtonStates()
    }

    private fun updateButtonStates() {
        // Enable "My Spaces" button if user has logged-in accounts OR has access to spaces
        val shouldEnable = StorachaHelper.shouldEnableStorachaAccess(requireContext())

        viewBinding.btnMySpaces.isEnabled = shouldEnable
        viewBinding.btnMySpaces.alpha = if (shouldEnable) 1.0f else 0.5f
    }

    override fun getToolbarTitle(): String = getString(R.string.storacha)

    private fun navigateToAccountManagement() {
        val accountManager = StorachaAccountManager(requireContext())
        val action =
            if (accountManager.hasLoggedInAccounts()) {
                StorachaFragmentDirections.actionFragmentStorachaToFragmentStorachaAccounts()
            } else {
                StorachaFragmentDirections.actionFragmentStorachaToFragmentStorachaLogin()
            }
        findNavController().navigate(action)
    }
}
