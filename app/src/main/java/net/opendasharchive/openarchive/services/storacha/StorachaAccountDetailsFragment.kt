package net.opendasharchive.openarchive.services.storacha

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentStorachaAccountDetailsBinding
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.dialog.DialogType
import net.opendasharchive.openarchive.features.core.dialog.showDialog
import net.opendasharchive.openarchive.services.storacha.util.StorachaAccountManager
import net.opendasharchive.openarchive.services.storacha.viewModel.StorachaAccountDetailsViewModel
import net.opendasharchive.openarchive.util.extensions.applyEdgeToEdgeInsets
import net.opendasharchive.openarchive.util.extensions.toggle
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class StorachaAccountDetailsFragment : BaseFragment() {
    private lateinit var binding: FragmentStorachaAccountDetailsBinding
    private val viewModel: StorachaAccountDetailsViewModel by viewModel()
    private val args: StorachaAccountDetailsFragmentArgs by navArgs()
    private lateinit var spacesAdapter: SpacesUsageAdapter

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

        setupRecyclerView()
        setupObservers()
        setupFilterButtons()
        loadAccountData()

        binding.btLogout.setOnClickListener {
            performLogout()
        }
    }

    private fun setupRecyclerView() {
        spacesAdapter = SpacesUsageAdapter()
        binding.rvSpaces.apply {
            adapter = spacesAdapter
            layoutManager = LinearLayoutManager(requireContext())
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

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.loadingContainer.toggle(isLoading)
            if (isLoading) {
                binding.loadingText.text = getString(R.string.loading_usage)
            }
        }

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
        // Log planProduct for debugging
        Timber.d("Account plan product: ${accountUsage.planProduct}")

        // Check if we have valid plan information
        val hasPlanInfo =
            !accountUsage.planProduct.isNullOrBlank() &&
                (
                    accountUsage.planProduct.contains("starter", ignoreCase = true) ||
                        accountUsage.planProduct.contains("free", ignoreCase = true) ||
                        accountUsage.planProduct.contains("lite", ignoreCase = true) ||
                        accountUsage.planProduct.contains("basic", ignoreCase = true) ||
                        accountUsage.planProduct.contains(
                            "business",
                            ignoreCase = true,
                        ) ||
                        accountUsage.planProduct.contains("pro", ignoreCase = true) ||
                        accountUsage.planProduct.contains(
                            "enterprise",
                            ignoreCase = true,
                        )
                )

        if (hasPlanInfo) {
            // Show plan-based information
            val (planName, storageLimit, monthlyCost, additionalCost) =
                when {
                    accountUsage.planProduct.contains(
                        "starter",
                        ignoreCase = true,
                    ) || accountUsage.planProduct.contains("free", ignoreCase = true) ->
                        Tuple4("Starter", 5L * 1024 * 1024 * 1024, 0.0, 0.15) // 5GB

                    accountUsage.planProduct.contains(
                        "lite",
                        ignoreCase = true,
                    ) ||
                        accountUsage.planProduct.contains(
                            "basic",
                            ignoreCase = true,
                        )
                    ->
                        Tuple4("Lite", 100L * 1024 * 1024 * 1024, 10.0, 0.05) // 100GB

                    else -> // business/pro/enterprise
                        Tuple4("Business", 2L * 1024 * 1024 * 1024 * 1024, 100.0, 0.03) // 2TB
                }

            Timber.d("Showing plan info: $planName with ${formatBytes(storageLimit)} storage limit")

            // Show and populate plan information
            binding.tvPackage.isVisible = true
            binding.tvAllocationBilling.isVisible = false
            binding.piUsage.isVisible = false

            val packageText = "$planName Plan"
//                if (monthlyCost > 0) {
//                    "$planName Plan - $${monthlyCost.toInt()}/month"
//                } else {
//                    "$planName Plan - Free"
//                }
            binding.tvPackage.text = packageText

            val storageFormatted = formatBytes(storageLimit)
            val additionalCostFormatted = String.format("$%.3f", additionalCost)
            val allocationText =
                if (monthlyCost == 0.0) {
                    "$storageFormatted free, then $additionalCostFormatted/GB"
                } else {
                    "$storageFormatted included, then $additionalCostFormatted/GB"
                }
            binding.tvAllocationBilling.text = allocationText

            // Calculate and show usage with plan context
            val usagePercentage =
                if (storageLimit > 0) {
                    ((accountUsage.totalUsage.bytes.toDouble() / storageLimit.toDouble()) * 100)
                        .toInt()
                        .coerceIn(0, 100)
                } else {
                    0
                }

            val used = formatBytes(accountUsage.totalUsage.bytes)
            val total = formatBytes(storageLimit)
            binding.tvUtilisation.text = "$used used"
            binding.piUsage.progress = usagePercentage
        } else {
            // No plan information available - hide plan elements and show only usage
            Timber.d("No valid plan info available, showing usage only")

            binding.tvPackage.isVisible = false
            binding.tvAllocationBilling.isVisible = false
            binding.piUsage.isVisible = false

            // Show only the human-readable usage amount
            binding.tvUtilisation.text = "Used: ${accountUsage.totalUsage.human}"
        }

        // Display spaces list if available
        if (accountUsage.spaces.isNotEmpty()) {
            binding.tvSpacesHeader.isVisible = true
            binding.llFilterButtons.isVisible = true
            binding.rvSpaces.isVisible = true
            spacesAdapter.setSpaces(accountUsage.spaces)
        } else {
            binding.tvSpacesHeader.isVisible = false
            binding.llFilterButtons.isVisible = false
            binding.rvSpaces.isVisible = false
        }
    }

    private fun setupFilterButtons() {
        var isNameSortAscending = true
        var isSizeSortAscending = false

        binding.btnSortName.setOnClickListener {
            val sortType = if (isNameSortAscending) SortType.NAME_ASC else SortType.NAME_DESC
            spacesAdapter.sortBy(sortType)
            isNameSortAscending = !isNameSortAscending

            // Update button text to show current sort direction
            binding.btnSortName.text = if (isNameSortAscending) "Name ↑" else "Name ↓"
            binding.btnSortSize.text = "Sort by Size"
        }

        binding.btnSortSize.setOnClickListener {
            val sortType = if (isSizeSortAscending) SortType.SIZE_ASC else SortType.SIZE_DESC
            spacesAdapter.sortBy(sortType)
            isSizeSortAscending = !isSizeSortAscending

            // Update button text to show current sort direction
            binding.btnSortSize.text = if (isSizeSortAscending) "Size ↑" else "Size ↓"
            binding.btnSortName.text = "Sort by Name"
        }
    }

    private fun formatBytes(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var unitIndex = 0

        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }

        return if (size == size.toLong().toDouble()) {
            "${size.toLong()}${units[unitIndex]}"
        } else {
            String.format("%.1f%s", size, units[unitIndex])
        }
    }

    // Helper class for tuple-like functionality
    private data class Tuple4<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D,
    )

    private fun performLogout() {
        dialogManager.showDialog(dialogManager.requireResourceProvider()) {
            type = DialogType.Warning
            title = UiText.StringResource(R.string.logout)
            message = UiText.StringResource(R.string.logout_confirmation)
            positiveButton {
                text = UiText.StringResource(R.string.logout)
                action = {
                    viewModel.logout(args.sessionId)
                }
            }
            neutralButton {
                text = UiText.StringResource(R.string.action_cancel)
            }
        }
    }

    override fun getToolbarTitle(): String = getString(R.string.account)
}
