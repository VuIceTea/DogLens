package com.example.doglens.data.model

data class RecognitionResult(
    val id: String,
    val userId: String,
    val dogId: String,
    val confidence: Int,
    val imageUrl: String,
    val recognizedAt: String
)