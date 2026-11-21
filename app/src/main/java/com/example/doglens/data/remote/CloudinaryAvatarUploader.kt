package com.example.doglens.data.remote

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.DataOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class CloudinaryAvatarUploader(context: Context) {

    private val appContext = context.applicationContext

    companion object {
        private const val CLOUD_NAME = "dfass7bhc"
        private const val UPLOAD_PRESET = "doglens_avatar_unsigned"
        private const val MAX_FILE_SIZE = 5 * 1024 * 1024
    }

    suspend fun uploadAvatar(uri: Uri): String =
        withContext(Dispatchers.IO) {

            val resolver = appContext.contentResolver

            val mimeType = resolver.getType(uri)
                ?: throw IOException("Không xác định được định dạng ảnh")

            val extension = when (mimeType) {
                "image/jpeg" -> "jpg"
                "image/png" -> "png"
                "image/webp" -> "webp"
                else -> throw IOException("Chỉ hỗ trợ JPG, PNG và WEBP")
            }

            val imageBytes = resolver.openInputStream(uri)?.use {
                it.readBytes()
            } ?: throw IOException("Không thể đọc ảnh đã chọn")

            if (imageBytes.isEmpty()) {
                throw IOException("Ảnh không có dữ liệu")
            }

            if (imageBytes.size > MAX_FILE_SIZE) {
                throw IOException("Ảnh không được vượt quá 5 MB")
            }

            val boundary = "----DogLens${UUID.randomUUID()}"

            val url = URL(
                "https://api.cloudinary.com/v1_1/" +
                        "$CLOUD_NAME/image/upload"
            )

            val connection =
                url.openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.connectTimeout = 15000
                connection.readTimeout = 30000

                connection.setRequestProperty(
                    "Content-Type",
                    "multipart/form-data; boundary=$boundary"
                )

                DataOutputStream(connection.outputStream).use { output ->

                    fun writeText(value: String) {
                        output.write(value.toByteArray(Charsets.UTF_8))
                    }

                    // Gửi file ảnh
                    writeText("--$boundary\r\n")
                    writeText(
                        "Content-Disposition: form-data; " +
                                "name=\"file\"; filename=\"avatar.$extension\"\r\n"
                    )
                    writeText("Content-Type: $mimeType\r\n\r\n")
                    output.write(imageBytes)
                    writeText("\r\n")

                    // Gửi tên Upload Preset
                    writeText("--$boundary\r\n")
                    writeText(
                        "Content-Disposition: form-data; " +
                                "name=\"upload_preset\"\r\n\r\n"
                    )
                    writeText("$UPLOAD_PRESET\r\n")

                    writeText("--$boundary--\r\n")
                    output.flush()
                }

                val responseCode = connection.responseCode

                val responseStream =
                    if (responseCode in 200..299) {
                        connection.inputStream
                    } else {
                        connection.errorStream
                    }

                val responseText = responseStream
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    .orEmpty()

                if (responseCode !in 200..299) {
                    throw IOException(
                        "Upload ảnh thất bại: $responseText"
                    )
                }

                val secureUrl = JSONObject(responseText)
                    .optString("secure_url")

                if (secureUrl.isBlank()) {
                    throw IOException("Cloudinary không trả về URL ảnh")
                }

                secureUrl

            } finally {
                connection.disconnect()
            }
        }
}