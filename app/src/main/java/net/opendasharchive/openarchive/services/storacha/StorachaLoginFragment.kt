package net.opendasharchive.openarchive.services.storacha

import android.content.Context
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentStorachaLoginBinding
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.services.storacha.util.DidManager
import net.opendasharchive.openarchive.services.storacha.util.StorachaAccountManager
import net.opendasharchive.openarchive.services.storacha.viewModel.StorachaLoginViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class StorachaLoginFragment : BaseFragment() {
    private lateinit var viewBinding: FragmentStorachaLoginBinding
    private val viewModel: StorachaLoginViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        viewBinding = FragmentStorachaLoginBinding.inflate(inflater)
        return viewBinding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        // Setup clickable sign up link
        viewBinding.tvSignUpLink.text =
            Html.fromHtml(getString(R.string.sign_up_storacha), Html.FROM_HTML_MODE_LEGACY)
        viewBinding.tvSignUpLink.movementMethod = LinkMovementMethod.getInstance()

        // Setup login button click
        viewBinding.btLogin.setOnClickListener { performLogin() }

        // Setup Enter key to trigger login and dismiss keyboard
        viewBinding.tvEmail.setOnEditorActionListener { _, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_GO -> {
                    hideKeyboard()
                    performLogin()
                    true
                }

                else -> false
            }
        }

        // Handle hardware Enter key
        viewBinding.tvEmail.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                hideKeyboard()
                performLogin()
                true
            } else {
                false
            }
        }

        viewModel.loginResult.observe(
            viewLifecycleOwner,
            Observer { result ->
                result.onSuccess { loginResponse ->
                    val email =
                        viewBinding.tvEmail.text
                            .toString()
                            .trim()
                    val accountManager = StorachaAccountManager(requireContext())
                    val didManager = DidManager(requireContext())
                    val userDid = didManager.getOrCreateDid()

                    accountManager.addAccount(
                        email = email,
                        sessionId = loginResponse.sessionId,
                        isVerified = loginResponse.verified,
                        did = userDid,
                    )

                    val action =
                        if (loginResponse.verified) {
                            StorachaLoginFragmentDirections.actionFragmentStorachaLoginToFragmentStorachaSpaceSetupSuccess()
                        } else {
                            StorachaLoginFragmentDirections.actionFragmentStorachaLoginToFragmentStorachaEmailVerificationSent()
                        }
                    findNavController().navigate(action)
                }
                result.onFailure {
                    Toast
                        .makeText(
                            requireContext(),
                            "Login failed: ${it.message}",
                            Toast.LENGTH_LONG,
                        ).show()
                }
            },
        )
    }

    private fun performLogin() {
        val email =
            viewBinding.tvEmail.text
                .toString()
                .trim()

        if (!isValidEmail(email)) {
            viewBinding.groupNameTextfieldContainer.error = "Invalid email"
            return
        }

        viewBinding.groupNameTextfieldContainer.error = null

        try {
            val didManager = DidManager(requireContext())
            val did = didManager.getOrCreateDid()
            viewModel.login(email, did)
        } catch (e: Exception) {
            Toast
                .makeText(
                    requireContext(),
                    "Failed to generate DID: ${e.message}",
                    Toast.LENGTH_LONG,
                ).show()
        }
    }

    private fun isValidEmail(email: String): Boolean =
        android.util.Patterns.EMAIL_ADDRESS
            .matcher(email)
            .matches()

    private fun hideKeyboard() {
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(viewBinding.tvEmail.windowToken, 0)
    }

    override fun getToolbarTitle() = getString(R.string.label_login)

    override fun shouldShowBackButton() = true
}
