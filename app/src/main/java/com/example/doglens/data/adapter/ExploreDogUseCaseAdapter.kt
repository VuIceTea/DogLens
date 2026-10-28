package com.example.doglens.data.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.doglens.R
import com.example.doglens.data.model.DogUseCase

class ExploreDogUseCaseAdapter(
    private val onClick: (DogUseCase) -> Unit
) : RecyclerView.Adapter<ExploreDogUseCaseAdapter.ViewHolder>() {

    private val items = mutableListOf<DogUseCase>()

    fun submitList(newItems: List<DogUseCase>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.imgDogUseCase)
        val name: TextView = view.findViewById(R.id.txtDogUseCaseName)
        val desc: TextView = view.findViewById(R.id.txtDogUseCaseDesc)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dog_use_case, parent, false)
        val density = parent.resources.displayMetrics.density
        val itemWidth = (112 * density).toInt()
        view.layoutParams =
            RecyclerView.LayoutParams(itemWidth, RecyclerView.LayoutParams.WRAP_CONTENT).apply {
                marginEnd = (10 * density).toInt()
            }
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val item = items[position]
        holder.name.text = item.name
        holder.desc.text = item.description
        if(item.localImageRes != null) {
            holder.image.setImageResource(item.localImageRes)
        } else {
            holder.image.setImageDrawable(null)
        }
        holder.itemView.setOnClickListener {
            onClick(item)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }
}