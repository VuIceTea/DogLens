package com.example.doglens.data.model

data class Dog (
    val id: String,
    val name: String,
    val description: String,
    val imageUrl: String? = null,
    val localImageRes: Int? = null,
    val backgroundColorHex: String = "#E3F0FD",
    val sortOrder: Int = 0,

    val sizeCategoryId: String = "medium",
    val minWeightKg: Int? = null,
    val maxWeightKg: Int? = null,
    val minHeightCm: Int? = null,
    val maxHeightCm: Int? = null
)