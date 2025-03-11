package net.opendasharchive.openarchive.features.settings

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import net.opendasharchive.openarchive.FolderAdapter
import net.opendasharchive.openarchive.FolderAdapterListener
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityFoldersBinding
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.features.folders.AddFolderActivity
import net.opendasharchive.openarchive.util.extensions.toggle

class FoldersActivity : BaseActivity(), FolderAdapterListener {

    companion object {
        const val EXTRA_SHOW_ARCHIVED = "show_archived"
        const val EXTRA_SELECTED_SPACE_ID = "selected_space_id"
        const val EXTRA_SELECTED_PROJECT_ID = "SELECTED_PROJECT_ID"
    }

    private lateinit var mBinding: ActivityFoldersBinding
    private lateinit var mAdapter: FolderAdapter

    private var mArchived = false
    private var mSelectedSpaceId = -1L
    private var mSelectedProjectId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mArchived = intent.getBooleanExtra(EXTRA_SHOW_ARCHIVED, false)
        mSelectedSpaceId = intent.getLongExtra(EXTRA_SELECTED_SPACE_ID, -1L)
        mSelectedProjectId = intent.getLongExtra(EXTRA_SELECTED_PROJECT_ID, -1L)

        mBinding = ActivityFoldersBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setupToolbar(
            title = getString(if (mArchived) R.string.archived_folders else R.string.folders),
            showBackButton = true
        )

        setupRecyclerView()

        setupButtons()
    }

    private fun setupRecyclerView() {
        mAdapter = FolderAdapter(context = this, listener = this, isArchived = mArchived)
        mBinding.rvProjects.layoutManager = LinearLayoutManager(this)
        mBinding.rvProjects.adapter = mAdapter
    }

    private fun setupButtons() {
//        mBinding.fabAdd.apply {
//            visibility = if (mArchived) View.INVISIBLE else View.VISIBLE
//            setOnClickListener { addFolder() }
//        }

        mBinding.btViewArchived.apply {
            toggle(!mArchived)
            setOnClickListener {
                val i = Intent(this@FoldersActivity, FoldersActivity::class.java)
                i.putExtra(EXTRA_SHOW_ARCHIVED, true)
                startActivity(i)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshProjects()
        invalidateOptionsMenu()
    }

    private fun refreshProjects() {
        val projects = if (mArchived) {
            Space.current?.archivedProjects
        } else {
            Space.current?.projects?.filter { !it.isArchived }
        } ?: emptyList()

        mAdapter.update(projects)
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_folder_list, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val archivedCount = Space.get(mSelectedSpaceId)?.archivedProjects?.size ?: 0
        menu?.findItem(R.id.action_archived_folders)?.isVisible = (!mArchived && archivedCount > 0)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {

            R.id.action_archived_folders -> {
                navigateToArchivedFolders()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun navigateToArchivedFolders() {
        val intent = Intent(this, FoldersActivity::class.java).apply {
            putExtra(EXTRA_SHOW_ARCHIVED, true)
            putExtra(EXTRA_SELECTED_SPACE_ID, mSelectedSpaceId)
            putExtra(EXTRA_SELECTED_PROJECT_ID, mSelectedProjectId)
        }
        startActivity(intent)
    }


    override fun projectClicked(project: Project) {
        val resultIntent = Intent()
        resultIntent.putExtra("SELECTED_FOLDER_ID", project.id)
        setResult(RESULT_OK, resultIntent)
        finish() // Close FoldersActivity and return to MainActivity

        val intent = Intent(this, EditFolderActivity::class.java).apply {
            putExtra(EditFolderActivity.EXTRA_CURRENT_PROJECT_ID, project.id)
        }
        startActivity(intent)
    }
}