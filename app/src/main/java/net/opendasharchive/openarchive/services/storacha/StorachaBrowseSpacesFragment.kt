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
import net.opendasharchive.openarchive.databinding.FragmentStorachaBrowseSpacesBinding
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.util.extensions.toggle


class StorachaBrowseSpacesFragment : BaseFragment(), MenuProvider {

    private lateinit var mBinding: FragmentStorachaBrowseSpacesBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = FragmentStorachaBrowseSpacesBinding.inflate(layoutInflater)
        mBinding.rvFolderList.layoutManager = LinearLayoutManager(requireContext())
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mBinding.progressBar.toggle(true)

        Handler(Looper.getMainLooper()).postDelayed({
            mBinding.projectsEmpty.toggle(false)
            mBinding.progressBar.toggle(false)
            mBinding.rvFolderList.adapter =
                StorachaBrowseSpacesAdapter(listOf(Space("Trip"),Space("Birthday Party"),Space("Wedding"),Space("Conference"))) { account ->
                    val action = StorachaBrowseSpacesFragmentDirections.actionFragmentStorachaBrowseSpacesToFragmentStorachaMedia()
                    findNavController().navigate(action)
                }
        }, 500)

        activity?.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun getToolbarTitle(): String = getString(R.string.spaces)

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_browse_folder, menu)
    }

    override fun onPrepareMenu(menu: Menu) {
        super.onPrepareMenu(menu)
        val addMenuItem = menu.findItem(R.id.action_add)
        addMenuItem?.isVisible = false
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_add -> {
                true
            }
            else -> false
        }
    }
}