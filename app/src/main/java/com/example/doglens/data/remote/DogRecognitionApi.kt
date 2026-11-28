package com.example.doglens.data.remote

import com.example.doglens.BuildConfig
import com.example.doglens.data.repository.RecognitionFailedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

internal class DogRecognitionApi {
    val isConfigured: Boolean
        get() = BuildConfig.DOG_RECOGNITION_API_URL.isNotBlank()

    private fun endpoint(): URL {
        check(isConfigured) { "Dịch vụ nhận diện ảnh chưa được cấu hình." }
        val url = URL(BuildConfig.DOG_RECOGNITION_API_URL)
        val debugLoopback = BuildConfig.DEBUG && url.protocol == "http" &&
            (url.host == "127.0.0.1" || url.host == "localhost")
        check((url.protocol == "https" || debugLoopback) && url.userInfo == null) {
            "Dịch vụ nhận diện ảnh chưa được cấu hình."
        }
        return url
    }

    suspend fun recognize(bytes: ByteArray, mimeType: String, token: String): Pair<String, Int> =
        withContext(Dispatchers.IO) {
            val connection = endpoint().openConnection() as HttpURLConnection
            try {
                connection.instanceFollowRedirects = false
                connection.requestMethod = "POST"
                connection.connectTimeout = 12_000
                connection.readTimeout = 40_000
                connection.doOutput = true
                connection.setFixedLengthStreamingMode(bytes.size)
                connection.setRequestProperty("Content-Type", mimeType)
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.setRequestProperty("Accept", "application/json")
                connection.outputStream.use { it.write(bytes) }
                if (connection.responseCode == 422) {
                    val detail = connection.errorStream?.bufferedReader()?.use { reader ->
                        runCatching { JSONObject(reader.readText()).optString("detail") }.getOrNull()
                    }
                    throw RecognitionFailedException(dogDetected = detail == "uncertain_breed")
                }
                check(connection.responseCode in 200..299) { "Không thể kết nối dịch vụ nhận diện." }
                val result = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                val confidence = result.getInt("confidence")
                require(confidence in 0..100) { "API trả về độ tin cậy không hợp lệ." }
                val dogId = result.optString("dogId")
                if (dogId.isBlank() || confidence < 60) throw RecognitionFailedException(dogDetected = true)
                check(dogId.matches(Regex("[A-Za-z0-9_-]{1,100}"))) { "API trả về mã giống chó không hợp lệ." }
                dogId to confidence
            } finally {
                connection.disconnect()
            }
        }
}
