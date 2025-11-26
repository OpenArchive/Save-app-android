package net.opendasharchive.openarchive.features.main.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.RvDrawerRowBinding
import net.opendasharchive.openarchive.db.Project


interface FolderDrawerAdapterListener {
    fun onProjectSelected(project: Project)
    fun getSelectedProject(): Project?
}

class FolderDrawerAdapter(
    private val listener: FolderDrawerAdapterListener
) : ListAdapter<Project, FolderDrawerAdapter.FolderViewHolder>(DIFF_CALLBACK) {

    private var selectedProject: Project? = listener.getSelectedProject()

    inner class FolderViewHolder(
        private val binding: RvDrawerRowBinding,
        private val listener: FolderDrawerAdapterListener
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(project: Project) {

            binding.rvTitle.text = project.description

            val isSelected = project.id == selectedProject?.id
            val iconRes = if (isSelected) R.drawable.baseline_folder_white_24 else R.drawable.outline_folder_white_24
            val iconColor = if (isSelected) R.color.colorTertiary else R.color.colorOnBackground
            val textColor = if (isSelected) R.color.colorOnBackground else R.color.colorText

            val icon = ContextCompat.getDrawable(binding.rvIcon.context, iconRes)
            icon?.setTint(ContextCompat.getColor(binding.rvIcon.context, iconColor))
            binding.rvIcon.setImageDrawable(icon)

            binding.rvTitle.setTextColor(ContextCompat.getColor(binding.rvTitle.context, textColor))

            binding.root.setOnClickListener {
                onItemSelected(project)
            }
        }

        private fun onItemSelected(project: Project) {
            val previousIndex = currentList.indexOf(selectedProject)
            val newIndex = currentList.indexOf(project)

            selectedProject = project

            if (previousIndex != -1) notifyItemChanged(previousIndex)
            if (newIndex != -1) notifyItemChanged(newIndex)

            listener.onProjectSelected(project)
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Project>() {
            override fun areItemsTheSame(oldItem: Project, newItem: Project): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Project, newItem: Project): Boolean {
                return oldItem.description == newItem.description
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val binding = RvDrawerRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FolderViewHolder(binding, listener = listener)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        val project = getItem(position)

        holder.bind(project)
    }

    fun update(projects: List<Project>) {
        // Preserve selection if the selected project is still present
        val previouslySelectedId = selectedProject?.id
        selectedProject = projects.find { it.id == previouslySelectedId }

        submitList(projects)
    }

    fun updateSelectedProject(project: Project?) {
        val previousIndex = currentList.indexOf(selectedProject)
        val newIndex = currentList.indexOf(project)

        selectedProject = project

        if (previousIndex != -1) notifyItemChanged(previousIndex)
        if (newIndex != -1) notifyItemChanged(newIndex)
    }
}