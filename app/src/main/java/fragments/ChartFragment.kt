package com.example.mybussines.fragments

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.annotation.RequiresApi
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.example.mybussines.R
import com.example.mybussines.utils.SimpleChartView
import com.example.mybussines.utils.SimplePieChartView

class ChartFragment : Fragment() {

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chart, container, false)

        setupSpinners(view)
        loadCustomData(view)

        // Rozmycie tła
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val bg1 = view.findViewById<CardView>(R.id.glassCardBg1)
            val bg2 = view.findViewById<CardView>(R.id.glassCardBg2)

            val blurEffect = RenderEffect.createBlurEffect(20f, 20f, Shader.TileMode.CLAMP)
            bg1?.setRenderEffect(blurEffect)
            bg2?.setRenderEffect(blurEffect)
        }

        return view
    }

    private fun loadCustomData(view: View) {
        val prefs = requireContext().getSharedPreferences("chart_data", 0)
        val positions = prefs.getStringSet("positions", emptySet()) ?: emptySet()

        Log.d("CHART_DATA", "Znaleziono ${positions.size} pozycji")

        val chartView = view.findViewById<SimpleChartView>(R.id.simpleChart2)
        val pieChartView = view.findViewById<SimplePieChartView>(R.id.pieChart)

        if (positions.isEmpty()) {
            chartView?.setData(emptyList(), "Brak danych")
            pieChartView?.setData(emptyMap())
            return
        }

        // Dane do wykresu liniowego (WSZYSTKIE pozycje w czasie)
        val lineData = mutableListOf<Pair<String, Float>>()

        // Dane do wykresu kołowego (TYLKO OTWARTE)
        val pieData = mutableMapOf<String, Float>()

        positions.forEach { pos ->
            val parts = pos.split("|")
            if (parts.size >= 7) {
                val name = parts[0]
                val openDate = parts[1]
                val closeDate = parts[2]
                val openPrice = parts[3].toFloatOrNull() ?: 0f
                val closePrice = parts[4].toFloatOrNull() ?: 0f
                val type = parts[5]
                val isStillOpen = parts[6].toBoolean()

                // LineChart: wszystkie pozycje (wartość zainwestowana = cena otwarcia)
                lineData.add(openDate to openPrice)
                if (!isStillOpen) {
                    lineData.add(closeDate to closePrice)
                }

                // PieChart: TYLKO otwarte pozycje (aktualna wartość)
                if (isStillOpen) {
                    pieData[type] = (pieData[type] ?: 0f) + closePrice
                }

                Log.d("CHART_DATA", "$name: $openDate-$closeDate | $openPrice -> $closePrice | $type | otwarte=$isStillOpen")
            }
        }

        // Posortuj dane liniowe po dacie
        lineData.sortBy { it.first }

        // Ustaw dane
        chartView?.setData(lineData.map { it.second }, lineData.joinToString(", ") { it.first })
        pieChartView?.setData(pieData)
    }

    private fun setupSpinners(view: View) {
        val spinner1 = view.findViewById<Spinner>(R.id.spinnerTab1)
        spinner1.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            listOf("Wszystko", "Maklerskie", "Kryptowaluty")
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        val spinner2 = view.findViewById<Spinner>(R.id.spinnerTab2)
        spinner2.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            listOf("3 Miesiące", "6 Miesięcy", "Rok", "Ustaw")
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
    }
}