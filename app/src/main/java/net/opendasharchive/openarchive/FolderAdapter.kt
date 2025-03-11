package net.opendasharchive.openarchive

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.opendasharchive.openarchive.databinding.RvFoldersRowBinding
import net.opendasharchive.openarchive.db.Project
import java.lang.ref.WeakReference

interface FolderAdapterListener {

    fun projectClicked(project: Project)

}

class FolderAdapter(private val context: Context, private val listener: FolderAdapterListener, private val isArchived: Boolean = false) : ListAdapter<Project, FolderAdapter.FolderViewHolder>(DIFF_CALLBACK) {

    inner class FolderViewHolder(private val binding: RvFoldersRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(project: Project) {

            binding.rvTitle.text = project.description

            val icon = ContextCompat.getDrawable(context, R.drawable.ic_folder_new)

            binding.rvIcon.setImageDrawable(icon)

            itemView.setOnClickListener {
                    listener.projectClicked(project)
            }
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
        return FolderViewHolder(
            RvFoldersRowBinding.inflate(
                LayoutInflater.from(parent.context),
                parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        val project = getItem(position)

        holder.bind( project)
    }

    fun update(projects: List<Project>) {

        submitList(projects)
    }
}