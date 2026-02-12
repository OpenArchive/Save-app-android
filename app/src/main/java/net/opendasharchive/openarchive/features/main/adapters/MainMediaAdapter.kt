package net.opendasharchive.openarchive.features.main.adapters

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.databinding.RvMediaBoxBinding
import net.opendasharchive.openarchive.db.sugar.Media
import net.opendasharchive.openarchive.features.main.MainActivity
import net.opendasharchive.openarchive.upload.BroadcastManager
import java.lang.ref.WeakReference

class MainMediaAdapter(
    private val activity: Activity?,
    private val mediaList: List<Media>,
    private val recyclerView: RecyclerView,
    private val checkSelecting: () -> Unit,
    private val allowMultiProjectSelection: Boolean = false,
    private val onDeleteClick: (Media, Int) -> Unit,
) : RecyclerView.Adapter<MainMediaViewHolder>() {

    companion object {
        private const val PAYLOAD_SELECTION = "selection"
        private const val PAYLOAD_PROGRESS = "progress"

        private val supportedStatuses: List<Media.Status> = listOf(
            Media.Status.Local, Media.Status.Uploading, Media.Status.Error
        )
    }

    var media: ArrayList<Media> = ArrayList(mediaList)
        private set

    var doImageFade = true

    var isEditMode = false

    var selecting = false

    private var mActivity = WeakReference(activity)

    private val selectedItems = mutableSetOf<Long>()

    init {
        setHasStableIds(true)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainMediaViewHolder {
        val binding = RvMediaBoxBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val mvh = MainMediaViewHolder(binding)

        // Normal click: either toggle selection if already in selection mode or perform normal action.
        mvh.itemView.setOnClickListener { v ->
            val pos = recyclerView.getChildLayoutPosition(v)
            if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
            if (selecting) {
                toggleSelection(pos)
            } else {
                handleNormalClick(pos)
            }
        }

        // Long-click: enable selection mode (if not already enabled) and toggle selection.
        mvh.itemView.setOnLongClickListener { v ->
            val pos = recyclerView.getChildLayoutPosition(v)
            if (pos == RecyclerView.NO_POSITION) return@setOnLongClickListener true
            if (!selecting) {
                selecting = true
                // If multi-project selection is allowed, the parent fragment may already have enabled selection
                // on other adapters. Otherwise, we are only enabling it here.
                checkSelecting.invoke()
            }
            toggleSelection(pos)
            true
        }

        return mvh
    }

    override fun getItemCount(): Int = media.size

    override fun getItemId(position: Int): Long = media[position].id

    override fun onBindViewHolder(holder: MainMediaViewHolder, position: Int) {
        AppLogger.i("onBindViewHolder called for position $position")
        holder.bind(media[position], selecting, doImageFade)
    }

    override fun onBindViewHolder(
        holder: MainMediaViewHolder, position: Int, payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty()) {
            val payload = payloads[0]
            when (payload) {
                "progress" -> {
                    holder.updateProgress(media[position].uploadPercentage ?: 0)
                }

                "full" -> {
                    holder.bind(media[position], selecting, doImageFade)
                }
            }
        } else {
            holder.bind(media[position], selecting, doImageFade)
        }
    }

    // --- Helper functions for selection handling ---
    private fun toggleSelection(position: Int) {
        val item = media[position]
        item.selected = !item.selected
        item.save()
        notifyItemChanged(position)
        // Update the adapter’s overall selecting flag.
        selecting = media.any { it.selected }
        checkSelecting.invoke()
    }

    private fun handleNormalClick(position: Int) {
        val item = media[position]
        val mediaStatus = item.sStatus
        // Default behavior if needed.
        if (mediaStatus == Media.Status.Local) {
            if (supportedStatuses.contains(Media.Status.Local)) {
                mActivity.get()?.let {
                    //PreviewActivity.start(it, item.projectId)
                }
            }
        } else if (mediaStatus == Media.Status.Queued || mediaStatus == Media.Status.Uploading) {
            if (supportedStatuses.contains(Media.Status.Uploading)) {
                (mActivity.get() as? MainActivity)?.showUploadManagerFragment()
            }
        } else if (mediaStatus == Media.Status.Error) {
            if (supportedStatuses.contains(Media.Status.Error)) {
                onDeleteClick.invoke(item, position)
            }
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
        checkSelecting.invoke()
        return true
    }

    fun updateData(newMediaList: List<Media>) {
        val diffCallback = MediaDiffCallback(this.media, newMediaList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        media.clear()
        media.addAll(newMediaList)
        diffResult.dispatchUpdatesTo(this)
    }

    fun clearSelections() {
        selectedItems.clear()
        media.forEach {
            if (it.selected) {
                it.selected = false
                it.save()
            }
        }
        selecting = false
        notifyDataSetChanged()
        checkSelecting.invoke()
    }

    private fun selectView(view: View) {
        if (!selecting) return

        val mediaId = view.tag as? Long ?: return
        val wasSelected = selectedItems.contains(mediaId)

        if (wasSelected) {
            selectedItems.remove(mediaId)
        } else {
            if (!allowMultiProjectSelection) {
                selectedItems.clear()
                media.forEach { it.selected = false }
            }
            selectedItems.add(mediaId)
        }

        media.firstOrNull { it.id == mediaId }?.selected = !wasSelected
        checkSelecting.invoke()
        notifyItemChanged(media.indexOfFirst { it.id == mediaId })
    }

    fun onItemMove(oldPos: Int, newPos: Int) {
        if (!isEditMode) return

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

//        val snackbar =
//            Snackbar.make(recyclerView, R.string.confirm_remove_media, Snackbar.LENGTH_INDEFINITE)
//        snackbar.setAction(R.string.undo) { _ ->
//            undone = true
//            media.add(pos, item)
//
//            notifyItemInserted(pos)
//        }
//
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


//                }
//
//                super.onDismissed(transientBottomBar, event)
//            }
//        })

        //snackbar.show()

        removeItem(item.id)

        BroadcastManager.postDelete(recyclerView.context, item.id)
    }

    fun getSelectedCount(): Int = media.count { it.selected }

    fun deleteSelected(): Boolean {
        var hasDeleted = false
        // Copy list to avoid concurrent modification.
        val selectedItems = media.filter { it.selected }
        selectedItems.forEach { item ->
            val idx = media.indexOf(item)
            if (idx != -1) {
                media.removeAt(idx)
                notifyItemRemoved(idx)
                item.delete()
                hasDeleted = true
            }
        }
        selecting = false
        checkSelecting.invoke()
        return hasDeleted
    }
}

private class MediaDiffCallback(
    private val oldList: List<Media>, private val newList: List<Media>
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

        return oldItem.status == newItem.status && oldItem.uploadPercentage == newItem.uploadPercentage && oldItem.selected == newItem.selected && oldItem.title == newItem.title
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        return super.getChangePayload(oldItemPosition, newItemPosition)
    }
}
