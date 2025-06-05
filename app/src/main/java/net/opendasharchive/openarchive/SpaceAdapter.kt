package net.opendasharchive.openarchive

import android.content.Context
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.opendasharchive.openarchive.databinding.RvSpacesRowBinding
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.util.extensions.scaled
import java.lang.ref.WeakReference

interface SpaceAdapterListener {

    fun spaceClicked(space: Space)

    fun editSpaceClicked(spaceId: Long?)

    fun getSelectedSpace(): Space?
}

class SpaceAdapter(private val context: Context, listener: SpaceAdapterListener?) : ListAdapter<Space, SpaceAdapter.ViewHolder>(DIFF_CALLBACK), SpaceAdapterListener {

    inner class ViewHolder(private val binding: RvSpacesRowBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(listener: WeakReference<SpaceAdapterListener>?, space: Space?) {

            val isSelected = listener?.get()?.getSelectedSpace()?.id == space?.id
            itemView.isSelected = isSelected
            val textColorRes = if (isSelected) R.color.colorTertiary else R.color.colorText
            val iconColorRes = if (isSelected) R.color.colorTertiary else R.color.colorOnBackground
            val backgroundRes = if (isSelected) R.drawable.item_background_selector else android.R.color.transparent

            binding.root.setBackgroundResource(backgroundRes)

            val icon = space?.getAvatar(context)?.scaled(32, context)

            icon?.setTint(ContextCompat.getColor(binding.rvIcon.context, iconColorRes))
            binding.rvIcon.setImageDrawable(icon)

            binding.rvEdit.setColorFilter(ContextCompat.getColor(binding.rvEdit.context, iconColorRes))


            binding.rvTitle.text = space?.friendlyName
            binding.rvTitle.setTextColor(ContextCompat.getColor(binding.rvTitle.context, textColorRes))


            binding.rvEdit.setOnClickListener {
                listener?.get()?.editSpaceClicked(space?.id)
            }

            if (space != null) {
                binding.root.setOnClickListener {
                    listener?.get()?.spaceClicked(space)
                }
            } else {
                binding.root.setOnClickListener(null)
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Space>() {
            override fun areItemsTheSame(oldItem: Space, newItem: Space): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Space, newItem: Space): Boolean {
                return oldItem.friendlyName == newItem.friendlyName
            }
        }

    }

    private val mListener = WeakReference(listener)

    private var mLastSelected: Space? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(RvSpacesRowBinding.inflate(LayoutInflater.from(parent.context),
            parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val space = getItem(position)

        holder.bind(WeakReference(this), space)
    }

    fun update(spaces: List<Space>) {
        notifyItemChanged(getIndex(mLastSelected))

        //@Suppress("NAME_SHADOWING")
        //val spaces = spaces.toMutableList()
        //spaces.add(Space(ADD_SPACE_ID))

        submitList(spaces)
    }

    override fun spaceClicked(space: Space) {
        // Notify previous and new selected items
        val previousIndex = getIndex(getSelectedSpace())
        val newIndex = getIndex(space)

        mLastSelected = space

        notifyItemChanged(previousIndex)
        notifyItemChanged(newIndex)

        mListener.get()?.spaceClicked(space)
    }

    override fun editSpaceClicked(spaceId: Long?) {
        mListener.get()?.editSpaceClicked(spaceId)
    }

    override fun getSelectedSpace(): Space? {
        mLastSelected = mListener.get()?.getSelectedSpace()

        return mLastSelected
    }

    private fun getIndex(space: Space?): Int {
        return if (space == null) {
            -1
        }
        else {
            currentList.indexOf(space)
        }
    }
}

class SpaceItemDecoration(private val space: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        // Add space to the bottom of each item except the last one
        val position = parent.getChildAdapterPosition(view)
        val itemCount = state.itemCount

        outRect.bottom = space

        // Optional: Add top margin only to the first item
        if (position == 0) {
            outRect.top = space
        }
    }
}