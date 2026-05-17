package com.example.homie.ui.main.notifications

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.homie.data.model.NotificationItem
import com.example.homie.databinding.ItemNotificationBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationsAdapter(
    private val onItemClick: (NotificationItem) -> Unit
) : ListAdapter<NotificationItem, NotificationsAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.binding.tvNotificationTitle.text = item.title
        holder.binding.tvNotificationBody.text = item.body
        holder.binding.tvNotificationTime.text = formatTime(item.timestamp)

        val bgColor = if (item.read) android.R.color.transparent else com.example.homie.R.color.unread_notification_bg
        holder.itemView.setBackgroundResource(bgColor)

        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    private fun formatTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        return when {
            diff < 60_000 -> "Just now"
            diff < 3_600_000 -> "${diff / 60_000}m ago"
            diff < 86_400_000 -> "${diff / 3_600_000}h ago"
            else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<NotificationItem>() {
        override fun areItemsTheSame(a: NotificationItem, b: NotificationItem) = a.id == b.id
        override fun areContentsTheSame(a: NotificationItem, b: NotificationItem) = a == b
    }
}
