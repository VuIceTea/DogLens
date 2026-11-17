package com.example.doglens.data.model

data class RecognitionHistoryItem(
    val result: RecognitionResult,
    val dog: Dog,
    val isFavorite: Boolean
)
