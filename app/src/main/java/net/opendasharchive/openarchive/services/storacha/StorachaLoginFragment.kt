package net.opendasharchive.openarchive.services.storacha

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import net.opendasharchive.openarchive.databinding.FragmentStorachaLoginBinding
import net.opendasharchive.openarchive.services.storacha.util.DidManager
import net.opendasharchive.openarchive.services.storacha.viewModel.StorachaLoginViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class StorachaLoginFragment : Fragment() {
    private lateinit var viewBinding: FragmentStorachaLoginBinding
    private val viewModel: StorachaLoginViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

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
        viewBinding.btLogin.setOnClickListener {
            val email =
                viewBinding.tvEmail.text
                    .toString()
                    .trim()

            if (!isValidEmail(email)) {
                viewBinding.groupNameTextfieldContainer.error = "Invalid email"
                return@setOnClickListener
            }

            val did = DidManager(requireContext()).getOrCreateDid()
            viewModel.login(email, did)
        }

        viewModel.loginResult.observe(
            viewLifecycleOwner,
            Observer { result ->
                result.onSuccess {
                    val action =
                        StorachaLoginFragmentDirections
                            .actionFragmentStorachaLoginToFragmentStorachaEmailVerificationSent()
                    findNavController().navigate(action)
                }
                result.onFailure {
                    Toast
                        .makeText(requireContext(), "Login failed: ${it.message}", Toast.LENGTH_LONG)
                        .show()
                }
            },
        )
    }

    private fun isValidEmail(email: String): Boolean =
        android.util.Patterns.EMAIL_ADDRESS
            .matcher(email)
            .matches()
}
