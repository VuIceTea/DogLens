package com.example.doglens.data.local

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.example.doglens.data.repository.RecognitionFailedException
import org.json.JSONArray
import org.json.JSONObject
import java.nio.FloatBuffer
import kotlin.math.exp
import kotlin.math.roundToInt

internal class LocalDogRecognition(context: Context) {
    private val assets = context.applicationContext.assets
    private val environment by lazy { OrtEnvironment.getEnvironment() }
    private val detector by lazy { loadModel("yolox_s.onnx") }
    private val classifier by lazy { loadModel("breed-classifier-int8.onnx") }
    private val temperature by lazy {
        val metadata = JSONObject(assets.open("recognition/model-metadata.json").bufferedReader().use { it.readText() })
        metadata.getJSONObject("calibration").getDouble("temperature")
    }
    private val breedIds by lazy {
        val catalog = JSONArray(assets.open("recognition/breed-catalog.json").bufferedReader().use { it.readText() })
        List(catalog.length()) { catalog.optJSONObject(it)?.getString("id") }
    }

    fun recognize(photo: Bitmap, onProgress: (Int) -> Unit): Pair<String, Int> {
        val bounds = detectDog(photo) ?: throw RecognitionFailedException()
        onProgress(60)
        val logits = classify(photo, bounds)
        check(logits.size == breedIds.size && logits.all { it.isFinite() }) {
            "Không đọc được kết quả mô hình nhận diện."
        }

        val ranked = logits.indices.sortedByDescending { logits[it] }
        val first = ranked[0]
        val second = ranked[1]
        val dogId = breedIds[first] ?: throw RecognitionFailedException()
        val peak = logits[first].toDouble()
        val total = logits.sumOf { exp((it - peak) / temperature) }
        val score = 1.0 / total
        val runnerUp = exp((logits[second] - peak) / temperature) / total
        if (score < 0.65 || score - runnerUp < 0.15) throw RecognitionFailedException()
        return dogId to (score * 100).roundToInt().coerceAtMost(99)
    }

    private fun detectDog(photo: Bitmap): RectF? {
        findDog(photo, RectF(0f, 0f, photo.width.toFloat(), photo.height.toFloat()))?.let { return it }

        // Match the backend's close-up passes. Each window still requires a dog detection.
        val landscape = photo.width >= photo.height
        val width = (photo.width * if (landscape) 0.55f else 0.8f).roundToInt()
        val height = (photo.height * if (landscape) 0.8f else 0.55f).roundToInt()
        if (minOf(width, height) < 24) return null

        for (position in floatArrayOf(0.55f, 0.45f)) {
            val left = (photo.width * (if (landscape) position else 0.4f) - width / 2f).roundToInt()
            val top = (photo.height * (if (landscape) 0.4f else position) - height / 2f).roundToInt()
            val window = RectF(left.toFloat(), top.toFloat(), (left + width).toFloat(), (top + height).toFloat())
            findDog(photo, window)?.let { return it }
        }
        return null
    }

    private fun findDog(photo: Bitmap, window: RectF): RectF? {
        val size = 640
        val ratio = size / maxOf(window.width(), window.height())
        val width = (window.width() * ratio).toInt().coerceIn(1, size)
        val height = (window.height() * ratio).toInt().coerceIn(1, size)
        val pixels = modelPixels(photo, window, size, width, height)
        val plane = pixels.size
        val bgr = FloatArray(plane * 3)
        pixels.forEachIndexed { index, pixel ->
            bgr[index] = (pixel and 255).toFloat()
            bgr[index + plane] = (pixel shr 8 and 255).toFloat()
            bgr[index + 2 * plane] = (pixel shr 16 and 255).toFloat()
        }

        return OnnxTensor.createTensor(
            environment, FloatBuffer.wrap(bgr), longArrayOf(1, 3, size.toLong(), size.toLong())
        ).use { tensor ->
            detector.run(mapOf(detector.inputNames.single() to tensor)).use { output ->
                @Suppress("UNCHECKED_CAST")
                val predictions = (output[0].value as Array<Array<FloatArray>>)[0]
                decodeDogBox(predictions, window, width, height, size)
            }
        }
    }

