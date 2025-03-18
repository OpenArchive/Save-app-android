package net.opendasharchive.openarchive.features.folders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FolderRowBinding
import java.text.SimpleDateFormat

class BrowseFoldersAdapter(
    private val folders: List<Folder> = emptyList(),
    private val onClick: (folder: Folder) -> Unit
) : RecyclerView.Adapter<BrowseFoldersAdapter.FolderViewHolder>() {

    companion object {
        private val formatter = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.LONG, SimpleDateFormat.MEDIUM)
    }

    private var mSelected: Folder? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val binding = FolderRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return FolderViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        holder.bind(folders[position])
    }

    override fun getItemCount(): Int = folders.size

    inner class FolderViewHolder(private val binding: FolderRowBinding, private val onClick: (folder: Folder) -> Unit) : RecyclerView.ViewHolder(binding.root) {

        fun bind(folder: Folder) {

            val isSelected = mSelected == folder

            itemView.isSelected = isSelected


            val icon = ContextCompat.getDrawable(binding.icon.context, R.drawable.ic_folder_new)
            icon?.setTint(ContextCompat.getColor(binding.icon.context, R.color.colorOnBackground))
            binding.icon.setImageDrawable(icon)

            binding.name.text = folder.name
            binding.timestamp.text = formatter.format(folder.modified)

            binding.rvTick.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE

            binding.root.setOnClickListener {
                if (mSelected == folder) return@setOnClickListener

                val previousSelected = mSelected
                mSelected = folder

                // Notify changes for previous and current selection
                notifyItemChanged(folders.indexOf(previousSelected))
                notifyItemChanged(folders.indexOf(mSelected))

                onClick.invoke(folder)
            }
        }
    }
}