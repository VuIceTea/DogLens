package com.example.doglens.data.repository

import android.net.Uri
import com.example.doglens.data.model.RecognitionResult

interface RecognitionRepository {
    // upload ảnh. onProgress báo % upload/xử lý 0..100.
    // Không nhận diện được -> throw RecognitionFailedException
    suspend fun recognize(imageUri: Uri, onProgress: (Int) -> Unit): RecognitionResult
    suspend fun getHistory(): List<RecognitionResult>
    suspend fun getById(id: String): RecognitionResult?
    suspend fun delete(id: String)
}

class RecognitionFailedException(
    message: String = "Không nhận diện được giống chó"
) : Exception(message)