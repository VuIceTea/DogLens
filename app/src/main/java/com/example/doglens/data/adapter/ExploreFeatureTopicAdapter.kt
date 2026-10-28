package com.example.doglens.data.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.doglens.R
import com.example.doglens.data.model.FeaturedTopic

class ExploreFeatureTopicAdapter(
    private val onClick: (FeaturedTopic) -> Unit
) : RecyclerView.Adapter<ExploreFeatureTopicAdapter.ViewHolder>() {

    private val items = mutableListOf<FeaturedTopic>()

    fun submitList(newItems: List<FeaturedTopic>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.imgFeatureTopic)
        val title: TextView = view.findViewById(R.id.txtFeaturedTopicTitle)
        val nextButton: View = view.findViewById(R.id.btnFeaturedTopicNext)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_featured_topic, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val topic = items[position]

        holder.title.text = topic.title

        if(topic.localImageRes != null) {
            holder.image.setImageResource(topic.localImageRes)
        } else {
            holder.image.setImageDrawable(null)
        }

        holder.itemView.setOnClickListener {
            onClick(topic)
        }

        holder.nextButton.setOnClickListener {
            onClick(topic)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }
}