package net.opendasharchive.openarchive.services.snowbird

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.OneLineRowBinding
import net.opendasharchive.openarchive.db.SnowbirdRepo
import net.opendasharchive.openarchive.db.shortHash
import net.opendasharchive.openarchive.extensions.scaled
import net.opendasharchive.openarchive.util.TwoLetterDrawable
import java.lang.ref.WeakReference

class SnowbirdRepoListAdapter(listener: ((String) -> Unit)? = null)
    : ListAdapter<SnowbirdRepo, SnowbirdRepoListAdapter.SnowbirdRepoListViewHolder>(DIFF_CALLBACK) {

    inner class SnowbirdRepoListViewHolder(private val binding: OneLineRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(repo: SnowbirdRepo?) {
            if (repo == null) {
                return
            }

            val context = binding.button.context

            binding.button.setLeftIcon(ContextCompat.getDrawable(context, R.drawable.snowbird)?.scaled(40, context))
            //binding.button.setBackgroundResource(R.drawable.button_outlined_ripple)
            binding.button.setTitle(repo.name)
            binding.button.setSubTitle(repo.shortHash())

            if (repo.permissions == "READ_ONLY") {
                binding.button.setRightIcon(TwoLetterDrawable.ReadOnly(context))
            } else {
                binding.button.setRightIcon(TwoLetterDrawable.ReadWrite(context))
            }

            binding.button.setOnClickListener {
                mListener.get()?.invoke(repo.key)
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SnowbirdRepo>() {
            override fun areItemsTheSame(oldItem: SnowbirdRepo, newItem: SnowbirdRepo): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: SnowbirdRepo, newItem: SnowbirdRepo): Boolean {
                return oldItem.key == newItem.key
            }
        }
    }

    private val mListener = WeakReference(listener)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SnowbirdRepoListViewHolder {
        return SnowbirdRepoListViewHolder(
            OneLineRowBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: SnowbirdRepoListViewHolder, position: Int) {
        val repo = getItem(position)
        holder.bind(repo)
    }
}