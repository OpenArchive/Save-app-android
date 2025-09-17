package net.opendasharchive.openarchive.services.storacha

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentStorachaAccountDetailsBinding
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.services.storacha.util.StorachaAccountManager
import net.opendasharchive.openarchive.services.storacha.viewModel.StorachaAccountDetailsViewModel
import net.opendasharchive.openarchive.util.extensions.applyEdgeToEdgeInsets
import org.koin.androidx.viewmodel.ext.android.viewModel

class StorachaAccountDetailsFragment : BaseFragment() {
    private lateinit var binding: FragmentStorachaAccountDetailsBinding
    private val viewModel: StorachaAccountDetailsViewModel by viewModel()
    private val args: StorachaAccountDetailsFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentStorachaAccountDetailsBinding.inflate(layoutInflater)

        binding.buttonBar.applyEdgeToEdgeInsets(
            typeMask = WindowInsetsCompat.Type.navigationBars(),
        ) { insets ->
            bottomMargin = insets.bottom
        }

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        loadAccountData()

        binding.btLogout.setOnClickListener {
            performLogout()
        }
    }

    private fun setupObservers() {
        viewModel.accountUsage.observe(viewLifecycleOwner) { result ->
            result
                .onSuccess { accountUsage ->
                    updateUI(accountUsage)
                }.onFailure { error ->
                    // Handle error - could show a Toast or error message
                    binding.tvPackage.text = "Error loading data"
                    binding.tvUtilisation.text = "Unable to fetch usage"
                }
        }

//        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
//            binding.piUsage.isVisible = !isLoading
//        }

        viewModel.logoutResult.observe(viewLifecycleOwner) { result ->
            result
                .onSuccess {
                    val accountManager = StorachaAccountManager(requireContext())
                    accountManager.removeAccount(args.email)
                    val action =
                        StorachaAccountDetailsFragmentDirections.actionFragmentStorachaAccountDetailsToFragmentStoracha()
                    findNavController().navigate(action)
                }.onFailure {
                    // Even if API logout fails, remove locally and navigate back
                    val accountManager = StorachaAccountManager(requireContext())
                    accountManager.removeAccount(args.email)
                    val action =
                        StorachaAccountDetailsFragmentDirections.actionFragmentStorachaAccountDetailsToFragmentStoracha()
                    findNavController().navigate(action)
                }
        }
    }

    private fun loadAccountData() {
        binding.etEmail.setText(args.email)
        viewModel.loadAccountUsage(args.sessionId)
    }

    private fun updateUI(accountUsage: net.opendasharchive.openarchive.services.storacha.model.AccountUsageResponse) {
        val maxBytes = 2L * 1024 * 1024 * 1024 * 1024 // 2TB
        val usagePercentage = viewModel.getUsagePercentage(accountUsage.totalUsage.bytes, maxBytes)

        binding.tvPackage.text = "Business"
        binding.tvAllocationBilling.text = "2TiB and $0.03/over that"
        binding.tvUtilisation.text =
            viewModel.formatUsageText(accountUsage.totalUsage.bytes)
        binding.piUsage.progress = usagePercentage
    }

    private fun performLogout() {
        viewModel.logout(args.sessionId)
    }

    override fun getToolbarTitle(): String = getString(R.string.account)
}
