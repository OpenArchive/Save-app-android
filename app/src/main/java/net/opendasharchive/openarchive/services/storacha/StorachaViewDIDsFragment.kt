package net.opendasharchive.openarchive.services.storacha

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentStorachaViewDidsBinding
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.features.core.UiImage
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.dialog.DialogType
import net.opendasharchive.openarchive.features.core.dialog.showDialog
import net.opendasharchive.openarchive.services.storacha.viewModel.DidAccount
import net.opendasharchive.openarchive.services.storacha.viewModel.StorachaViewDIDsViewModel
import net.opendasharchive.openarchive.util.extensions.toggle
import org.koin.androidx.viewmodel.ext.android.viewModel

class StorachaViewDIDsFragment :
    BaseFragment(),
    MenuProvider {
    private lateinit var mBinding: FragmentStorachaViewDidsBinding
    private val viewModel: StorachaViewDIDsViewModel by viewModel()
    private val args: StorachaViewDIDsFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        mBinding = FragmentStorachaViewDidsBinding.inflate(layoutInflater)
        mBinding.rvFolderList.layoutManager = LinearLayoutManager(requireContext())
        return mBinding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        viewModel.loadDIDs(args.sessionId, args.spaceId)
        activity?.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupObservers() {
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            mBinding.progressBar.toggle(isLoading)
        }

        viewModel.dids.observe(viewLifecycleOwner) { dids ->
            mBinding.projectsEmpty.toggle(dids.isEmpty())
            // Convert DidAccount to Account for the adapter
            val accounts =
                dids.map { Account(it.did, "") } // Empty sessionId since DIDs don't need it
            mBinding.rvFolderList.adapter =
                StorachaBrowseAccountsAdapter(
                    accounts,
                    true,
                ) { account ->
                    // Convert back to DidAccount for the dialog
                    val didAccount =
                        dids.find { it.did == account.email }
                            ?: return@StorachaBrowseAccountsAdapter
                    showRevokeDialog(didAccount)
                }
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                // TODO: Show error dialog or snackbar
                // For now, you can add proper error handling here
            }
        }
    }

    private fun showRevokeDialog(account: DidAccount) {
        dialogManager.showDialog(dialogManager.requireResourceProvider()) {
            type = DialogType.Error
            icon = UiImage.DrawableResource(R.drawable.ic_trash)
            title = UiText.StringResource(R.string.revoke_access)
            message = UiText.StringResource(R.string.revoke_access_prompt)
            destructiveButton {
                text = UiText.StringResource(R.string.revoke)
                action = {
                    viewModel.revokeDID(args.sessionId, args.spaceId, account)
                    dialogManager.dismissDialog()
                }
            }
            neutralButton {
                text = UiText.StringResource(R.string.lbl_Cancel)
                action = {
                    dialogManager.dismissDialog()
                }
            }
        }
    }

    override fun getToolbarTitle(): String = "Manage DIDs"

    override fun onCreateMenu(
        menu: Menu,
        menuInflater: MenuInflater,
    ) {
        menuInflater.inflate(R.menu.menu_browse_folder, menu)
    }

    override fun onPrepareMenu(menu: Menu) {
        super.onPrepareMenu(menu)
        val addMenuItem = menu.findItem(R.id.action_add)
        addMenuItem?.isVisible = true
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
        when (menuItem.itemId) {
            R.id.action_add -> {
                val action =
                    StorachaViewDIDsFragmentDirections.actionFragmentStorachaViewDidsToFragmentStorachaDidAccess(
                        args.spaceId,
                        args.sessionId,
                    )
                findNavController().navigate(action)
                true
            }

            else -> false
        }
}
