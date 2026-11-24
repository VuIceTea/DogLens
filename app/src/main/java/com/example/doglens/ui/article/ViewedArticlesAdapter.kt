package com.example.doglens.ui.article

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.doglens.R

class ViewedArticlesAdapter(
    private val onClick: (ViewedArticleRow) -> Unit
) : RecyclerView.Adapter<ViewedArticlesAdapter.Holder>() {
    private val items = mutableListOf<ViewedArticleRow>()

    fun submitList(rows: List<ViewedArticleRow>) {
        items.clear(); items.addAll(rows); notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder = Holder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_viewed_article, parent, false)
    )

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])
    override fun getItemCount(): Int = items.size

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(row: ViewedArticleRow) {
            val article = row.article
            val image = itemView.findViewById<ImageView>(R.id.imgArticle)
            when {
                article.localImageRes != null -> image.setImageResource(article.localImageRes)
                !article.imageUrl.isNullOrBlank() -> Glide.with(image).load(article.imageUrl).centerCrop().into(image)
            }
            itemView.findViewById<TextView>(R.id.tvCategory).apply {
                text = row.category.name
                backgroundTintList = ColorStateList.valueOf(Color.parseColor(row.category.colorHex))
            }
            itemView.findViewById<TextView>(R.id.tvDate).text = article.publishedAt
            itemView.findViewById<TextView>(R.id.tvTitle).text = article.title
            itemView.setOnClickListener { onClick(row) }
        }
    }
}
