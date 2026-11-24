package com.example.doglens.ui.camera

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

class CropGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(115, 255, 255, 255)
        strokeWidth = resources.displayMetrics.density
        style = Paint.Style.STROKE
    }

    private val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = 4f * resources.displayMetrics.density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        style = Paint.Style.STROKE
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        canvas.drawLine(w / 3f, 0f, w / 3f, h, gridPaint)
        canvas.drawLine(w * 2f / 3f, 0f, w * 2f / 3f, h, gridPaint)
        canvas.drawLine(0f, h / 3f, w, h / 3f, gridPaint)
        canvas.drawLine(0f, h * 2f / 3f, w, h * 2f / 3f, gridPaint)

        val inset = 3f * resources.displayMetrics.density
        val length = 24f * resources.displayMetrics.density
        val path = Path().apply {
            moveTo(inset, inset + length); lineTo(inset, inset); lineTo(inset + length, inset)
            moveTo(w - inset - length, inset); lineTo(w - inset, inset); lineTo(w - inset, inset + length)
            moveTo(inset, h - inset - length); lineTo(inset, h - inset); lineTo(inset + length, h - inset)
            moveTo(w - inset - length, h - inset); lineTo(w - inset, h - inset); lineTo(w - inset, h - inset - length)
        }
        canvas.drawPath(path, cornerPaint)
    }
}
