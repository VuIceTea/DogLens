package com.example.doglens.ui.article

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.doglens.R

class ViewedArticlesAdapter(
    private val showViewed: Boolean,
    private val useCoverCards: Boolean = false,
    private val onClick: (ViewedArticleRow) -> Unit
) : RecyclerView.Adapter<ViewedArticlesAdapter.Holder>() {
    private val items = mutableListOf<ViewedArticleRow>()

    fun submitList(rows: List<ViewedArticleRow>) {
        items.clear(); items.addAll(rows); notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder = Holder(
        LayoutInflater.from(parent.context).inflate(
            if (useCoverCards) R.layout.item_article_cover else R.layout.item_viewed_article, parent, false
        )
    )

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])
    override fun getItemCount(): Int = items.size

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(row: ViewedArticleRow) {
            itemView.bindArticleCard(row, showViewed)
            itemView.setOnClickListener { onClick(row) }
        }
    }
}

internal fun View.bindArticleCard(row: ViewedArticleRow, showViewed: Boolean = false) {
    val article = row.article
    findViewById<ImageView>(R.id.imgArticle).loadArticlePhoto(article)
    findViewById<TextView>(R.id.tvCategory).apply {
        text = row.category.name
        backgroundTintList = ColorStateList.valueOf(Color.parseColor(row.category.colorHex))
    }
    findViewById<TextView>(R.id.tvDate)?.text = if (showViewed) article.publishedAt else ""
    findViewById<TextView>(R.id.articleListMeta)?.text =
        if (showViewed) "Đã xem" else "${article.readMinutes} phút đọc"
    findViewById<View>(R.id.articleViewedIcon)?.visibility = if (showViewed) View.VISIBLE else View.GONE
    findViewById<TextView>(R.id.articleListSummary).apply {
        text = article.summary
        visibility = if (!showViewed && article.summary.isNotBlank()) View.VISIBLE else View.GONE
    }
    findViewById<TextView>(R.id.tvTitle).text = article.title
}
