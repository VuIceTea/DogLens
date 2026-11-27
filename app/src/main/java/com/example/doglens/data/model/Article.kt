package com.example.doglens.data.model

data class Article(
    val id: String,
    val categoryId: String,
    val title: String,
    val summary: String = "",
    val imageUrl: String? = null,
    val localImageRes: Int? = null,
    val publishedAt: String,
    val readMinutes: Int = 0,
    val viewCount: Int = 0,
    val sources: Map<String, String> = emptyMap()
)
