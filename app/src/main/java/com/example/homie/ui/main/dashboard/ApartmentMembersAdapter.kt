package com.example.homie.ui.main.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.homie.data.model.User
import com.example.homie.databinding.ItemMemberSettingsBinding

class ApartmentMembersAdapter(
    private val currentUserId: String,
    private val onRemoveClick: (User) -> Unit
) : ListAdapter<User, ApartmentMembersAdapter.ViewHolder>(UserDiffCallback()) {

    inner class ViewHolder(val binding: ItemMemberSettingsBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMemberSettingsBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = getItem(position)
        holder.binding.tvMemberName.text = user.name.ifBlank { user.email }

        val isSelf = user.userId == currentUserId
        holder.binding.btnRemove.isEnabled = !isSelf
        holder.binding.btnRemove.alpha = if (isSelf) 0.3f else 1f
        if (!isSelf) {
            holder.binding.btnRemove.setOnClickListener { onRemoveClick(user) }
        }
    }

    class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User) =
            oldItem.userId == newItem.userId
        override fun areContentsTheSame(oldItem: User, newItem: User) = oldItem == newItem
    }
}
