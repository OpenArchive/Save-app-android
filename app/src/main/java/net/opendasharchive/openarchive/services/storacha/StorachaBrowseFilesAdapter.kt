package net.opendasharchive.openarchive.services.storacha

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.StorachaDidRowBinding
import net.opendasharchive.openarchive.services.storacha.model.UploadEntry

class StorachaBrowseFilesAdapter(
    private val files: List<UploadEntry> = emptyList(),
    private val onClick: (file: UploadEntry) -> Unit,
) : RecyclerView.Adapter<StorachaBrowseFilesAdapter.FileViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): FileViewHolder {
        val binding =
            StorachaDidRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FileViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(
        holder: FileViewHolder,
        position: Int,
    ) {
        holder.bind(files[position])
    }

    override fun getItemCount(): Int = files.size

    inner class FileViewHolder(
        private val binding: StorachaDidRowBinding,
        private val onClick: (file: UploadEntry) -> Unit,
    ) : RecyclerView.ViewHolder(
            binding.root,
        ) {
        fun bind(file: UploadEntry) {
            val icon = ContextCompat.getDrawable(binding.icon.context, R.drawable.ic_unknown_file)
            icon?.setTint(ContextCompat.getColor(binding.icon.context, R.color.colorOnBackground))
            binding.icon.setImageDrawable(icon)
            binding.didKey.text = file.cid
            binding.rvTick.visibility = View.GONE
            binding.root.setOnClickListener {
                onClick.invoke(file)
            }
        }
    }
}
