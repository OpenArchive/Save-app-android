package net.opendasharchive.openarchive.features.settings

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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import net.opendasharchive.openarchive.FolderAdapter
import net.opendasharchive.openarchive.FolderAdapterListener
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentFoldersBinding
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.util.extensions.toggle

class FoldersFragment : BaseFragment(), FolderAdapterListener, MenuProvider {

    companion object Companion {
        const val EXTRA_SHOW_ARCHIVED = "show_archived"
        const val EXTRA_SELECTED_SPACE_ID = "selected_space_id"
        const val EXTRA_SELECTED_PROJECT_ID = "SELECTED_PROJECT_ID"
    }

    private lateinit var mBinding: FragmentFoldersBinding
    private lateinit var mAdapter: FolderAdapter

    private var mArchived = true
    private var mSelectedSpaceId = -1L
    private var mSelectedProjectId: Long = -1L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = FragmentFoldersBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get arguments from Navigation component
        mArchived = arguments?.getBoolean("show_archived", false) ?: false
        mSelectedSpaceId = arguments?.getLong("selected_space_id", -1L) ?: -1L
        mSelectedProjectId = arguments?.getLong("selected_project_id", -1L) ?: -1L

        activity?.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        setupRecyclerView()
        setupButtons()
    }

    private fun setupRecyclerView() {
        mAdapter = FolderAdapter(context = requireContext(), listener = this, isArchived = mArchived)
        mBinding.rvProjects.layoutManager = LinearLayoutManager(requireContext())
        mBinding.rvProjects.adapter = mAdapter
    }

    private fun setupButtons() {
        mBinding.btViewArchived.apply {
            toggle(!mArchived)
            setOnClickListener {
                // Navigation logic should be handled by parent activity/fragment
                // For now, we'll keep the intent approach but this should be replaced with proper navigation
                val i = Intent(requireContext(), FoldersFragment::class.java)
                i.putExtra(EXTRA_SHOW_ARCHIVED, true)
                startActivity(i)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshProjects()
        activity?.invalidateOptionsMenu()
    }

    private fun refreshProjects() {
        val projects = if (mArchived) {
            Space.current?.archivedProjects
        } else {
            Space.current?.projects?.filter { !it.isArchived }
        } ?: emptyList()

        mAdapter.update(projects)

        if (projects.isEmpty()) {
            mBinding.rvProjects.visibility = View.GONE
            mBinding.tvNoFolders.visibility = View.VISIBLE
        } else {
            mBinding.rvProjects.visibility = View.VISIBLE
            mBinding.tvNoFolders.visibility = View.GONE
        }
    }


    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_folder_list, menu)
    }

    override fun onPrepareMenu(menu: Menu) {
        val archivedCount = Space.get(mSelectedSpaceId)?.archivedProjects?.size ?: 0
        menu.findItem(R.id.action_archived_folders)?.isVisible = (!mArchived && archivedCount > 0)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_archived_folders -> {
                navigateToArchivedFolders()
                true
            }
            else -> false
        }
    }

    private fun navigateToArchivedFolders() {
        val intent = Intent(requireContext(), FoldersFragment::class.java).apply {
            putExtra(EXTRA_SHOW_ARCHIVED, true)
            putExtra(EXTRA_SELECTED_SPACE_ID, mSelectedSpaceId)
            putExtra(EXTRA_SELECTED_PROJECT_ID, mSelectedProjectId)
        }
        startActivity(intent)
    }


    override fun projectClicked(project: Project) {
        val resultIntent = Intent()
        resultIntent.putExtra("SELECTED_FOLDER_ID", project.id)
        requireActivity().setResult(android.app.Activity.RESULT_OK, resultIntent)
        
        // Navigate using Navigation Component with Safe Args
        val action = FoldersFragmentDirections.actionFragmentFoldersToFragmentFolderDetail(currentProjectId = project.id)
        findNavController().navigate(action)
    }

    override fun getToolbarTitle(): String = getString(if (mArchived) R.string.archived_folders else R.string.folders)
    override fun shouldShowBackButton() = true
}