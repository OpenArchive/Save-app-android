package net.opendasharchive.openarchive.services.snowbird

import android.content.res.ColorStateList
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.RequiresExtension
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.SnowbirdMediaGridItemBinding
import net.opendasharchive.openarchive.services.snowbird.service.db.SnowbirdFileItem
import java.lang.ref.WeakReference

class SnowbirdFileViewHolder(val binding: SnowbirdMediaGridItemBinding) : RecyclerView.ViewHolder(binding.root)

class SnowbirdFileListAdapter(
    onClickListener: ((SnowbirdFileItem) -> Unit)? = null,
    onLongPressListener: ((SnowbirdFileItem) -> Unit)? = null
) : ListAdapter<SnowbirdFileItem, SnowbirdFileViewHolder>(SnowbirdFileDiffCallback()) {

    private val onClickCallback = WeakReference(onClickListener)
    private val onLongPressCallback = WeakReference(onLongPressListener)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SnowbirdFileViewHolder {
        val binding = SnowbirdMediaGridItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SnowbirdFileViewHolder(binding)
    }

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
    override fun onBindViewHolder(holder: SnowbirdFileViewHolder, position: Int) {
        val item = getItem(position)

        with (holder.binding) {
            val context = root.context

            // Set filename
            name.text = item.name ?: "No name provided"

            // Determine file type and show appropriate icon
            val fileExtension = item.name?.substringAfterLast(".", "")?.lowercase() ?: ""

            when {
                isImageFile(fileExtension) -> setDefaultIcon(R.drawable.ic_image)
                isVideoFile(fileExtension) -> setDefaultIcon(R.drawable.ic_video)
                isAudioFile(fileExtension) -> setDefaultIcon(R.drawable.ic_music)
                else -> setDefaultIcon(R.drawable.ic_folder_new)
            }

            // Show download badge if not downloaded
            downloadBadge.visibility = if (item.isDownloaded) View.GONE else View.VISIBLE

            root.setOnClickListener {
                onClickCallback.get()?.invoke(item)
            }

            root.setOnLongClickListener {
                onLongPressCallback.get()?.invoke(item)
                true
            }
        }
    }

    private fun SnowbirdMediaGridItemBinding.setDefaultIcon(iconRes: Int) {
        icon.scaleType = ImageView.ScaleType.CENTER_INSIDE
        icon.setImageDrawable(ContextCompat.getDrawable(root.context, iconRes)?.mutate())
        icon.imageTintList = ColorStateList.valueOf(
            ContextCompat.getColor(root.context, R.color.colorOnBackground)
        )
    }

    private fun isImageFile(extension: String): Boolean {
        return extension in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "heif")
    }

    private fun isVideoFile(extension: String): Boolean {
        return extension in listOf("mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "3gp")
    }

    private fun isAudioFile(extension: String): Boolean {
        return extension in listOf("mp3", "wav", "ogg", "m4a", "flac", "aac", "wma")
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