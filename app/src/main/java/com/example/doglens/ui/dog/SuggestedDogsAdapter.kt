package com.example.doglens.ui.dog

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.doglens.utils.loadBreedPhoto
import com.example.doglens.R
import com.example.doglens.data.model.Dog
import kotlin.math.roundToInt

class SuggestedDogsAdapter(
    private val onDogClick: (Dog) -> Unit
) : RecyclerView.Adapter<SuggestedDogsAdapter.Holder>() {

    private val items = mutableListOf<SuggestedDogRow>()

    fun submitList(newItems: List<SuggestedDogRow>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_suggested_dog, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val image: ImageView = view.findViewById(R.id.imgDog)
        private val name: TextView = view.findViewById(R.id.tvDogName)
        private val description: TextView = view.findViewById(R.id.tvDescription)
        private val weight: TextView = view.findViewById(R.id.tvWeight)
        private val height: TextView = view.findViewById(R.id.tvHeight)
        private val traits: LinearLayout = view.findViewById(R.id.traitContainer)

        fun bind(row: SuggestedDogRow) {
            val dog = row.dog
            name.text = dog.name
            description.text = dog.summary ?: dog.description
            weight.text = range(dog.minWeightKg, dog.maxWeightKg, "kg")
            height.text = range(dog.minHeightCm, dog.maxHeightCm, "cm")

            image.loadBreedPhoto(dog)

            traits.removeAllViews()
            val palette = listOf(
                Pair("#E8F4FF", "#168FF2"),
                Pair("#F2EAFE", "#8653E8"),
                Pair("#E7F8EE", "#139A55")
            )
            row.traits.take(3).forEachIndexed { index, value ->
                val colors = palette[index % palette.size]
                traits.addView(TextView(itemView.context).apply {
                    text = value
                    textSize = 10f
                    setTextColor(Color.parseColor(colors.second))
                    setPadding(dp(7), dp(4), dp(7), dp(4))
                    background = GradientDrawable().apply {
                        setColor(Color.parseColor(colors.first))
                        cornerRadius = dp(16).toFloat()
                    }
                    maxLines = 1
                }, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = dp(5) })
            }

            itemView.setOnClickListener { onDogClick(dog) }
        }

        private fun range(min: Int?, max: Int?, unit: String): String =
            if (min != null && max != null) "$min – $max$unit" else "—"

        private fun dp(value: Int): Int =
            (value * itemView.resources.displayMetrics.density).roundToInt()
    }
}
