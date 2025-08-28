package net.opendasharchive.openarchive.services.storacha

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.StorachaDidRowBinding

class StorachaBrowseAccountsAdapter(
    private val accounts: List<Account> = emptyList(),
    private val isDid: Boolean,
    private val onClick: (account: Account) -> Unit,
) : RecyclerView.Adapter<StorachaBrowseAccountsAdapter.AccountViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): AccountViewHolder {
        val binding = StorachaDidRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AccountViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(
        holder: AccountViewHolder,
        position: Int,
    ) {
        holder.bind(accounts[position])
    }

    override fun getItemCount(): Int = accounts.size

    inner class AccountViewHolder(
        private val binding: StorachaDidRowBinding,
        private val onClick: (account: Account) -> Unit,
    ) : RecyclerView.ViewHolder(
            binding.root,
        ) {
        fun bind(account: Account) {
            if (!isDid) {
                val icon = ContextCompat.getDrawable(binding.icon.context, R.drawable.ic_account_circle)
                icon?.setTint(ContextCompat.getColor(binding.icon.context, R.color.colorOnBackground))
                binding.icon.setImageDrawable(icon)
            } else {
                binding.icon.visibility = View.GONE
                binding.rvTick.setImageResource(R.drawable.ic_delete_danger_24dp)
                binding.rvTick.imageTintList =
                    android.content.res.ColorStateList
                        .valueOf(ContextCompat.getColor(binding.rvTick.context, R.color.red))
            }
            binding.didKey.text = account.email
            binding.rvTick.setOnClickListener {
                onClick.invoke(account)
            }
        }
    }
}

data class Account(
    val email: String,
    val sessionId: String,
)
