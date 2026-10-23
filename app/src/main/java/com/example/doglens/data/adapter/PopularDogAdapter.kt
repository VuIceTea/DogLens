package com.example.doglens.data.adapter

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.doglens.R
import com.example.doglens.data.model.Dog

class PopularDogAdapter(
    private  val populars: List<Dog>
) : RecyclerView.Adapter<PopularDogAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val img: ImageView = view.findViewById(R.id.imgPopular)
        val name: TextView = view.findViewById(R.id.txtPopularDogName)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_populardog, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val popular = populars[position]

        holder.name.text = popular.name
        holder.img.setImageResource(popular.image)
    }

    override fun getItemCount(): Int {
        return populars.size
    }

}