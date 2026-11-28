package com.example.doglens.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorSpace
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

private const val MAX_PHOTO_EDGE = 1600

internal fun decodeDogPhoto(context: Context, uri: Uri): Bitmap {
    val resolver = context.contentResolver
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        return ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.setTargetColorSpace(ColorSpace.get(ColorSpace.Named.SRGB))
            val size = info.size
            val scale = MAX_PHOTO_EDGE.toFloat() / maxOf(size.width, size.height)
            if (scale < 1f) {
                decoder.setTargetSize(
                    (size.width * scale).roundToInt().coerceAtLeast(1),
                    (size.height * scale).roundToInt().coerceAtLeast(1)
                )
            }
        }
    }

    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    check(options.outWidth > 0 && options.outHeight > 0) { "Không đọc được ảnh." }
    options.inJustDecodeBounds = false
    options.inSampleSize = 1
    while (maxOf(options.outWidth, options.outHeight) / options.inSampleSize > MAX_PHOTO_EDGE) {
        options.inSampleSize *= 2
    }
    val exif = resolver.openInputStream(uri)?.use {
        try {
            ExifInterface(it)
        } catch (_: java.io.IOException) {
            null
        }
    }
    val photo = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        ?: error("Không đọc được ảnh.")
    val matrix = Matrix().apply {
        if (exif?.isFlipped == true) setScale(-1f, 1f)
        postRotate(exif?.rotationDegrees?.toFloat() ?: 0f)
    }
    if (matrix.isIdentity) return photo
    return Bitmap.createBitmap(photo, 0, 0, photo.width, photo.height, matrix, true)
        .also { if (it !== photo) photo.recycle() }
}

internal fun Bitmap.encodeRecognitionJpeg(): ByteArray = ByteArrayOutputStream().use { output ->
    check(compress(Bitmap.CompressFormat.JPEG, 90, output)) { "Không thể lưu ảnh nhận diện." }
    output.toByteArray()
}
