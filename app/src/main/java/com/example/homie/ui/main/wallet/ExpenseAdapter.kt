package com.example.homie.ui.main.wallet

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.homie.data.model.Expense
import com.example.homie.databinding.ItemExpenseBinding

class ExpenseAdapter : RecyclerView.Adapter<ExpenseAdapter.ViewHolder>() {

    private val list = mutableListOf<Expense>()

    fun submitList(data: List<Expense>) {
        list.clear()
        list.addAll(data)
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: ItemExpenseBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExpenseBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.binding.tvAmount.text = "${item.amount} ₪"
        holder.binding.tvCategory.text = item.category
        holder.binding.tvPayer.text = "Paid by ${item.payerName}"
        holder.binding.tvDescription.text = item.description

    }
}
