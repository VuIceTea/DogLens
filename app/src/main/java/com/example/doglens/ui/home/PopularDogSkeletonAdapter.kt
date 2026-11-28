package com.example.doglens.ui.home

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.doglens.R

internal class PopularDogSkeletonAdapter : RecyclerView.Adapter<PopularDogSkeletonAdapter.Holder>() {
    override fun getItemCount() = 5

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_popular_dog_skeleton, parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) = Unit

    override fun onViewAttachedToWindow(holder: Holder) {
        holder.pulse.start()
    }

    override fun onViewDetachedFromWindow(holder: Holder) {
        holder.pulse.cancel()
        holder.itemView.alpha = 1f
    }

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val pulse = ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0.5f).apply {
            duration = 700L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
        }
    }
}
