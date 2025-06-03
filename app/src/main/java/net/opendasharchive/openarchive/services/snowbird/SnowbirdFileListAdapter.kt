package net.opendasharchive.openarchive.services.snowbird

import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresExtension
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.OneLineRowBinding
import net.opendasharchive.openarchive.db.SnowbirdFileItem
import net.opendasharchive.openarchive.extensions.scaled
import java.lang.ref.WeakReference

class SnowbirdFileViewHolder(val binding: OneLineRowBinding) : RecyclerView.ViewHolder(binding.root)

class SnowbirdFileListAdapter(
    onClickListener: ((SnowbirdFileItem) -> Unit)? = null,
    onLongPressListener: ((SnowbirdFileItem) -> Unit)? = null
) : ListAdapter<SnowbirdFileItem, SnowbirdFileViewHolder>(SnowbirdFileDiffCallback()) {

    private val onClickCallback = WeakReference(onClickListener)
    private val onLongPressCallback = WeakReference(onLongPressListener)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SnowbirdFileViewHolder {
        val binding = OneLineRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SnowbirdFileViewHolder(binding)
    }

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
    override fun onBindViewHolder(holder: SnowbirdFileViewHolder, position: Int) {
        val item = getItem(position)

        with (holder.binding) {
            val context = button.context

            button.setLeftIcon(ContextCompat.getDrawable(context, R.drawable.snowbird)?.scaled(40, context))
            //button.setBackgroundResource(R.drawable.button_outlined_ripple)
            button.setTitle(item.name ?: "No name provided")

            if (item.isDownloaded) {
                button.setRightIcon(ContextCompat.getDrawable(context, R.drawable.outline_cloud_done_24)?.scaled(40, context))
            } else {
                button.setRightIcon(ContextCompat.getDrawable(context, R.drawable.outline_cloud_download_24)?.scaled(40, context))
            }

            button.setOnClickListener {
                onClickCallback.get()?.invoke(item)
            }

            button.setOnLongClickListener {
                onLongPressCallback.get()?.invoke(item)
                true
            }

            if (item.size > 0) {
                // convert bytes to human-readable format
                val sizeText = when {
                    item.size >= 1_000_000_000 -> "${item.size / 1_000_000_000.0} GB"
                    item.size >= 1_000_000 -> "${item.size / 1_000_000.0} MB"
                    item.size >= 1_000 -> "${item.size / 1_000.0} KB"
                    else -> "${item.size} bytes"
                }
                button.setSubTitle(sizeText)
            }
        }
    }
}

class SnowbirdFileDiffCallback : DiffUtil.ItemCallback<SnowbirdFileItem>() {
    override fun areItemsTheSame(oldItem: SnowbirdFileItem, newItem: SnowbirdFileItem): Boolean {
        return oldItem.hash == newItem.hash
    }

    override fun areContentsTheSame(oldItem: SnowbirdFileItem, newItem: SnowbirdFileItem): Boolean {
        return oldItem == newItem
    }
}