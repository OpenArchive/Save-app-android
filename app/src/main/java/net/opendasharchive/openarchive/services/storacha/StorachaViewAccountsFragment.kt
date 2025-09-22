package net.opendasharchive.openarchive.services.storacha

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentStorachaAccountsBinding
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.services.storacha.util.StorachaAccountManager
import net.opendasharchive.openarchive.util.extensions.toggle

class StorachaViewAccountsFragment : BaseFragment() {
//    , MenuProvider

    private lateinit var mBinding: FragmentStorachaAccountsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        mBinding = FragmentStorachaAccountsBinding.inflate(layoutInflater)
        mBinding.rvAccountList.layoutManager = LinearLayoutManager(requireContext())
        return mBinding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        loadLoggedInAccounts()
//        activity?.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onResume() {
        super.onResume()
        // Refresh accounts when returning from account details (in case account was logged out)
        loadLoggedInAccounts()
    }

    private fun loadLoggedInAccounts() {
        mBinding.loadingContainer.toggle(true)

        Handler(Looper.getMainLooper()).postDelayed({
            val accountManager = StorachaAccountManager(requireContext())
            val loggedInAccounts = accountManager.getLoggedInAccounts()

            if (loggedInAccounts.isEmpty()) {
                mBinding.projectsEmpty.toggle(true)
                mBinding.rvAccountList.adapter = null
            } else {
                mBinding.projectsEmpty.toggle(false)
                mBinding.rvAccountList.adapter =
                    StorachaBrowseAccountsAdapter(
                        loggedInAccounts.map { Account(it.email, it.sessionId) },
                        false,
                    ) { account ->
                        val action =
                            StorachaViewAccountsFragmentDirections.fragmentStorachaAccountsToFragmentStorachaAccountDetails(
                                email = account.email,
                                sessionId = account.sessionId,
                            )
                        findNavController().navigate(action)
                    }
            }
            mBinding.loadingContainer.toggle(false)
        }, 500)
    }

    override fun getToolbarTitle(): String = getString(R.string.accounts)

//    override fun onCreateMenu(
//        menu: Menu,
//        menuInflater: MenuInflater,
//    ) {
//        menuInflater.inflate(R.menu.menu_browse_folder, menu)
//    }
//
//    override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
//        when (menuItem.itemId) {
//            R.id.action_add -> {
//                val action =
//                    StorachaViewAccountsFragmentDirections.actionFragmentStorachaAccountsToFragmentStorachaLogin()
//                findNavController().navigate(action)
//                true
//            }
//
//            else -> false
//        }
}
