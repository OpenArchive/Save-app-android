package net.opendasharchive.openarchive

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.opendasharchive.openarchive.databinding.RvSimpleRowBinding
import net.opendasharchive.openarchive.db.Project
import java.lang.ref.WeakReference

interface FolderAdapterListener {

    fun projectClicked(project: Project)

    fun getSelectedProject(): Project?
}

class FolderAdapter(listener: FolderAdapterListener?) : ListAdapter<Project, FolderAdapter.ViewHolder>(DIFF_CALLBACK), FolderAdapterListener {

    class ViewHolder(private val binding: RvSimpleRowBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(listener: WeakReference<FolderAdapterListener>?, project: Project?) {
            binding.rvTitle.text = project?.description

            if (listener?.get()?.getSelectedProject()?.id == project?.id) {
                val icon = ContextCompat.getDrawable(binding.rvIcon.context, R.drawable.baseline_folder_white_24)
                val color = ContextCompat.getColor(binding.rvIcon.context, R.color.colorPrimary)
                icon?.setTint(color)
                binding.rvIcon.setImageDrawable(icon)
            } else {
                val icon = ContextCompat.getDrawable(binding.rvIcon.context, R.drawable.outline_folder_white_24)
                val color = ContextCompat.getColor(binding.rvIcon.context, R.color.colorOnBackground)
                icon?.setTint(color)
                binding.rvIcon.setImageDrawable(icon)
            }

            binding.rvTitle.setTextColor(getColor(binding.rvTitle.context,
                listener?.get()?.getSelectedProject()?.id == project?.id))

            if (project != null) {
                binding.root.setOnClickListener {
                    listener?.get()?.projectClicked(project)
                }
            }
            else {
                binding.root.setOnClickListener(null)
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

        private var highlightColor: Int? = null
        private var defaultColor: Int? = null

        fun getColor(context: Context, highlight: Boolean): Int {
            if (highlight) {
                var color = highlightColor

                if (color != null) return color

                color = ContextCompat.getColor(context, R.color.colorPrimary)
                highlightColor = color

                return color
            }

            var color = defaultColor

            if (color != null) return color

            val textview = TextView(context)
            color = textview.currentTextColor
            defaultColor = color

            return color
        }
    }

    private val mListener: WeakReference<FolderAdapterListener>?

    private var mLastSelected: Project? = null

    init {
        mListener = WeakReference(listener)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(RvSimpleRowBinding.inflate(LayoutInflater.from(parent.context),
            parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val project = getItem(position)

        holder.bind(WeakReference(this), project)
    }

    fun update(projects: List<Project>) {
        notifyItemChanged(getIndex(mLastSelected))

        submitList(projects)
    }

    override fun projectClicked(project: Project) {
        notifyItemChanged(getIndex(getSelectedProject()))
        notifyItemChanged(getIndex(project))

        mListener?.get()?.projectClicked(project)
    }

    override fun getSelectedProject(): Project? {
        mLastSelected = mListener?.get()?.getSelectedProject()

        return mLastSelected
    }

    private fun getIndex(project: Project?): Int {
        return if (project == null) {
            -1
        }
        else {
            currentList.indexOf(project)
        }
    }
}