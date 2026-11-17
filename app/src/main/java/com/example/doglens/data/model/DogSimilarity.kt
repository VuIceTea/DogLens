package com.example.doglens.data.model

data class DogSimilarity(
    // giống đang xem
    val dogId: String,
    // giống tương tự
    val similarDogId: String,
    val similarityPercent: Int
)
