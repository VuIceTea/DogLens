package com.example.doglens.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.google.android.play.core.integrity.p

object ImageUtils {
    // Lưu ảnh vào Pictures/DogLens. Cần Android 10+ (không phải xin quyền). Gọi trên Dispatchers.IO.
    fun saveToGallery(context: Context, bitmap: Bitmap): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false

        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "DogLens_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/DogLens")
        }
        val target =
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false

        return try {
            resolver.openOutputStream(target)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            } ?: false
        } catch (e: Exception) {
            resolver.delete(target, null, null)
            false
        }
    }
}