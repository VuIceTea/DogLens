package com.example.doglens.ui.article

import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.example.doglens.MainActivity
import com.example.doglens.R
import com.example.doglens.data.model.Article
import com.example.doglens.utils.loadBreedPhoto

internal fun Fragment.openArticleCatalog(breedId: String? = null) {
    (requireActivity() as MainActivity).openScreen(
        ViewedArticlesFragment.catalog(breedId), R.color.explore_background
    )
}

internal fun ImageView.loadArticlePhoto(article: Article) {
    val bundled = article.breedId?.let {
        resources.getIdentifier("breed_photo_$it", "drawable", context.packageName)
    } ?: 0
    loadBreedPhoto(article.imageUrl, article.localImageRes, bundled)
}
