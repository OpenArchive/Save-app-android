package net.opendasharchive.openarchive.features.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import net.opendasharchive.openarchive.FolderAdapter
import net.opendasharchive.openarchive.FolderAdapterListener
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentFolderListBinding
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.features.core.NavArgument
import net.opendasharchive.openarchive.util.extensions.toggle

class  FolderListFragment : BaseFragment(), FolderAdapterListener {

    private lateinit var binding: FragmentFolderListBinding
    private lateinit var adapter: FolderAdapter

    private var showArchived = true
    private var selectedSpaceId = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            showArchived = it.getBoolean(NavArgument.SHOW_ARCHIVED_FOLDERS, false)
            selectedSpaceId = it.getLong(NavArgument.SPACE_ID, -1L)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFolderListBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        adapter = FolderAdapter(context = requireContext(), listener = this, isArchived = showArchived)
        binding.rvProjects.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProjects.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        refreshProjects()
    }

    private fun refreshProjects() {
        val projects = if (showArchived) {
            Space.current?.archivedProjects
        } else {
            Space.current?.projects?.filter { !it.isArchived }
        } ?: emptyList()

        adapter.update(projects)

        if (projects.isEmpty()) {
            binding.rvProjects.visibility = View.GONE
            binding.tvNoFolders.visibility = View.VISIBLE
        } else {
            binding.rvProjects.visibility = View.VISIBLE
            binding.tvNoFolders.visibility = View.GONE
        }
    }


    override fun projectClicked(project: Project) {
        val action = FolderListFragmentDirections.actionFragmentFolderListToFragmentEditFolder(folderId = project.id)
        findNavController().navigate(action)
    }

    override fun getToolbarTitle(): String = getString(R.string.archived_folders)
}