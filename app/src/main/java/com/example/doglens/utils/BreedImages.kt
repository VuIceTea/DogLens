package com.example.doglens.utils

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.example.doglens.R
import com.example.doglens.data.model.Dog

internal fun ImageView.loadBreedPhoto(dog: Dog) {
    val bundled = resources.getIdentifier("breed_photo_${dog.id}", "drawable", context.packageName)
    loadBreedPhoto(dog.imageUrl, dog.localImageRes, bundled)
}

internal fun ImageView.loadBreedPhoto(url: String?, local: Int?, bundled: Int = 0) {
    val placeholder = R.drawable.bg_breed_photo_placeholder
    val fallback = local?.takeUnless { it == R.drawable.ic_detail_paw } ?: placeholder
    val model: Any = when {
        bundled != 0 -> bundled
        !url.isNullOrBlank() -> url
        else -> fallback
    }
    Glide.with(this).load(model).centerCrop()
        .placeholder(placeholder)
        .error(fallback)
        .into(this)
}
