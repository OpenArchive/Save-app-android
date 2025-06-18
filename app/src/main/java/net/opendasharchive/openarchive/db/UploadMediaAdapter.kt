package net.opendasharchive.openarchive.db

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.databinding.RvMediaRowSmallBinding
import net.opendasharchive.openarchive.upload.BroadcastManager
import java.lang.ref.WeakReference

class UploadMediaAdapter(
    activity: Activity?,
    mediaItems: List<Media>,
    private val recyclerView: RecyclerView,
    private val checkSelecting: (() -> Unit)? = null,
    private val onDeleteClick: (Media, Int) -> Unit,
) : RecyclerView.Adapter<UploadMediaViewHolder>() {

    var media: ArrayList<Media> = ArrayList(mediaItems)
        private set

    var doImageFade = true

    private var mActivity = WeakReference(activity)

    init {
        setHasStableIds(true)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UploadMediaViewHolder {
        val binding =
            RvMediaRowSmallBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val mvh = UploadMediaViewHolder(
            binding = binding,
            onDeleteClick = { position ->
                deleteItem(position)
            }
        )

        mvh.itemView.setOnClickListener { v ->
            val position = recyclerView.getChildLayoutPosition(v)
            val item = media[position]

            if (item.sStatus == Media.Status.Error) {
                onDeleteClick.invoke(item, position)
            } else {
                if (checkSelecting != null) {
                    selectView(v)
                }
            }
        }

        if (checkSelecting != null) {
            mvh.itemView.setOnLongClickListener { v ->
                selectView(v)

                true
            }
        }

        return mvh
    }

    override fun getItemCount(): Int = media.size

    override fun getItemId(position: Int): Long {
        return media[position].id
    }

    override fun onBindViewHolder(holder: UploadMediaViewHolder, position: Int) {
        AppLogger.i("onBindViewHolder called for position $position")
        holder.bind(media[position], doImageFade)
    }

    override fun onBindViewHolder(
        holder: UploadMediaViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty()) {
            val payload = payloads[0]
            when (payload) {
                "progress" -> {

                }

                "full" -> {
                    holder.bind(media[position], doImageFade)
                }
            }
        } else {
            holder.bind(media[position], doImageFade)
        }
    }

    fun updateItem(mediaId: Long, progress: Int, isUploaded: Boolean = false): Boolean {
        val mediaIndex = media.indexOfFirst { it.id == mediaId }
        AppLogger.i("updateItem: mediaId=$mediaId idx=$mediaIndex")
        if (mediaIndex < 0) return false

        val item = media[mediaIndex]

        if (isUploaded) {
            item.status = Media.Status.Uploaded.id
            AppLogger.i("Media item $mediaId uploaded, notifying item changed at position $mediaIndex")
            notifyItemChanged(mediaIndex, "full")
        } else if (progress >= 0) {
            item.uploadPercentage = progress
            item.status = Media.Status.Uploading.id
            notifyItemChanged(mediaIndex, "progress")
        } else {
            item.status = Media.Status.Queued.id
            notifyItemChanged(mediaIndex, "full")
        }

        return true
    }

    fun removeItem(mediaId: Long): Boolean {
        val idx = media.indexOfFirst { it.id == mediaId }
        if (idx < 0) return false

        media.removeAt(idx)

        notifyItemRemoved(idx)

        checkSelecting?.invoke()

        return true
    }

    fun updateData(newMediaList: List<Media>) {
        val diffCallback = MediaDiffCallback(this.media, newMediaList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        this.media.clear()
        this.media.addAll(newMediaList)

        diffResult.dispatchUpdatesTo(this)
    }

    private fun selectView(view: View) {
        val mediaId = view.tag as? Long ?: return

        val m = media.firstOrNull { it.id == mediaId } ?: return
        m.selected = !m.selected
        m.save()

        notifyItemChanged(media.indexOf(m))

        checkSelecting?.invoke()
    }

    fun onItemMove(oldPos: Int, newPos: Int) {

        val mediaToMov = media.removeAt(oldPos)
        media.add(newPos, mediaToMov)

        var priority = media.size

        for (item in media) {
            item.priority = priority--
            item.save()
        }

        notifyItemMoved(oldPos, newPos)
    }

    fun deleteItem(pos: Int) {
        if (pos < 0 || pos >= media.size) return

        val item = media[pos]
//        var undone = false

//        val snackbar = Snackbar.make(recyclerView, R.string.confirm_remove_media, Snackbar.LENGTH_LONG)
//        snackbar.setAction(R.string.undo) { _ ->
//            undone = true
//            media.add(pos, item)
//
//            notifyItemInserted(pos)
//        }

//        snackbar.addCallback(object : Snackbar.Callback() {
//            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
//                if (!undone) {
        val collection = item.collection

        // Delete collection along with the item, if the collection
        // would become empty.
        if ((collection?.size ?: 0) < 2) {
            collection?.delete()
        } else {
            item.delete()
        }

        BroadcastManager.postDelete(recyclerView.context, item.id)
//                }
//
//                super.onDismissed(transientBottomBar, event)
//            }
//        })

        // snackbar.show()

        removeItem(item.id)

        mActivity.get()?.let {
            BroadcastManager.postDelete(it, item.id)
        }
    }


    fun deleteSelected(): Boolean {
        var hasDeleted = false

        for (item in media.filter { it.selected }) {
            val idx = media.indexOf(item)
            media.remove(item)

            notifyItemRemoved(idx)

            item.delete()

            hasDeleted = true
        }

        checkSelecting?.invoke()

        return hasDeleted
    }
}

class MediaDiffCallback(
    private val oldList: List<Media>,
    private val newList: List<Media>
) : DiffUtil.Callback() {

    override fun getOldListSize() = oldList.size

    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // Compare only the fields that affect the UI

        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]

        return oldItem.status == newItem.status &&
                oldItem.uploadPercentage == newItem.uploadPercentage &&
                oldItem.selected == newItem.selected &&
                oldItem.title == newItem.title
    }
}