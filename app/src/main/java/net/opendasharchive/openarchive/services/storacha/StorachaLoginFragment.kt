package net.opendasharchive.openarchive.services.storacha

import android.content.Context
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentStorachaLoginBinding
import net.opendasharchive.openarchive.services.storacha.util.DidManager
import net.opendasharchive.openarchive.services.storacha.util.StorachaAccountManager
import net.opendasharchive.openarchive.services.storacha.viewModel.StorachaLoginViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class StorachaLoginFragment : Fragment() {
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

        viewBinding.btLogin.setOnClickListener {
            val email =
                viewBinding.tvEmail.text
                    .toString()
                    .trim()

            if (!isValidEmail(email)) {
                viewBinding.groupNameTextfieldContainer.error = "Invalid email"
                return@setOnClickListener
            }

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

        viewModel.loginResult.observe(
            viewLifecycleOwner,
            Observer { result ->
                result.onSuccess { loginResponse ->
                    val email = viewBinding.tvEmail.text.toString().trim()
                    val accountManager = StorachaAccountManager(requireContext())
                    accountManager.addAccount(email, loginResponse.sessionId)

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

    private fun isValidEmail(email: String): Boolean =
        android.util.Patterns.EMAIL_ADDRESS
            .matcher(email)
            .matches()
}
