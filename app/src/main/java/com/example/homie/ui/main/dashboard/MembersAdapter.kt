package com.example.homie.ui.main.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.homie.data.model.User
import com.example.homie.databinding.ItemMemberBinding

class MembersAdapter(
    private var members: List<User>
) : RecyclerView.Adapter<MembersAdapter.MemberViewHolder>() {

    inner class MemberViewHolder(val binding: ItemMemberBinding)
        : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val binding = ItemMemberBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MemberViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val user = members[position]
        holder.binding.tvName.text = user.name
    }

    override fun getItemCount() = members.size

    fun updateData(newList: List<User>) {
        members = newList
        notifyDataSetChanged()
    }
}
