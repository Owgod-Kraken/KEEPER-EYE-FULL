package com.keepereye.app.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.keepereye.app.R

class HistoryAdapter(
    private val onItemClick: (String) -> Unit
) : ListAdapter<HistoryEntry, HistoryAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = getItem(position)
        holder.bind(entry)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvText: TextView = itemView.findViewById(R.id.tvHistoryText)
        private val tvTime: TextView = itemView.findViewById(R.id.tvHistoryTime)

        fun bind(entry: HistoryEntry) {
            tvText.text = entry.text
            tvTime.text = entry.getFormattedTime()
            itemView.setOnClickListener {
                onItemClick(entry.text)
            }
            itemView.contentDescription =
                "${entry.text}. ${entry.getFormattedTime()}. Toca para escuchar."
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<HistoryEntry>() {
        override fun areItemsTheSame(oldItem: HistoryEntry, newItem: HistoryEntry): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: HistoryEntry, newItem: HistoryEntry): Boolean {
            return oldItem == newItem
        }
    }
}
