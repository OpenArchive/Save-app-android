package net.opendasharchive.openarchive.services.storacha

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.opendasharchive.openarchive.databinding.ItemSpaceUsageBinding
import net.opendasharchive.openarchive.services.storacha.model.SpaceUsageEntry

enum class SortType {
    NAME_ASC,
    NAME_DESC,
    SIZE_ASC,
    SIZE_DESC,
}

class SpacesUsageAdapter : ListAdapter<SpaceUsageEntry, SpacesUsageAdapter.SpaceViewHolder>(SpaceDiffCallback()) {
    private var originalList: List<SpaceUsageEntry> = emptyList()
    private var currentSortType = SortType.NAME_ASC

    fun setSpaces(spaces: List<SpaceUsageEntry>) {
        originalList = spaces
        sortAndSubmit()
    }

    fun sortBy(sortType: SortType) {
        currentSortType = sortType
        sortAndSubmit()
    }

    private fun sortAndSubmit() {
        val sortedList =
            when (currentSortType) {
                SortType.NAME_ASC -> originalList.sortedBy { it.name.lowercase() }
                SortType.NAME_DESC -> originalList.sortedByDescending { it.name.lowercase() }
                SortType.SIZE_ASC -> originalList.sortedBy { it.usage.bytes }
                SortType.SIZE_DESC -> originalList.sortedByDescending { it.usage.bytes }
            }
        submitList(sortedList)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): SpaceViewHolder {
        val binding =
            ItemSpaceUsageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
        return SpaceViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: SpaceViewHolder,
        position: Int,
    ) {
        holder.bind(getItem(position))
    }

    class SpaceViewHolder(
        private val binding: ItemSpaceUsageBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(space: SpaceUsageEntry) {
            binding.tvSpaceName.text = space.name
            binding.tvSpaceUsage.text = space.usage.human
        }
    }

    private class SpaceDiffCallback : DiffUtil.ItemCallback<SpaceUsageEntry>() {
        override fun areItemsTheSame(
            oldItem: SpaceUsageEntry,
            newItem: SpaceUsageEntry,
        ): Boolean = oldItem.spaceDid == newItem.spaceDid

        override fun areContentsTheSame(
            oldItem: SpaceUsageEntry,
            newItem: SpaceUsageEntry,
        ): Boolean = oldItem == newItem
    }
}
