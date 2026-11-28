package com.example.doglens.utils

import android.graphics.BitmapFactory
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.example.doglens.R
import com.example.doglens.data.model.Dog
import java.util.concurrent.ConcurrentHashMap

private val usablePhotoResources = ConcurrentHashMap<Int, Boolean>()
private val commonsThumbnail = Regex(
    "^https://(?:thumb|upload)\\.wikimedia\\.org/wikipedia/commons/thumb/[^?#]+/\\d+px-[^/?#]+$"
)
private val thumbnailWidth = Regex("/\\d+px-")

internal fun ImageView.loadBreedPhoto(dog: Dog) {
    val bundled = resources.getIdentifier("breed_photo_${dog.id}", "drawable", context.packageName)
    loadBreedPhoto(dog.imageUrl, dog.localImageRes, bundled)
}

internal fun ImageView.loadBreedPhoto(url: String?, local: Int?, bundled: Int = 0) {
    val placeholder = R.drawable.bg_breed_photo_placeholder
    val fallback = local?.takeUnless { it == R.drawable.ic_detail_paw } ?: placeholder
    val hasBundledPhoto = bundled != 0 && usablePhotoResources.getOrPut(bundled) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(resources, bundled, bounds)
        bounds.outWidth > 1 && bounds.outHeight > 1
    }
    val model: Any = when {
        hasBundledPhoto -> bundled
        !url.isNullOrBlank() -> url
        else -> fallback
    }
    val glide = Glide.with(this)
    val request = glide.load(model).centerCrop().placeholder(placeholder).error(fallback)
    if (model is String && commonsThumbnail.matches(model)) {
        val smallerPhoto = model.replace(thumbnailWidth, "/500px-")
        if (smallerPhoto != model) {
            request.error(glide.load(smallerPhoto).centerCrop().error(fallback))
        }
    }
    request.into(this)
}
