package com.example.homie.ui.main.wallet

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.homie.R
import com.example.homie.data.model.MemberBalance
import com.example.homie.databinding.ItemBalanceRowBinding

class BalanceSummaryAdapter(
    private val onSettleClick: (MemberBalance) -> Unit
) : ListAdapter<MemberBalance, BalanceSummaryAdapter.BalanceViewHolder>(DiffCallback()) {

    inner class BalanceViewHolder(val binding: ItemBalanceRowBinding)
        : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BalanceViewHolder {
        val binding = ItemBalanceRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return BalanceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BalanceViewHolder, position: Int) {
        val item = getItem(position)
        holder.binding.tvMemberName.text = item.name
        holder.binding.tvBalance.text = "%.2f ₪".format(item.balance)

        val colorRes = when {
            item.balance > 0.01 -> R.color.success
            item.balance < -0.01 -> R.color.error
            else -> R.color.text_tertiary
        }
        holder.binding.tvBalance.setTextColor(
            ContextCompat.getColor(holder.binding.root.context, colorRes)
        )

        if (item.balance < -0.01) {
            holder.binding.root.setOnClickListener { onSettleClick(item) }
            holder.binding.root.isClickable = true
        } else {
            holder.binding.root.setOnClickListener(null)
            holder.binding.root.isClickable = false
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<MemberBalance>() {
        override fun areItemsTheSame(old: MemberBalance, new: MemberBalance) =
            old.userId == new.userId
        override fun areContentsTheSame(old: MemberBalance, new: MemberBalance) =
            old == new
    }
}
