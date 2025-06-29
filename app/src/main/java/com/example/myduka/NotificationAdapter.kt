package com.example.myduka

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class NotificationAdapter(
    private val onItemClick: ((NotificationDC) -> Unit)? = null
) : ListAdapter<NotificationDC, NotificationAdapter.NotificationViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<NotificationDC>() {
            override fun areItemsTheSame(oldItem: NotificationDC, newItem: NotificationDC) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: NotificationDC, newItem: NotificationDC) =
                oldItem == newItem
        }
    }

    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTv     = itemView.findViewById<TextView>(R.id.textTitle)
        private val messageTv   = itemView.findViewById<TextView>(R.id.textMessage)
        private val timestampTv = itemView.findViewById<TextView>(R.id.textTimestamp)

        fun bind(notification: NotificationDC) {
            // 1) Content
            titleTv.text   = notification.title
            messageTv.text = notification.message

            // 2) Timestamp
            val now = System.currentTimeMillis()
            timestampTv.text = if (notification.timestamp in 1..now) {
                DateUtils.getRelativeTimeSpanString(
                    notification.timestamp, now, DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE
                ).toString()
            } else {
                "just now"
            }


            // 4) Click forward
            itemView.setOnClickListener { onItemClick?.invoke(notification) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        NotificationViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_notification, parent, false)
        )

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) =
        holder.bind(getItem(position))
}
