package com.example.doglens.data.mock

import android.net.Uri
import com.example.doglens.data.model.RecognitionResult
import com.example.doglens.data.repository.RecognitionFailedException
import com.example.doglens.data.repository.RecognitionRepository
import com.example.doglens.utils.TimeUtils
import kotlinx.coroutines.delay
import java.util.UUID

// Giả lập: chạy 0 -> 100% trong ~6 giây, luôn ra Golden Retriever 96%.
// Đặt shouldFail = true để thử màn "Không nhận diện được".
class MockRecognitionRepository(
    var shouldFail: Boolean = false
) : RecognitionRepository {

    private val userId = ProfileMockData.profile.user.id
    private val history = mutableListOf<RecognitionResult>()

    override suspend fun recognize(
        imageUri: Uri,
        onProgress: (Int) -> Unit
    ): RecognitionResult {
        for (percent in 0..100) {
            if (shouldFail && percent == 75) throw RecognitionFailedException()
            onProgress(percent)
            delay(60)
        }
        val confidence = 96
        // Độ tin cậy thấp -> coi như không nhận diện được
        if (confidence < MIN_CONFIDENCE) throw RecognitionFailedException()

        val result = RecognitionResult(
            id = UUID.randomUUID().toString(),
            userId = userId,
            dogId = "golden_retriever",
            confidence = confidence,
            imageUrl = imageUri.toString(),
            recognizedAt = TimeUtils.nowIso()
        )
        history.add(0, result)
        return result
    }

    override suspend fun getHistory(): List<RecognitionResult> =
        history.filter { it.userId == userId }

    override suspend fun getById(id: String): RecognitionResult? =
        history.firstOrNull { it.id == id }

    override suspend fun delete(id: String) {
        history.removeAll { it.id == id }
    }

    companion object {
        private const val MIN_CONFIDENCE = 60
    }
}