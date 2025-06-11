package com.example.sportsapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.sportsapp.databinding.ItemNotificationBinding
import com.example.sportsapp.models.AppNotification
import java.text.SimpleDateFormat
import java.util.*

class NotificationsAdapter(
    private val notifications: List<AppNotification>,
    private val onItemClick: (AppNotification) -> Unit
) : RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(notifications[position])
    }

    override fun getItemCount(): Int = notifications.size

    inner class NotificationViewHolder(
        private val binding: ItemNotificationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: AppNotification) {
            binding.apply {
                textTitle.text = notification.title
                textMessage.text = notification.message
                textTimestamp.text = formatTimestamp(notification.timestamp)

                // Set notification type icon and color
                when (notification.type) {
                    "success" -> {
                        iconType.setImageResource(R.drawable.ic_check_circle)
                        iconType.setColorFilter(ContextCompat.getColor(root.context, R.color.success_color))
                    }
                    "warning" -> {
                        iconType.setImageResource(R.drawable.ic_warning)
                        iconType.setColorFilter(ContextCompat.getColor(root.context, R.color.warning_color))
                    }
                    "error" -> {
                        iconType.setImageResource(R.drawable.ic_error)
                        iconType.setColorFilter(ContextCompat.getColor(root.context, R.color.error_color))
                    }
                    else -> {
                        iconType.setImageResource(R.drawable.ic_info)
                        iconType.setColorFilter(ContextCompat.getColor(root.context, R.color.primary_color))
                    }
                }

                // Handle read/unread state
                val backgroundColor = if (notification.isRead) {
                    ContextCompat.getColor(root.context, R.color.white)
                } else {
                    ContextCompat.getColor(root.context, R.color.notification_unread_bg)
                }
                cardNotification.setCardBackgroundColor(backgroundColor)

                val textColor = if (notification.isRead) {
                    ContextCompat.getColor(root.context, R.color.secondary_text)
                } else {
                    ContextCompat.getColor(root.context, R.color.primary_text)
                }
                textTitle.setTextColor(textColor)

                // Show unread indicator
                unreadIndicator.visibility = if (notification.isRead) View.GONE else View.VISIBLE

                // Handle click
                root.setOnClickListener {
                    onItemClick(notification)
                }
            }
        }

        private fun formatTimestamp(timestamp: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                val date = inputFormat.parse(timestamp)
                date?.let { outputFormat.format(it) } ?: timestamp
            } catch (e: Exception) {
                timestamp
            }
        }
    }
}