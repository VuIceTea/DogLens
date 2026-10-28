package com.example.doglens.data.model

data class DogUseCase (
    val id: String,
    val name: String,
    val description: String,
    val imageUrl: String? = null,
    val localImageRes: Int? = null,
    val dogIds: List<String> = emptyList(),
    val sortOrder: Int = 0
)