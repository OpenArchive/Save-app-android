package net.opendasharchive.openarchive.services.storacha

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentStorachaBinding
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.services.storacha.util.DidManager
import net.opendasharchive.openarchive.services.storacha.util.StorachaAccountManager

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

        viewBinding.btnJoinSpaces.setOnClickListener {
            val action = StorachaFragmentDirections.actionFragmentStorachaToFragmentStorachaClientQr("Test")
            findNavController().navigate(action)
        }

        viewBinding.btnMySpaces.setOnClickListener {
            val action = StorachaFragmentDirections.actionFragmentStorachaToFragmentStorachaBrowseSpaces()
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
        val accountManager = StorachaAccountManager(requireContext())
        val hasAccounts = DidManager(requireContext()).hasDid()
        
        // Disable My Spaces button if no accounts are logged in
        viewBinding.btnMySpaces.isEnabled = hasAccounts
        viewBinding.btnMySpaces.alpha = if (hasAccounts) 1.0f else 0.5f
    }

    override fun getToolbarTitle(): String = getString(R.string.storacha)
    
    private fun navigateToAccountManagement() {
        val accountManager = StorachaAccountManager(requireContext())
        val action = if (accountManager.hasLoggedInAccounts()) {
            StorachaFragmentDirections.actionFragmentStorachaToFragmentStorachaAccounts()
        } else {
            StorachaFragmentDirections.actionFragmentStorachaToFragmentStorachaLogin()
        }
        findNavController().navigate(action)
    }
}
