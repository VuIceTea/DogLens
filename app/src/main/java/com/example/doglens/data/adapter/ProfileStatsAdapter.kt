package com.example.doglens.data.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.doglens.R

class ProfileStatsAdapter(
    private val stats: List<StatItem>
) : RecyclerView.Adapter<ProfileStatsAdapter.StatViewHolder>() {

    data class StatItem(
        val icon: Int,
        val value: Int,
        val label: String,
        val iconBackgroundColor: String,
        val iconTintColor: String
    )

    inner class StatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgIcon: ImageView = itemView.findViewById(R.id.imgStatIcon)
        val txtValue: TextView = itemView.findViewById(R.id.txtStatValue)
        val txtLabel: TextView = itemView.findViewById(R.id.txtStatLabel)
        val statIconContainer: FrameLayout = itemView.findViewById(R.id.statIconContainer)

        val imgStatIcon: ImageView = itemView.findViewById(R.id.imgStatIcon)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ProfileStatsAdapter.StatViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_profile_stat, parent, false)
        return StatViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ProfileStatsAdapter.StatViewHolder,
        position: Int
    ) {
        val item = stats[position]

        holder.imgIcon.setImageResource(item.icon)
        holder.txtValue.text = item.value.toString()
        holder.txtLabel.text = item.label.toString()
        holder.imgStatIcon.setImageResource(item.icon)

        holder.statIconContainer.backgroundTintList =
            ColorStateList.valueOf(Color.parseColor(item.iconBackgroundColor))
    }

    override fun getItemCount(): Int {
        return stats.size
    }
}