package com.example.doglens.data.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.doglens.R
import com.example.doglens.data.model.RecognitionHistoryItem
import com.example.doglens.ui.history.HistoryRow

class HistoryAdapter(
    private val onItemClick: (RecognitionHistoryItem) -> Unit,
    private val onFavoriteClick: (RecognitionHistoryItem) -> Unit,
    private val onDeleteClick: (RecognitionHistoryItem) -> Unit
) : ListAdapter<HistoryRow, RecyclerView.ViewHolder>(DIFF) {

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is HistoryRow.Header -> TYPE_HEADER
        is HistoryRow.Entry -> TYPE_ENTRY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderViewHolder(inflater.inflate(R.layout.item_history_header, parent, false))
        } else {
            EntryViewHolder(inflater.inflate(R.layout.item_history_record, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is HistoryRow.Header -> (holder as HeaderViewHolder).bind(row)
            is HistoryRow.Entry -> (holder as EntryViewHolder).bind(row)
        }
    }

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTitle: TextView = view.findViewById(R.id.tvHeaderTitle)
        private val tvCount: TextView = view.findViewById(R.id.tvHeaderCount)

        fun bind(header: HistoryRow.Header) {
            tvTitle.text = header.title
            tvCount.text = "${header.count} kết quả"
        }
    }

    inner class EntryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val imgDog: ImageView = view.findViewById(R.id.imgDog)
        private val tvName: TextView = view.findViewById(R.id.tvDogName)
        private val tvTime: TextView = view.findViewById(R.id.tvTime)
        private val tvMatch: TextView = view.findViewById(R.id.tvMatch)
        private val tvWeight: TextView = view.findViewById(R.id.tvWeight)
        private val tvHeight: TextView = view.findViewById(R.id.tvHeight)
        private val tvOrigin: TextView = view.findViewById(R.id.tvOrigin)
        private val tvTag: TextView = view.findViewById(R.id.tvTag)
        private val btnFavorite: ImageView = view.findViewById(R.id.btnFavorite)
        private val btnDelete: ImageView = view.findViewById(R.id.btnDelete)

        fun bind(entry: HistoryRow.Entry) {
            val item = entry.item
            val dog = item.dog

            Glide.with(imgDog).load(item.result.imageUrl).centerCrop().into(imgDog)

            tvName.text = dog.name
            tvTime.text = entry.timeLabel
            tvMatch.text = "${item.result.confidence}% khớp"
            tvWeight.text = range(dog.minWeightKg, dog.maxWeightKg, "kg")
            tvHeight.text = range(dog.minHeightCm, dog.maxHeightCm, "cm")
            tvOrigin.text = dog.origin ?: "—"
            tvTag.text = dog.description

            btnFavorite.setImageResource(
                if (item.isFavorite) R.drawable.ic_heart_favorite else R.drawable.ic_heart_outline
            )

            itemView.setOnClickListener { onItemClick(item) }
            btnFavorite.setOnClickListener { onFavoriteClick(item) }
            btnDelete.setOnClickListener { onDeleteClick(item) }
        }

        private fun range(min: Int?, max: Int?, unit: String): String = when {
            min != null && max != null -> "$min – $max$unit"
            min != null -> "$min$unit"
            max != null -> "$max$unit"
            else -> "—"
        }
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ENTRY = 1

        private val DIFF = object : DiffUtil.ItemCallback<HistoryRow>() {
            override fun areItemsTheSame(oldItem: HistoryRow, newItem: HistoryRow): Boolean =
                when {
                    oldItem is HistoryRow.Header && newItem is HistoryRow.Header ->
                        oldItem.title == newItem.title
                    oldItem is HistoryRow.Entry && newItem is HistoryRow.Entry ->
                        oldItem.item.result.id == newItem.item.result.id
                    else -> false
                }

            override fun areContentsTheSame(oldItem: HistoryRow, newItem: HistoryRow): Boolean =
                oldItem == newItem
        }
    }
}