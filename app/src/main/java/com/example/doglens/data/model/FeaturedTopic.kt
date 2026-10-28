package com.example.doglens.data.model

data class FeaturedTopic(
    val id: String,
    val title: String,
    val description: String = "",
    val imageUrl: String? = null,
    val localImageRes: Int? = null,
    val dogIds: List<String> = emptyList(),
    val sortOrder: Int = 0
)