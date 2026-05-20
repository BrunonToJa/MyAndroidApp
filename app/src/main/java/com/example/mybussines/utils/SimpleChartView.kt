package com.example.mybussines.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

class SimpleChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val linePaint = Paint().apply {
        color = Color.parseColor("#5741D9")
        strokeWidth = 4f
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }

    private val pointPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 24f
        isAntiAlias = true
    }

    private val axisPaint = Paint().apply {
        color = Color.parseColor("#666666")
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private var data: List<Float> = emptyList()
    private var label: String = ""

    fun setData(newData: List<Float>, newLabel: String) {
        data = newData
        label = newLabel
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (width == 0 || height == 0) return

        val paddingLeft = 60f
        val paddingBottom = 50f
        val paddingTop = 40f
        val paddingRight = 20f

        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom

        // Rysuj osie
        canvas.drawLine(paddingLeft, paddingTop, paddingLeft, height - paddingBottom, axisPaint)
        canvas.drawLine(paddingLeft, height - paddingBottom, width - paddingRight, height - paddingBottom, axisPaint)

        // Jeśli brak danych - pokaż "Brak danych"
        if (data.isEmpty()) {
            canvas.drawText("Brak danych", paddingLeft, paddingTop - 10f, textPaint)
            return
        }

        val maxValue = data.maxOrNull() ?: 1f
        val minValue = data.minOrNull() ?: 0f
        val range = maxValue - minValue

        // Wartości na osi Y
        canvas.drawText(maxValue.toInt().toString(), 10f, paddingTop + 10f, textPaint)
        canvas.drawText(minValue.toInt().toString(), 10f, height - paddingBottom, textPaint)

        // Rysuj linię
        val path = Path()
        val points = data.mapIndexed { index, value ->
            val x = paddingLeft + (index.toFloat() / (data.size - 1).coerceAtLeast(1)) * chartWidth
            val y = if (range > 0) {
                paddingTop + chartHeight - ((value - minValue) / range) * chartHeight
            } else {
                paddingTop + chartHeight / 2
            }
            x to y
        }

        points.forEachIndexed { index, (x, y) ->
            if (index == 0) path.moveTo(x, y)
            else {
                val (prevX, prevY) = points[index - 1]
                val midX = (prevX + x) / 2
                path.cubicTo(
                    prevX + (midX - prevX) * 0.3f, prevY,
                    midX + (x - midX) * 0.7f, y,
                    x, y
                )
            }
        }

        canvas.drawPath(path, linePaint)

        // Rysuj kropki
        points.forEach { (x, y) ->
            canvas.drawCircle(x, y, 6f, pointPaint)
        }

        // Etykieta
        canvas.drawText(label, paddingLeft, paddingTop - 10f, textPaint)
    }
}