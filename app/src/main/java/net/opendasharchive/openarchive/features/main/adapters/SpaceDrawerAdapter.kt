package net.opendasharchive.openarchive.features.main.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.RvDrawerRowBinding
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.util.extensions.scaled

interface SpaceDrawerAdapterListener {
    fun onSpaceSelected(space: Space)
    fun onAddNewSpace()
    fun getSelectedSpace(): Space?
}

class SpaceDrawerAdapter(private val listener: SpaceDrawerAdapterListener) : ListAdapter<SpaceDrawerAdapter.SpaceItem, SpaceDrawerAdapter.ItemTypeViewHolder>(DIFF_CALLBACK) {

    private var selectedSpace: Space? = listener.getSelectedSpace()

    sealed class SpaceItem {
        data class SpaceItemData(val space: Space) : SpaceItem()
        data object AddSpaceItem : SpaceItem()
    }

    companion object {

        private const val VIEW_TYPE_SPACE = 0
        private const val VIEW_TYPE_ADD = 1

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SpaceItem>() {
            override fun areItemsTheSame(oldItem: SpaceItem, newItem: SpaceItem): Boolean {
                return when {
                    oldItem is SpaceItem.SpaceItemData && newItem is SpaceItem.SpaceItemData -> oldItem.space.id == newItem.space.id
                    oldItem is SpaceItem.AddSpaceItem && newItem is SpaceItem.AddSpaceItem -> true
                    else -> false
                }
            }

            override fun areContentsTheSame(oldItem: SpaceItem, newItem: SpaceItem): Boolean {
                return when {
                    oldItem is SpaceItem.SpaceItemData && newItem is SpaceItem.SpaceItemData -> oldItem.space.friendlyName == newItem.space.friendlyName
                    oldItem is SpaceItem.AddSpaceItem && newItem is SpaceItem.AddSpaceItem -> true
                    else -> false
                }
            }
        }
    }

    abstract class ItemTypeViewHolder(binding: RvDrawerRowBinding) : RecyclerView.ViewHolder(binding.root) {
        abstract fun bind(item: SpaceItem)
    }

    inner class SpaceViewHolder(private val binding: RvDrawerRowBinding) : ItemTypeViewHolder(binding) {
        override fun bind(item: SpaceItem) {

            val space = (item as SpaceItem.SpaceItemData).space

            val isSelected = listener.getSelectedSpace()?.id == space.id
            val backgroundColor = if(isSelected) R.color.colorTertiary else R.color.c23_light_grey
            val textColor = if (isSelected) R.color.colorOnBackground else R.color.colorText

            binding.root.setBackgroundColor(binding.root.context.getColor(backgroundColor))

            val icon = space.getAvatar(binding.rvIcon.context)?.scaled(21, binding.rvIcon.context)
            icon?.setTint(binding.rvIcon.context.getColor(R.color.colorOnBackground))
            binding.rvIcon.setImageDrawable(icon)

            binding.rvTitle.text = space.friendlyName
            binding.rvTitle.setTextColor(binding.rvTitle.context.getColor(textColor))

            binding.root.setOnClickListener {
                onItemSelected(space)
            }
        }

        private fun onItemSelected(space: Space) {
            val previousIndex = currentList.indexOfFirst { it is SpaceItem.SpaceItemData && it.space.id == selectedSpace?.id }
            val newIndex = currentList.indexOfFirst { it is SpaceItem.SpaceItemData && it.space.id == space.id }

            selectedSpace = space

            if (previousIndex != -1) notifyItemChanged(previousIndex)
            if (newIndex != -1) notifyItemChanged(newIndex)

            listener.onSpaceSelected(space)
        }
    }

    inner class AddSpaceViewHolder(private val binding: RvDrawerRowBinding) : ItemTypeViewHolder(binding) {
        override fun bind(item: SpaceItem) {
            val context = binding.rvTitle.context
            binding.rvTitle.text = context.getString(R.string.add_another_account)
            binding.rvTitle.setTextColor(ContextCompat.getColor(context, R.color.colorTertiary))

            val icon = ContextCompat.getDrawable(context, R.drawable.ic_add)
            icon?.setTint(ContextCompat.getColor(binding.rvIcon.context, R.color.colorTertiary))
            binding.rvIcon.setImageDrawable(icon)

            binding.root.setOnClickListener {
                listener.onAddNewSpace()
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is SpaceItem.SpaceItemData -> VIEW_TYPE_SPACE
            is SpaceItem.AddSpaceItem -> VIEW_TYPE_ADD
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemTypeViewHolder {
        val binding = RvDrawerRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return when (viewType) {
            VIEW_TYPE_SPACE -> SpaceViewHolder(binding)
            VIEW_TYPE_ADD -> AddSpaceViewHolder(binding)
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: ItemTypeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun update(spaces: List<Space>) {
        val items = spaces.map { SpaceItem.SpaceItemData(it) } + SpaceItem.AddSpaceItem
        submitList(items)
    }

    fun updateSelectedSpace(space: Space?) {
        val previousIndex = currentList.indexOfFirst { it is SpaceItem.SpaceItemData && it.space.id == selectedSpace?.id }
        val newIndex = currentList.indexOfFirst { it is SpaceItem.SpaceItemData && it.space.id == space?.id }

        selectedSpace = space

        if (previousIndex != -1) notifyItemChanged(previousIndex)
        if (newIndex != -1) notifyItemChanged(newIndex)
    }
}