    private fun decodeDogBox(
        predictions: Array<FloatArray>,
        window: RectF,
        scaledWidth: Int,
        scaledHeight: Int,
        size: Int
    ): RectF? {
        val dogClass = 5 + 16 // First five values are the box and objectness. COCO class 16 is dog.
        val scaleX = window.width() / scaledWidth
        val scaleY = window.height() / scaledHeight
        var bestScore = 0.45f
        var bestBox: RectF? = null
        var index = 0
        for (stride in intArrayOf(8, 16, 32)) {
            val gridSize = size / stride
            repeat(gridSize * gridSize) { cell ->
                val row = predictions[index++]
                val score = row[4] * row[dogClass]
                if (!score.isFinite() || score < bestScore) return@repeat
                if ((5 until row.size).maxByOrNull { row[it] } != dogClass) return@repeat

                val cx = (row[0] + cell % gridSize) * stride
                val cy = (row[1] + cell / gridSize) * stride
                val halfWidth = exp(row[2]) * stride / 2
                val halfHeight = exp(row[3]) * stride / 2
                val left = ((cx - halfWidth) * scaleX + window.left).coerceIn(window.left, window.right)
                val top = ((cy - halfHeight) * scaleY + window.top).coerceIn(window.top, window.bottom)
                val right = ((cx + halfWidth) * scaleX + window.left).coerceIn(window.left, window.right)
                val bottom = ((cy + halfHeight) * scaleY + window.top).coerceIn(window.top, window.bottom)
                if (right - left >= 24 && bottom - top >= 24) {
                    bestBox = RectF(left, top, right, bottom)
                    bestScore = score
                }
            }
        }
        return bestBox
    }

    private fun classify(photo: Bitmap, bounds: RectF): FloatArray {
        val size = 224
        val side = maxOf(bounds.width(), bounds.height()) * 1.18f
        val left = bounds.centerX() - side / 2
        val top = bounds.centerY() - side / 2
        val pixels = modelPixels(photo, RectF(left, top, left + side, top + side), size, size, size)
        val plane = pixels.size
        val values = FloatArray(plane * 3)
        pixels.forEachIndexed { index, pixel ->
            values[index] = (((pixel shr 16 and 255) / 255f) - 0.485f) / 0.229f
            values[plane + index] = (((pixel shr 8 and 255) / 255f) - 0.456f) / 0.224f
            values[plane * 2 + index] = (((pixel and 255) / 255f) - 0.406f) / 0.225f
        }
        return OnnxTensor.createTensor(
            environment, FloatBuffer.wrap(values), longArrayOf(1, 3, size.toLong(), size.toLong())
        ).use { tensor ->
            classifier.run(mapOf(classifier.inputNames.single() to tensor)).use { output ->
                @Suppress("UNCHECKED_CAST")
                (output[0].value as Array<FloatArray>)[0]
            }
        }
    }

    private fun modelPixels(photo: Bitmap, window: RectF, size: Int, width: Int, height: Int): IntArray {
        val input = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        return try {
            input.eraseColor(Color.rgb(114, 114, 114))
            val scaleX = width / window.width()
            val scaleY = height / window.height()
            Canvas(input).apply {
                clipRect(0, 0, width, height)
                scale(scaleX, scaleY)
                translate(-window.left, -window.top)
                drawBitmap(photo, null, RectF(0f, 0f, photo.width.toFloat(), photo.height.toFloat()),
                    Paint(Paint.FILTER_BITMAP_FLAG))
            }
            IntArray(size * size).also { input.getPixels(it, 0, size, 0, 0, size, size) }
        } finally {
            input.recycle()
        }
    }

    private fun loadModel(name: String): OrtSession {
        val model = assets.open("recognition/$name").use { it.readBytes() }
        return OrtSession.SessionOptions().use { options ->
            options.setIntraOpNumThreads(2)
            environment.createSession(model, options)
        }
    }
}
