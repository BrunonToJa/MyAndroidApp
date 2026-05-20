package com.example.mybussines.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class SimplePieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paintBTC = Paint().apply {
        color = Color.parseColor("#5741D9")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val paintETH = Paint().apply {
        color = Color.parseColor("#3B82F6")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val paintOther = Paint().apply {
        color = Color.parseColor("#6B7280")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 24f
        isAntiAlias = true
    }

    private var data: Map<String, Float> = emptyMap()

    fun setData(newData: Map<String, Float>) {
        data = newData
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (width == 0 || height == 0) return

        val padding = 40f
        val size = minOf(width, height) - 2 * padding
        val left = (width - size) / 2
        val top = (height - size) / 2
        val rect = RectF(left, top, left + size, top + size)

        if (data.isEmpty()) {
            canvas.drawText("Brak danych", padding, padding - 10f, textPaint)
            return
        }

        val total = data.values.sum()
        var startAngle = -90f

        val colors = listOf(paintBTC, paintETH, paintOther)

        data.entries.forEachIndexed { index, (label, value) ->
            val sweepAngle = (value / total) * 360f
            val paint = colors.getOrElse(index) { paintOther }

            canvas.drawArc(rect, startAngle, sweepAngle, true, paint)

            val midAngle = Math.toRadians((startAngle + sweepAngle / 2).toDouble())
            val labelRadius = size / 2 * 0.7f
            val labelX = (width / 2 + Math.cos(midAngle) * labelRadius).toFloat()
            val labelY = (height / 2 + Math.sin(midAngle) * labelRadius).toFloat()

            val percentText = "${(value / total * 100).toInt()}%"
            canvas.drawText(percentText, labelX - 20f, labelY, textPaint)

            startAngle += sweepAngle
        }
    }
}