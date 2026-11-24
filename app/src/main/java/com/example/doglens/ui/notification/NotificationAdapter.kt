package com.example.doglens.ui.notification

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.core.view.updateLayoutParams
import com.example.doglens.R

class NotificationAdapter(
    private val onClick: (DogLensNotification) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val rows = mutableListOf<NotificationRow>()

    fun submitList(newRows: List<NotificationRow>) {
        rows.clear()
        rows.addAll(newRows)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int) = when (rows[position]) {
        is NotificationRow.Header -> TYPE_HEADER
        is NotificationRow.Item -> TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderHolder(inflater.inflate(R.layout.item_notification_header, parent, false))
        } else {
            ItemHolder(inflater.inflate(R.layout.item_notification, parent, false))
        }
    }

    override fun getItemCount() = rows.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is NotificationRow.Header -> (holder as HeaderHolder).bind(row)
            is NotificationRow.Item -> (holder as ItemHolder).bind(row.notification)
        }
    }

    private class HeaderHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(row: NotificationRow.Header) {
            itemView.findViewById<TextView>(R.id.tvSectionTitle).text = row.title
        }
    }

    private inner class ItemHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(item: DogLensNotification) {
            itemView.setBackgroundResource(if (item.isRead) R.drawable.bg_notification_read else R.drawable.bg_notification_unread)
            itemView.findViewById<TextView>(R.id.tvNotificationTitle).text = item.title
            itemView.findViewById<TextView>(R.id.tvNotificationMessage).text = item.message
            itemView.findViewById<TextView>(R.id.tvNotificationTime).text = item.time
            itemView.findViewById<View>(R.id.unreadDot).visibility = if (item.isRead) View.GONE else View.VISIBLE

            val iconContainer = itemView.findViewById<View>(R.id.notificationIconContainer)
            val icon = itemView.findViewById<ImageView>(R.id.imgNotificationIcon)
            when (item.type) {
                NotificationType.RECOGNITION -> {
                    icon.setImageResource(R.drawable.topic_kid_friendly_golden)
                    icon.imageTintList = null
                    icon.setBackgroundResource(R.drawable.bg_notification_avatar_clip)
                    icon.scaleType = ImageView.ScaleType.CENTER_CROP
                    icon.updateLayoutParams {
                        width = itemView.resources.getDimensionPixelSize(R.dimen.notification_avatar_size)
                        height = itemView.resources.getDimensionPixelSize(R.dimen.notification_avatar_size)
                    }
                    iconContainer.setBackgroundResource(R.drawable.bg_notification_icon_blue)
                }
                NotificationType.FAVORITE -> bindIcon(iconContainer, icon, R.drawable.ic_heart_favorite, R.drawable.bg_notification_icon_pink, "#FF4564")
                NotificationType.ARTICLE -> bindIcon(iconContainer, icon, R.drawable.ic_article_bookmark_outline, R.drawable.bg_notification_icon_blue, "#168BFF")
                NotificationType.UPDATE -> bindIcon(iconContainer, icon, R.drawable.ic_system_update, R.drawable.bg_notification_icon_purple, "#8B5CF6")
            }
            itemView.setOnClickListener { onClick(item) }
        }

        private fun bindIcon(container: View, icon: ImageView, iconRes: Int, backgroundRes: Int, color: String) {
            container.setBackgroundResource(backgroundRes)
            icon.setImageResource(iconRes)
            icon.background = null
            icon.scaleType = ImageView.ScaleType.CENTER_INSIDE
            val size = itemView.resources.getDimensionPixelSize(R.dimen.notification_icon_size)
            icon.updateLayoutParams { width = size; height = size }
            icon.imageTintList = ColorStateList.valueOf(Color.parseColor(color))
        }
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }
}
