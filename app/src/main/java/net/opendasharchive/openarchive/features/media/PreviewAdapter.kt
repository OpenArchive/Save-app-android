package net.opendasharchive.openarchive.features.media

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import net.opendasharchive.openarchive.databinding.RvMediaBoxBinding
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.features.media.adapter.PreviewViewHolder
import java.lang.ref.WeakReference

class PreviewAdapter(listener: Listener? = null): ListAdapter<Media, PreviewViewHolder>(DIFF_CALLBACK) {

    interface Listener {

        fun mediaClicked(media: Media)

        fun mediaSelectionChanged()
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Media>() {
            override fun areItemsTheSame(oldItem: Media, newItem: Media): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Media, newItem: Media): Boolean {
                return oldItem.originalFilePath == newItem.originalFilePath
                        && oldItem.mimeType == newItem.mimeType
                        && oldItem.createDate == newItem.createDate
                        && oldItem.updateDate == newItem.updateDate
                        && oldItem.uploadDate == newItem.uploadDate
                        && oldItem.serverUrl == newItem.serverUrl
                        && oldItem.title == newItem.title
                        && oldItem.description == newItem.description
                        && oldItem.author == newItem.author
                        && oldItem.location == newItem.location
                        && oldItem.tags == newItem.tags
                        && oldItem.licenseUrl == newItem.licenseUrl
                        && oldItem.mediaHash.contentEquals(newItem.mediaHash)
                        && oldItem.mediaHashString == newItem.mediaHashString
                        && oldItem.status == newItem.status
                        && oldItem.statusMessage == newItem.statusMessage
                        && oldItem.projectId == newItem.projectId
                        && oldItem.collectionId == newItem.collectionId
                        && oldItem.contentLength == newItem.contentLength
                        && oldItem.progress == newItem.progress
                        && oldItem.flag == newItem.flag
                        && oldItem.priority == newItem.priority
                        && oldItem.selected == newItem.selected
            }
        }
    }

    private val mListener = WeakReference(listener)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreviewViewHolder {
        val binding = RvMediaBoxBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val mvh = PreviewViewHolder(binding)

        mvh.itemView.setOnClickListener { view ->
            val media = getMedia(view) ?: return@setOnClickListener

            if (currentList.firstOrNull { it.selected } != null) {
                toggleSelection(media)

                return@setOnClickListener
            }

            mListener.get()?.mediaClicked(media)
        }

        mvh.itemView.setOnLongClickListener { view ->
            val media = getMedia(view) ?: return@setOnLongClickListener false

            toggleSelection(media)

            return@setOnLongClickListener true
        }

        return  mvh
    }

    override fun onBindViewHolder(holder: PreviewViewHolder, position: Int) {
        holder.bind(getItem(position), batchMode = true, doImageFade = false)
    }

    private fun getMedia(view: View): Media? {
        val id = view.tag as? Long ?: return null

        return currentList.firstOrNull { it.id == id }
    }

    private fun toggleSelection(media: Media) {
        media.selected = !media.selected

        notifyItemChanged(currentList.indexOf(media))

        mListener.get()?.mediaSelectionChanged()
    }
}