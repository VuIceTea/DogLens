package com.example.doglens.data.model

data class ArticleCategory(
    val id: String,
    val name: String,
    val colorHex: String = "#E3F0FD",
    val sortOrder: Int = 0
)
