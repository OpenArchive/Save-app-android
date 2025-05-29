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
import net.opendasharchive.openarchive.features.folders.Folder
import net.opendasharchive.openarchive.util.extensions.toggle


class StorachaViewAccountsFragment : BaseFragment(), MenuProvider {

    private lateinit var mBinding: FragmentStorachaAccountsBinding

    private var mSelected: Folder? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = FragmentStorachaAccountsBinding.inflate(layoutInflater)
        mBinding.rvAccountList.layoutManager = LinearLayoutManager(requireContext())
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mBinding.progressBar.toggle(true)

        Handler(Looper.getMainLooper()).postDelayed({
            mBinding.projectsEmpty.toggle(false)
            mBinding.progressBar.toggle(false)
            mBinding.rvAccountList.adapter =
                StorachaBrowseAccountsAdapter(listOf(Account("prathieshna@gmail.com"),Account("upul@gmail.com"),Account("elelan@gmail.com"),Account("navoda@gmail.com"))) { account ->
                    val action = StorachaViewAccountsFragmentDirections.fragmentStorachaAccountsToFragmentStorachaAccountDetails()
                    findNavController().navigate(action)
                }
        }, 500)

        activity?.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun getToolbarTitle(): String = getString(R.string.accounts)

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_browse_folder, menu)
    }

    override fun onPrepareMenu(menu: Menu) {
        super.onPrepareMenu(menu)
        val addMenuItem = menu.findItem(R.id.action_add)
        addMenuItem?.isVisible = mSelected != null
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_add -> {
                val action = StorachaViewAccountsFragmentDirections.actionFragmentStorachaAccountsToFragmentStorachaLogin()
                findNavController().navigate(action)
                true
            }
            else -> false
        }
    }
}