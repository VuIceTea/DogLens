package com.example.doglens.data.mock

import android.net.Uri
import androidx.annotation.DrawableRes
import com.example.doglens.R
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
    private val history = mutableListOf<RecognitionResult>().apply { addAll(seedHistory()) }

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
            .sortedByDescending { TimeUtils.parseIso(it.recognizedAt) }

    override suspend fun getById(id: String): RecognitionResult? =
        history.firstOrNull { it.id == id }

    override suspend fun delete(id: String) {
        history.removeAll { it.id == id }
    }

    override suspend fun clearHistory() {
        history.removeAll { it.userId == userId }
    }

    // Dữ liệu mẫu: ảnh lấy từ drawable, thời gian lùi dần so với hiện tại
    private fun seedHistory(): List<RecognitionResult> {
        val now = System.currentTimeMillis()
        val hour = 60 * 60 * 1000L

        fun seed(
            id: String,
            dogId: String,
            confidence: Int,
            @DrawableRes image: Int,
            hoursAgo: Long
        ) =
            RecognitionResult(
                id = id,
                userId = userId,
                dogId = dogId,
                confidence = confidence,
                imageUrl = "android.resource://$PACKAGE_NAME/$image",
                recognizedAt = TimeUtils.toIso(now - hoursAgo * hour)
            )

        return listOf(
            seed("rec_001", "golden_retriever", 96, R.drawable.img_golden_retriever, 1),
            seed("rec_002", "corgi", 92, R.drawable.corgi, 3),
            seed("rec_003", "poodle", 89, R.drawable.poodle, 5),
            seed("rec_004", "siberian_husky", 94, R.drawable.siberian_husky, 26),
            seed("rec_005", "pomeranian", 90, R.drawable.dog_small_pomeranian, 31),
            seed("rec_006", "french_bulldog", 93, R.drawable.french_bulldog, 24 * 10)
        )
    }


    companion object {
        private const val MIN_CONFIDENCE = 60
        private const val PACKAGE_NAME = "com.example.doglens"
    }
}