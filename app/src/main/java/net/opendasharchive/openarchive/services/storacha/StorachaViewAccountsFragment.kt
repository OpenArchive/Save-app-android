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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentStorachaAccountsBinding
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.services.storacha.util.SessionManager
import net.opendasharchive.openarchive.services.storacha.util.StorachaAccountManager
import net.opendasharchive.openarchive.util.extensions.toggle
import org.koin.android.ext.android.inject

class StorachaViewAccountsFragment : BaseFragment() {
//    , MenuProvider

    private lateinit var mBinding: FragmentStorachaAccountsBinding
    private val sessionManager: SessionManager by inject()

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

        ViewCompat.setOnApplyWindowInsetsListener(mBinding.rvAccountList) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())

            view.updatePadding(
                bottom = insets.bottom + view.paddingBottom
            )

            windowInsets
        }

        // Validate session before loading accounts
        validateSessionAndLoadAccounts()
//        activity?.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    /**
     * Validates the current session before loading accounts.
     * If session is invalid, removes the invalid account and navigates to login screen.
     */
    private fun validateSessionAndLoadAccounts() {
        mBinding.loadingContainer.toggle(true)

        lifecycleScope.launch {
            val isValid = sessionManager.validateSession()

            if (!isValid) {
                // Session is invalid, remove the invalid account and navigate to login
                sessionManager.removeCurrentAccount()
                mBinding.loadingContainer.toggle(false)

                // Navigate to login and clear back stack to storacha fragment
                // This prevents infinite loop when pressing back
                findNavController().navigate(
                    R.id.fragment_storacha_login,
                    null,
                    androidx.navigation.navOptions {
                        popUpTo(R.id.fragment_storacha) {
                            inclusive = false
                        }
                    }
                )
            } else {
                // Session is valid, proceed to load accounts
                loadLoggedInAccounts()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh accounts when returning from account details (in case account was logged out)
        // Also re-validate session
        validateSessionAndLoadAccounts()
    }

    private fun loadLoggedInAccounts() {
        // Check if fragment is still attached before starting
        if (!isAdded || context == null) {
            return
        }

        mBinding.loadingContainer.toggle(true)

        Handler(Looper.getMainLooper()).postDelayed({
            // Double-check fragment is still attached before accessing context
            if (!isAdded || context == null) {
                return@postDelayed
            }

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
