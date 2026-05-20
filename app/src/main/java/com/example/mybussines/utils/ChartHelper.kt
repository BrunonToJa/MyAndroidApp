package com.example.mybussines.utils

import android.graphics.Color
import android.util.Log
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

object ChartHelper {

    private const val TAG = "ChartHelper"

    fun setupLineChart(lineChart: LineChart?) {
        Log.d(TAG, "setupLineChart called, lineChart = $lineChart")

        if (lineChart == null) {
            Log.e(TAG, "LineChart is null!")
            return
        }

        Log.d(TAG, "LineChart width: ${lineChart.width}, height: ${lineChart.height}")

        val entries = listOf(
            Entry(0f, 42f),
            Entry(1f, 45f),
            Entry(2f, 40f),
            Entry(3f, 48f),
            Entry(4f, 52f)
        )

        val dataSet = LineDataSet(entries, "BTC").apply {
            color = Color.parseColor("#5741D9")
            setDrawCircles(true)
            circleRadius = 4f
            lineWidth = 3f
            valueTextColor = Color.WHITE
            valueTextSize = 12f
        }

        lineChart.apply {  // teraz lineChart jest non-null
            data = LineData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            setBackgroundColor(Color.TRANSPARENT)

            xAxis.apply {
                textColor = Color.WHITE
                setDrawGridLines(false)
            }

            axisLeft.apply {
                textColor = Color.WHITE
                setDrawGridLines(false)
            }
            axisRight.isEnabled = false

            invalidate()
            Log.d(TAG, "LineChart setup complete")
        }
    }
}