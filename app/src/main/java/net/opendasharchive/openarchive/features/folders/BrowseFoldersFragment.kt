package net.opendasharchive.openarchive.features.folders

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentBrowseFoldersBinding
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.features.core.dialog.showSuccessDialog
import net.opendasharchive.openarchive.util.extensions.toggle
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Date


class BrowseFoldersFragment : BaseFragment(), MenuProvider {

    private lateinit var mBinding: FragmentBrowseFoldersBinding
    private val mViewModel: BrowseFoldersViewModel by viewModel()

    private var mSelected: Folder? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = FragmentBrowseFoldersBinding.inflate(layoutInflater)

        mBinding.rvFolderList.layoutManager = LinearLayoutManager(requireContext())

        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        val space = Space.current
        if (space != null) mViewModel.getFiles(space)

        mViewModel.folders.observe(viewLifecycleOwner) {
            mBinding.projectsEmpty.toggle(it.isEmpty())

            mBinding.rvFolderList.adapter = BrowseFoldersAdapter(it) { folder ->
                this.mSelected = folder
                activity?.invalidateOptionsMenu()
            }
        }

        mViewModel.progressBarFlag.observe(viewLifecycleOwner) {
            mBinding.progressBar.toggle(it)
        }
    }


    override fun getToolbarTitle(): String = getString(R.string.browse_existing)

    private fun addFolder(folder: Folder?) {
        if (folder == null) return
        val space = Space.current ?: return

        // This should not happen. These should have been filtered on display.
        if (space.hasProject(folder.name)) return

        val license = space.license


        val project = Project(folder.name, Date(), space.id, licenseUrl = license)
        project.save()

        showFolderCreated(project.id)
    }

    private fun showFolderCreated(projectId: Long) {

        dialogManager.showSuccessDialog(
            title = R.string.label_success_title,
            message = R.string.create_folder_ok_message,
            positiveButtonText = R.string.label_got_it,
            onDone = {
                navigateBackWithResult(projectId)
            }
        )
    }

    private fun navigateBackWithResult(projectId: Long) {
        requireActivity().setResult(RESULT_OK, Intent().apply {
            putExtra(AddFolderActivity.EXTRA_FOLDER_ID, projectId)
        })
        requireActivity().finish()
    }

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
                addFolder(mSelected)
                true
            }

            else -> false
        }
    }
}