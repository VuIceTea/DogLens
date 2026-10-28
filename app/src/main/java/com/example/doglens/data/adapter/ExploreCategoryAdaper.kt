package com.example.doglens.data.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.doglens.data.model.Dog
import com.example.doglens.R
import com.example.doglens.data.model.Category
import com.google.android.material.card.MaterialCardView

class ExploreCategoryAdaper(
    private val onClick: (Dog) -> Unit
) : RecyclerView.Adapter<ExploreCategoryAdaper.ViewHolder>() {

    private val items = mutableListOf<Dog>()

    // cập nhật dữ liệu sau khi tạo Adapter
    fun submitList(newItems: List<Dog>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged() // báo RecyclerView vẽ lại danh sách
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.cardDogCategory)
        val categoryImage: ImageView = view.findViewById(R.id.imgDogCategory)
        val categoryName: TextView = view.findViewById(R.id.txtDogCategoryName)

        val categoryDesc: TextView = view.findViewById(R.id.txtDogCategoryDesc)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_explore_category, parent, false)

        val density = parent.resources.displayMetrics.density
        val itemWidth = (112 * density).toInt()

        view.layoutParams =
            RecyclerView.LayoutParams(itemWidth, RecyclerView.LayoutParams.WRAP_CONTENT)
                .apply {
                    marginEnd = (10 * density).toInt()
                }
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val category = items[position]

        holder.categoryName.text = category.name
        holder.categoryDesc.text = category.description
        category.localImageRes?.let {
            holder.categoryImage.setImageResource(it)
        }
        holder.card.setCardBackgroundColor(
            android.graphics.Color.parseColor(category.backgroundColorHex)
        )
        holder.itemView.setOnClickListener {
            onClick(category)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }
}