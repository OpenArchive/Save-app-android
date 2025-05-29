package net.opendasharchive.openarchive.services.storacha

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.StorachaSpaceRowBinding

class StorachaBrowseSpacesAdapter(
    private val spaces: List<Space> = emptyList(),
    private val onClick: (space: Space) -> Unit,
) : RecyclerView.Adapter<StorachaBrowseSpacesAdapter.SpaceViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): SpaceViewHolder {
        val binding = StorachaSpaceRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SpaceViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(
        holder: SpaceViewHolder,
        position: Int,
    ) {
        holder.bind(spaces[position])
    }

    override fun getItemCount(): Int = spaces.size

    inner class SpaceViewHolder(
        private val binding: StorachaSpaceRowBinding,
        private val onClick: (space: Space) -> Unit,
    ) : RecyclerView.ViewHolder(
            binding.root,
        ) {
        fun bind(space: Space) {
            val icon = ContextCompat.getDrawable(binding.icon.context, R.drawable.ic_folder_new)
            icon?.setTint(ContextCompat.getColor(binding.icon.context, R.color.colorOnBackground))
            binding.rvTick.visibility = View.VISIBLE
            binding.icon.setImageDrawable(icon)
            binding.name.text = space.name
            binding.root.setOnClickListener {
                onClick.invoke(space)
            }
        }
    }
}

data class Space(
    val name: String,
)
