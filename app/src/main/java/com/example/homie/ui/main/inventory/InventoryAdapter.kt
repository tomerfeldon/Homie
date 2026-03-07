package com.example.homie.ui.main.inventory

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.homie.data.model.InventoryItem
import com.example.homie.databinding.ItemInventoryBinding

class InventoryAdapter(
    private val onPurchased: (InventoryItem, Boolean) -> Unit
) : RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder>() {

    private val list = mutableListOf<InventoryItem>()

    fun submitList(data: List<InventoryItem>) {
        list.clear()
        list.addAll(data)
        notifyDataSetChanged()
    }

    inner class InventoryViewHolder(val binding: ItemInventoryBinding)
        : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InventoryViewHolder {
        val binding = ItemInventoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return InventoryViewHolder(binding)
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: InventoryViewHolder, position: Int) {

        val item = list[position]

        holder.binding.checkPurchased.text = item.name
        holder.binding.checkPurchased.isChecked = item.purchased
        holder.binding.tvQuantity.text = "Qty: ${item.quantity}"
        holder.binding.tvAddedBy.text = "Added by ${item.addedByName}"

        holder.binding.checkPurchased.setOnCheckedChangeListener(null)

        holder.binding.checkPurchased.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !item.purchased) {
                onPurchased(item, true)
            }
        }
    }
}
