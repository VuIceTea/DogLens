package com.example.doglens.data.model

data class ExploreBanner (
    val title: String,
    val description: String,
    val actionText: String,
    val imageUrl: String? = null,
    val localImageRes: Int? = null
)