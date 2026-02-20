package net.opendasharchive.openarchive.services.storacha

import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentStorachaEmailVerificationSentBinding
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.dialog.DialogType
import net.opendasharchive.openarchive.features.core.dialog.showDialog
import net.opendasharchive.openarchive.services.storacha.util.StorachaAccountManager
import net.opendasharchive.openarchive.services.storacha.viewModel.StorachaEmailVerificationSentViewModel
import net.opendasharchive.openarchive.util.extensions.applyEdgeToEdgeInsets
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class StorachaEmailVerificationSentFragment : BaseFragment() {
    private lateinit var mBinding: FragmentStorachaEmailVerificationSentBinding
    private val args: StorachaEmailVerificationSentFragmentArgs by navArgs()
    private val viewModel: StorachaEmailVerificationSentViewModel by viewModel {
        val accountManager = StorachaAccountManager(requireContext())
        val currentAccount = accountManager.getCurrentAccount()
        val sessionId = currentAccount?.sessionId ?: ""
        parametersOf(requireActivity().application, sessionId)
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

        mBinding.root.applyEdgeToEdgeInsets(
            typeMask = WindowInsetsCompat.Type.navigationBars(),
        ) { insets ->
            bottomMargin = insets.bottom
        }

        // Disable hardware back button
        val backPressCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Do nothing - block back navigation
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressCallback)

        // Display the email that was passed as an argument
        mBinding.emailId.text = getString(R.string.sent_to, args.email)

        // Setup change email link
        mBinding.tvChangeEmailLink.text = Html.fromHtml(getString(R.string.change_email_link), Html.FROM_HTML_MODE_LEGACY)
        mBinding.tvChangeEmailLink.movementMethod = LinkMovementMethod.getInstance()
        mBinding.tvChangeEmailLink.setOnClickListener {
            navigateBackToLogin()
        }

        viewModel.navigateNext.observe(
            viewLifecycleOwner,
            Observer {
                val action =
                    StorachaEmailVerificationSentFragmentDirections
                        .actionFragmentStorachaEmailVerificationSentToFragmentStorachaSpaceSetupSuccess()
                findNavController().navigate(action)
            },
        )

        viewModel.showTimeoutDialog.observe(
            viewLifecycleOwner,
            Observer {
                showTimeoutDialog()
            },
        )
    }

    override fun onResume() {
        super.onResume()
        viewModel.resumePolling()
    }

    override fun onPause() {
        super.onPause()
        viewModel.pausePolling()
    }

    override fun getToolbarTitle() = getString(R.string.email_verification)

    override fun shouldShowBackButton() = false

    private fun showTimeoutDialog() {
        dialogManager.showDialog(dialogManager.requireResourceProvider()) {
            type = DialogType.Warning
            title = UiText.Dynamic("Verification Timeout")
            message =
                UiText.Dynamic(
                    "We didn't receive confirmation of your email verification. Please check your email and try again, or return to login.",
                )
            positiveButton {
                text = UiText.Dynamic("Try Again")
                action = { viewModel.tryAgain() }
            }
            neutralButton {
                text = UiText.Dynamic("Back to Login")
                action = { navigateBackToLogin() }
            }
        }
    }

    private fun navigateBackToLogin() {
        // Stop polling to prevent background requests
        viewModel.pausePolling()

        // Clear the current unverified account to prevent auto-navigation back
        val accountManager = StorachaAccountManager(requireContext())
        val currentAccount = accountManager.getCurrentAccount()

        // Remove the current unverified account completely to start fresh
        currentAccount?.email?.let { email ->
            accountManager.removeAccount(email)
        }

        // Navigate directly to login screen with clear navigation stack
        val action = StorachaEmailVerificationSentFragmentDirections.actionFragmentStorachaEmailVerificationSentToFragmentStorachaLogin()
        findNavController().navigate(action)
    }
}
