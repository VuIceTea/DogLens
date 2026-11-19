package com.example.doglens.data.model

data class DogImage(
    val id: String,
    val dogId: String,
    val imageUrl: String? = null,
    val localImageRes: Int? = null,
    val sortOrder: Int = 0
)