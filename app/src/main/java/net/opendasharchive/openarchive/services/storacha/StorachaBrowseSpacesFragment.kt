package net.opendasharchive.openarchive.services.storacha

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentStorachaBrowseSpacesBinding
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.services.storacha.util.DidManager
import net.opendasharchive.openarchive.services.storacha.util.StorachaAccountManager
import net.opendasharchive.openarchive.services.storacha.util.StorachaHelper
import net.opendasharchive.openarchive.services.storacha.viewModel.StorachaBrowseSpacesViewModel
import net.opendasharchive.openarchive.util.extensions.toggle
import org.koin.androidx.viewmodel.ext.android.viewModel

class StorachaBrowseSpacesFragment : BaseFragment() {
    private lateinit var mBinding: FragmentStorachaBrowseSpacesBinding
    private val viewModel: StorachaBrowseSpacesViewModel by viewModel()

    // Track if we're doing a pull-to-refresh to avoid dual loading indicators
    private var isPullToRefresh = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        mBinding = FragmentStorachaBrowseSpacesBinding.inflate(layoutInflater)
        mBinding.rvFolderList.layoutManager = LinearLayoutManager(requireContext())

        // Setup swipe refresh
        mBinding.swipeRefreshLayout.setOnRefreshListener {
            isPullToRefresh = true
            refreshSpaces()
        }

        return mBinding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        val did = DidManager(requireContext()).getOrCreateDid()
        val accountManager = StorachaAccountManager(requireContext())
        val currentAccount = accountManager.getCurrentAccount()
        val sessionId = currentAccount?.sessionId ?: ""
        viewModel.loadSpaces(did, sessionId)

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            // Only show center loading if it's not a pull-to-refresh
            mBinding.loadingContainer.toggle(isLoading && !isPullToRefresh)

            // Hide swipe refresh when loading is complete
            if (!isLoading) {
                mBinding.swipeRefreshLayout.isRefreshing = false
                isPullToRefresh = false // Reset the flag
            }
        }

        viewModel.spaces.observe(viewLifecycleOwner) { list ->
            mBinding.projectsEmpty.toggle(list.isEmpty())

            // Store space count in SharedPreferences for access checks
            // Only count spaces where user is not admin (joined spaces, not owned)
            val joinedSpaceCount = list.count { !it.isAdmin }
            StorachaHelper.updateSpaceCount(requireContext(), joinedSpaceCount)

            mBinding.rvFolderList.adapter =
                StorachaBrowseSpacesAdapter(list) { space ->
                    val action =
                        StorachaBrowseSpacesFragmentDirections.actionFragmentStorachaBrowseSpacesToFragmentStorachaMedia(
                            spaceId = space.did,
                            spaceName = space.name,
                            sessionId = if (space.isAdmin) sessionId else "",
                            isAdmin = space.isAdmin,
                        )
                    findNavController().navigate(action)
                }
        }
    }

    private fun refreshSpaces() {
        val did = DidManager(requireContext()).getOrCreateDid()
        val accountManager = StorachaAccountManager(requireContext())
        val currentAccount = accountManager.getCurrentAccount()
        val sessionId = currentAccount?.sessionId ?: ""
        viewModel.loadSpaces(did, sessionId)
    }

    override fun getToolbarTitle(): String = getString(R.string.spaces)
}
