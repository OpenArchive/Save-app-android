package net.opendasharchive.openarchive.services.storacha

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentStorachaLoginBinding
import net.opendasharchive.openarchive.features.core.BaseFragment

class StorachaLoginFragment : BaseFragment() {
    private lateinit var viewBinding: FragmentStorachaLoginBinding

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
            val action = StorachaLoginFragmentDirections.actionFragmentStorachaLoginToFragmentStorachaEmailVerificationSent()
            findNavController().navigate(action)
        }
    }

    override fun getToolbarTitle(): String = getString(R.string.add_account)
}
