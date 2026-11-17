package com.example.doglens.data.model

data class ArticleSection(
    val id: String,
    val articleId: String,
    val sortIndex: Int,
    val heading: String,
    val body: String,
    val imageUrl: String? = null
)