package com.example.doglens.data.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.doglens.R
import com.example.doglens.data.model.FavoriteDogItem

class FavoriteDogAdapter(
    private val onDogClick: (FavoriteDogItem) -> Unit,
    private val onFavoriteClick: (FavoriteDogItem) -> Unit
) : RecyclerView.Adapter<FavoriteDogAdapter.ViewHolder>() {

    private val items = mutableListOf<FavoriteDogItem>()

    fun submitLists(newItems: List<FavoriteDogItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView =
            view.findViewById(R.id.imgFavoriteDog)

        val name: TextView =
            view.findViewById(R.id.txtFavoriteDogName)

        val description: TextView =
            view.findViewById(R.id.txtFavoriteDogDesc)

        val weight: TextView =
            view.findViewById(R.id.txtFavoriteDogWeight)

        val height: TextView =
            view.findViewById(R.id.txtFavoriteDogHeight)

        val removeButton: ImageButton =
            view.findViewById(R.id.btnRemoveFavorite)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_favorite_dog, parent,false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val item = items[position]
        val dog = item.dog

        holder.name.text = dog.name
        holder.description.text = dog.description

        holder.weight.text = if (
            dog.minWeightKg != null && dog.maxWeightKg != null
        ) {
            "${dog.minWeightKg}–${dog.maxWeightKg} kg"
        } else {
            "Chưa có thông tin cân nặng"
        }

        holder.height.text = if (
            dog.minHeightCm != null && dog.maxHeightCm != null
        ) {
            "${dog.minHeightCm}–${dog.maxHeightCm} cm"
        } else {
            "Chưa có thông tin chiều cao"
        }

        if (dog.localImageRes != null) {
            holder.image.setImageResource(dog.localImageRes)
        } else {
            holder.image.setImageDrawable(null)
        }

        holder.itemView.setOnClickListener {
            onDogClick(item)
        }

        holder.removeButton.setOnClickListener {
            onFavoriteClick(item)
        }
    }

    override fun getItemCount(): Int = items.size
}