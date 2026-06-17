package com.example.mybussines.fragments

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.mybussines.R
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TreeMap

class HomeFragment : Fragment() {

    private lateinit var tvHeaderProfit: TextView

    private lateinit var tvTotalEntry: TextView
    private lateinit var tvTotalClose: TextView
    private lateinit var tvTotalProfit: TextView
    private lateinit var tvTotalPercent: TextView

    private lateinit var tvEmptyMonthly: TextView
    private lateinit var monthlyContainer: LinearLayout

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
        isLenient = false
    }

    private val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvHeaderProfit = view.findViewById(R.id.tvHeaderProfit)

        tvTotalEntry = view.findViewById(R.id.tvTotalEntry)
        tvTotalClose = view.findViewById(R.id.tvTotalClose)
        tvTotalProfit = view.findViewById(R.id.tvTotalProfit)
        tvTotalPercent = view.findViewById(R.id.tvTotalPercent)

        tvEmptyMonthly = view.findViewById(R.id.tvEmptyMonthly)
        monthlyContainer = view.findViewById(R.id.monthlyContainer)

        loadStatistics()
    }

    override fun onResume() {
        super.onResume()
        loadStatistics()
    }

    private fun loadStatistics() {
        val prefs = requireContext().getSharedPreferences("chart_data", 0)
        val savedPositions = prefs.getStringSet("positions", emptySet()) ?: emptySet()

        val closedPositions = savedPositions.mapNotNull { raw ->
            parsePosition(raw)
        }.filter { position ->
            !position.isStillOpen
        }

        updateTotalStatistics(closedPositions)
        updateMonthlyStatistics(closedPositions)
    }

    private fun updateTotalStatistics(positions: List<HomePosition>) {
        if (positions.isEmpty()) {
            tvHeaderProfit.text = "BD"
            tvHeaderProfit.setTextColor(Color.parseColor("#B0B0B0"))

            tvTotalEntry.text = "0.00"
            tvTotalClose.text = "0.00"
            tvTotalProfit.text = "0.00"
            tvTotalPercent.text = "BD"

            tvTotalProfit.setTextColor(Color.parseColor("#B0B0B0"))
            tvTotalPercent.setTextColor(Color.parseColor("#B0B0B0"))
            return
        }

        val totalEntry = positions.sumOf { it.openPrice.toDouble() }.toFloat()
        val totalClose = positions.sumOf { it.closePrice.toDouble() }.toFloat()
        val totalProfit = totalClose - totalEntry

        val totalPercent = if (totalEntry > 0f) {
            (totalProfit / totalEntry) * 100f
        } else {
            0f
        }

        val profitColor = if (totalProfit >= 0f) {
            Color.parseColor("#10B981")
        } else {
            Color.parseColor("#EF4444")
        }

        tvHeaderProfit.text = "${if (totalPercent >= 0f) "+" else ""}${formatPercent(totalPercent)}%"
        tvHeaderProfit.setTextColor(profitColor)

        tvTotalEntry.text = formatMoney(totalEntry)
        tvTotalClose.text = formatMoney(totalClose)
        tvTotalProfit.text = "${if (totalProfit >= 0f) "+" else ""}${formatMoney(totalProfit)}"
        tvTotalPercent.text = "${if (totalPercent >= 0f) "+" else ""}${formatPercent(totalPercent)}%"

        tvTotalProfit.setTextColor(profitColor)
        tvTotalPercent.setTextColor(profitColor)
    }

    private fun updateMonthlyStatistics(positions: List<HomePosition>) {
        monthlyContainer.removeAllViews()

        if (positions.isEmpty()) {
            tvEmptyMonthly.visibility = View.VISIBLE
            monthlyContainer.visibility = View.GONE
            return
        }

        tvEmptyMonthly.visibility = View.GONE
        monthlyContainer.visibility = View.VISIBLE

        val monthlyMap = TreeMap<String, MonthlyStats>()

        positions.forEach { position ->
            val monthKey = getMonthKey(position.closeDate) ?: return@forEach

            val stats = monthlyMap[monthKey] ?: MonthlyStats(month = monthKey)

            stats.count += 1
            stats.totalEntry += position.openPrice
            stats.totalClose += position.closePrice

            monthlyMap[monthKey] = stats
        }

        monthlyMap.toSortedMap(compareByDescending { it }).values.forEach { stats ->
            monthlyContainer.addView(createMonthlyRow(stats))
        }
    }

    private fun createMonthlyRow(stats: MonthlyStats): View {
        val context = requireContext()

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(10), 0, dp(10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val profit = stats.totalClose - stats.totalEntry

        val percent = if (stats.totalEntry > 0f) {
            (profit / stats.totalEntry) * 100f
        } else {
            0f
        }

        val profitColor = if (profit >= 0f) {
            Color.parseColor("#10B981")
        } else {
            Color.parseColor("#EF4444")
        }

        val topLine = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        val tvMonth = TextView(context).apply {
            text = stats.month
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val tvPercent = TextView(context).apply {
            text = "${if (percent >= 0f) "+" else ""}${formatPercent(percent)}%"
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
        }

        topLine.addView(tvMonth)
        topLine.addView(tvPercent)

        val detailLine1 = TextView(context).apply {
            text = "Wejście: ${formatMoney(stats.totalEntry)}   Zamknięcie: ${formatMoney(stats.totalClose)}"
            textSize = 13f
            setPadding(0, dp(4), 0, 0)
        }

        val detailLine2 = TextView(context).apply {
            text = "Zysk/strata: ${if (profit >= 0f) "+" else ""}${formatMoney(profit)}   Liczba: ${stats.count}"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(2), 0, 0)
        }

        val divider = View(context).apply {
            setBackgroundColor(Color.parseColor("#22FFFFFF"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
            ).apply {
                topMargin = dp(10)
            }
        }

        row.addView(topLine)
        row.addView(detailLine1)
        row.addView(detailLine2)
        row.addView(divider)

        return row
    }

    private fun parsePosition(raw: String): HomePosition? {
        val parts = raw.split("|")

        if (parts.size < 7) {
            return null
        }

        return try {
            HomePosition(
                name = parts[0],
                openDate = parts[1],
                closeDate = parts[2],
                openPrice = parts[3].replace(",", ".").toFloatOrNull() ?: return null,
                closePrice = parts[4].replace(",", ".").toFloatOrNull() ?: return null,
                accountType = parts[5],
                isStillOpen = parts[6].toBoolean()
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun getMonthKey(dateText: String): String? {
        return try {
            val date = dateFormat.parse(dateText) ?: return null
            monthFormat.format(date)
        } catch (e: Exception) {
            null
        }
    }

    private fun formatMoney(value: Float): String {
        return String.format(Locale.getDefault(), "%.2f", value)
    }

    private fun formatPercent(value: Float): String {
        return String.format(Locale.getDefault(), "%.2f", value)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private data class HomePosition(
        val name: String,
        val openDate: String,
        val closeDate: String,
        val openPrice: Float,
        val closePrice: Float,
        val accountType: String,
        val isStillOpen: Boolean
    )

    private data class MonthlyStats(
        val month: String,
        var count: Int = 0,
        var totalEntry: Float = 0f,
        var totalClose: Float = 0f
    )
}