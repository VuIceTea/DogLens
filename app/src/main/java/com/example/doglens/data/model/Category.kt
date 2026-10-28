package com.example.doglens.data.model

data class Category (
    val id: String,
    val name: String,
    val description: String,
    val imageUrl: String? = null,

    // ảnh tạm thời để mock UI
    val localImageRes: Int? = null,
    val backgroundColorHex: String = "#E3F0FD",
    val sortOrder:Int = 0
)