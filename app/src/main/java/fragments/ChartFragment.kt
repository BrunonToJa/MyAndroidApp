package com.example.mybussines.fragments

import android.app.DatePickerDialog
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.example.mybussines.R
import com.example.mybussines.utils.PortfolioFilterPrefs
import com.example.mybussines.utils.SimpleChartView
import com.example.mybussines.utils.SimplePieChartView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TreeMap

class ChartFragment : Fragment() {

    private var currentTypeFilter = PortfolioFilterPrefs.DEFAULT_TYPE_FILTER
    private var currentTimeFilter = PortfolioFilterPrefs.DEFAULT_TIME_FILTER

    private var customDateFrom: Date? = null
    private var customDateTo: Date? = null

    private var ignoreSpinnerEvents = false

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
        isLenient = false
    }

    private val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chart, container, false)

        loadSavedFilters()
        setupSpinners(view)
        loadCustomData(view)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val bg1 = view.findViewById<CardView>(R.id.glassCardBg1)
            val bg2 = view.findViewById<CardView>(R.id.glassCardBg2)

            val blurEffect = RenderEffect.createBlurEffect(20f, 20f, Shader.TileMode.CLAMP)

            bg1?.setRenderEffect(blurEffect)
            bg2?.setRenderEffect(blurEffect)
        }

        return view
    }

    override fun onResume() {
        super.onResume()

        view?.let {
            loadSavedFilters()
            syncSpinnerSelections(it)
            loadCustomData(it)
        }
    }

    private fun loadSavedFilters() {
        currentTypeFilter = PortfolioFilterPrefs.getTypeFilter(requireContext())
        currentTimeFilter = PortfolioFilterPrefs.getTimeFilter(requireContext())
        customDateFrom = PortfolioFilterPrefs.getCustomDateFrom(requireContext())
        customDateTo = PortfolioFilterPrefs.getCustomDateTo(requireContext())
    }

    private fun setupSpinners(view: View) {
        setupTypeSpinner(view)
        setupTimeSpinner(view)
        syncSpinnerSelections(view)
    }

    private fun setupTypeSpinner(view: View) {
        val spinner1 = view.findViewById<Spinner>(R.id.spinnerTab1)

        val typeOptions = mutableListOf(PortfolioFilterPrefs.TYPE_ALL)

        try {
            val accountTypes = resources.getStringArray(R.array.account_types).toList()

            accountTypes.forEach { type ->
                if (!typeOptions.contains(type)) {
                    typeOptions.add(type)
                }
            }
        } catch (e: Exception) {
            Log.w("CHART_DATA", "Nie znaleziono R.array.account_types", e)

            if (!typeOptions.contains("Maklerskie")) {
                typeOptions.add("Maklerskie")
            }

            if (!typeOptions.contains("Kryptowaluty")) {
                typeOptions.add("Kryptowaluty")
            }
        }

        spinner1.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            typeOptions
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        spinner1.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                selectedView: View?,
                position: Int,
                id: Long
            ) {
                if (ignoreSpinnerEvents) return

                currentTypeFilter = typeOptions[position]

                PortfolioFilterPrefs.saveTypeFilter(requireContext(), currentTypeFilter)

                loadCustomData(view)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupTimeSpinner(view: View) {
        val spinner2 = view.findViewById<Spinner>(R.id.spinnerTab2)
        val timeOptions = PortfolioFilterPrefs.timeOptions

        spinner2.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            timeOptions
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        spinner2.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                selectedView: View?,
                position: Int,
                id: Long
            ) {
                if (ignoreSpinnerEvents) return

                currentTimeFilter = timeOptions[position]

                PortfolioFilterPrefs.saveTimeFilter(requireContext(), currentTimeFilter)

                if (currentTimeFilter == PortfolioFilterPrefs.TIME_CUSTOM) {
                    showCustomDateRangeDialog(view)
                } else {
                    loadCustomData(view)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun syncSpinnerSelections(view: View) {
        val spinner1 = view.findViewById<Spinner>(R.id.spinnerTab1)
        val spinner2 = view.findViewById<Spinner>(R.id.spinnerTab2)

        if (spinner1.adapter == null || spinner2.adapter == null) return

        ignoreSpinnerEvents = true

        val typeIndex = findSpinnerIndex(spinner1, currentTypeFilter)
        if (typeIndex >= 0) {
            spinner1.setSelection(typeIndex, false)
        }

        val timeIndex = findSpinnerIndex(spinner2, currentTimeFilter)
        if (timeIndex >= 0) {
            spinner2.setSelection(timeIndex, false)
        }

        ignoreSpinnerEvents = false
    }

    private fun findSpinnerIndex(spinner: Spinner, value: String): Int {
        for (i in 0 until spinner.adapter.count) {
            if (spinner.adapter.getItem(i).toString() == value) {
                return i
            }
        }

        return -1
    }

    private fun showCustomDateRangeDialog(rootView: View) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_custom_date_range, null)

        val etDateFrom = dialogView.findViewById<EditText>(R.id.etDateFrom)
        val etDateTo = dialogView.findViewById<EditText>(R.id.etDateTo)

        customDateFrom?.let {
            etDateFrom.setText(dateFormat.format(it))
        }

        customDateTo?.let {
            etDateTo.setText(dateFormat.format(it))
        }

        etDateFrom.setOnClickListener {
            showDatePicker(etDateFrom)
        }

        etDateTo.setOnClickListener {
            showDatePicker(etDateTo)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Wybierz zakres dat zamknięcia")
            .setView(dialogView)
            .setPositiveButton("Zastosuj") { _, _ ->
                val fromText = etDateFrom.text.toString().trim()
                val toText = etDateTo.text.toString().trim()

                if (fromText.isEmpty() || toText.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "Wybierz datę od i datę do",
                        Toast.LENGTH_SHORT
                    ).show()

                    resetTimeFilterToAll(rootView)
                    return@setPositiveButton
                }

                try {
                    val from = dateFormat.parse(fromText)
                    val to = dateFormat.parse(toText)

                    if (from == null || to == null) {
                        Toast.makeText(
                            requireContext(),
                            "Podaj poprawne daty",
                            Toast.LENGTH_SHORT
                        ).show()

                        resetTimeFilterToAll(rootView)
                        return@setPositiveButton
                    }

                    if (from.after(to)) {
                        Toast.makeText(
                            requireContext(),
                            "Data od nie może być późniejsza niż data do",
                            Toast.LENGTH_SHORT
                        ).show()

                        resetTimeFilterToAll(rootView)
                        return@setPositiveButton
                    }

                    customDateFrom = from
                    customDateTo = to

                    currentTimeFilter = PortfolioFilterPrefs.TIME_CUSTOM

                    PortfolioFilterPrefs.saveTimeFilter(requireContext(), currentTimeFilter)
                    PortfolioFilterPrefs.saveCustomDateRange(
                        requireContext(),
                        customDateFrom,
                        customDateTo
                    )

                    loadCustomData(rootView)
                } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        "Podaj poprawne daty w formacie yyyy-MM-dd",
                        Toast.LENGTH_SHORT
                    ).show()

                    resetTimeFilterToAll(rootView)
                }
            }
            .setNegativeButton("Anuluj") { _, _ ->
                resetTimeFilterToAll(rootView)
            }
            .show()
    }

    private fun resetTimeFilterToAll(rootView: View) {
        customDateFrom = null
        customDateTo = null

        currentTimeFilter = PortfolioFilterPrefs.TIME_ALL

        PortfolioFilterPrefs.saveTimeFilter(requireContext(), currentTimeFilter)
        PortfolioFilterPrefs.saveCustomDateRange(requireContext(), null, null)

        val spinner2 = rootView.findViewById<Spinner>(R.id.spinnerTab2)
        spinner2.setSelection(0)

        loadCustomData(rootView)
    }

    private fun loadCustomData(view: View) {
        val prefs = requireContext().getSharedPreferences("chart_data", 0)
        val positions = prefs.getStringSet("positions", emptySet()) ?: emptySet()

        Log.d("CHART_DATA", "Znaleziono ${positions.size} pozycji")

        val chartView = view.findViewById<SimpleChartView>(R.id.simpleChart2)
        val pieChartView = view.findViewById<SimplePieChartView>(R.id.pieChart)
        val tvTotalProfit = view.findViewById<TextView>(R.id.tvTotalProfit)

        if (positions.isEmpty()) {
            chartView?.setData(emptyList(), "Brak danych")
            pieChartView?.setData(emptyMap())
            updateTotalProfit(tvTotalProfit, emptyList())
            return
        }

        val parsedPositions = positions.mapNotNull { raw ->
            parsePosition(raw)
        }.filter { position ->
            matchesTypeFilter(position.type)
        }

        if (parsedPositions.isEmpty()) {
            chartView?.setData(emptyList(), "Brak danych")
            pieChartView?.setData(emptyMap())
            updateTotalProfit(tvTotalProfit, emptyList())
            return
        }

        val monthlyTotalValues = calculateMonthlyTotalValues(parsedPositions)
        val filteredMonthlyValues = applyTimeFilter(monthlyTotalValues)

        if (filteredMonthlyValues.isEmpty()) {
            chartView?.setData(emptyList(), "Brak danych")
        } else {
            val values = filteredMonthlyValues.values.toList()
            val labels = filteredMonthlyValues.keys.joinToString(", ")

            chartView?.setData(values, labels)
        }

        /*
         * GŁÓWNY WYKRES KOŁOWY:
         * Pokazuje aktualnie otwarte pozycje i ich udział procentowy w budżecie.
         * Do wykresu przekazujemy wartości, a procent dopisujemy w nazwie.
         */
        val openPositionsBudgetShare = calculateOpenPositionsBudgetShare(parsedPositions)
        pieChartView?.setData(openPositionsBudgetShare)

        updateTotalProfit(tvTotalProfit, parsedPositions)
    }

    private fun parsePosition(raw: String): ChartPosition? {
        val parts = raw.split("|")

        if (parts.size < 7) {
            Log.w("CHART_DATA", "Błędny format pozycji: $raw")
            return null
        }

        return try {
            ChartPosition(
                name = parts[0],
                openDate = parts[1],
                closeDate = parts[2],
                openPrice = parts[3].replace(",", ".").toFloatOrNull() ?: 0f,
                closePrice = parts[4].replace(",", ".").toFloatOrNull() ?: 0f,
                type = parts[5],
                isStillOpen = parts[6].toBoolean()
            )
        } catch (e: Exception) {
            Log.w("CHART_DATA", "Nie udało się odczytać pozycji: $raw", e)
            null
        }
    }

    private fun calculateMonthlyTotalValues(
        positions: List<ChartPosition>
    ): TreeMap<String, Float> {
        val result = TreeMap<String, Float>()

        val now = Calendar.getInstance()

        positions.forEach { position ->
            val openCalendar = parseDateToCalendar(position.openDate) ?: return@forEach

            val endCalendar = if (position.isStillOpen) {
                now
            } else {
                parseDateToCalendar(position.closeDate) ?: return@forEach
            }

            normalizeToFirstDayOfMonth(openCalendar)
            normalizeToFirstDayOfMonth(endCalendar)

            val current = openCalendar.clone() as Calendar

            while (!current.after(endCalendar)) {
                val monthKey = monthFormat.format(current.time)

                val valueForThisMonth = getPositionValueForMonth(
                    position = position,
                    currentMonth = current,
                    endMonth = endCalendar
                )

                result[monthKey] = (result[monthKey] ?: 0f) + valueForThisMonth

                current.add(Calendar.MONTH, 1)
            }
        }

        Log.d("CHART_DATA", "Miesięczne wartości: $result")

        return result
    }

    private fun getPositionValueForMonth(
        position: ChartPosition,
        currentMonth: Calendar,
        endMonth: Calendar
    ): Float {
        return if (position.isStillOpen) {
            if (position.closePrice > 0f) {
                position.closePrice
            } else {
                position.openPrice
            }
        } else {
            if (isSameMonth(currentMonth, endMonth)) {
                position.closePrice
            } else {
                position.openPrice
            }
        }
    }

    private fun calculateOpenPositionsBudgetShare(
        positions: List<ChartPosition>
    ): Map<String, Float> {
        val openPositionsByName = mutableMapOf<String, Float>()

        positions.forEach { position ->
            if (position.isStillOpen) {
                val currentValue = if (position.closePrice > 0f) {
                    position.closePrice
                } else {
                    position.openPrice
                }

                if (currentValue > 0f) {
                    openPositionsByName[position.name] =
                        (openPositionsByName[position.name] ?: 0f) + currentValue
                }
            }
        }

        val totalBudget = openPositionsByName.values.sum()

        if (totalBudget <= 0f) {
            return emptyMap()
        }

        val result = linkedMapOf<String, Float>()

        openPositionsByName
            .toList()
            .sortedByDescending { it.second }
            .forEach { entry ->
                val name = entry.first
                val value = entry.second
                val percent = (value / totalBudget) * 100f

                val label = "$name ${"%.1f".format(percent)}%"

                result[label] = value
            }

        return result
    }

    private fun updateTotalProfit(
        tvTotalProfit: TextView,
        positions: List<ChartPosition>
    ) {
        val closedPositions = positions.filter { position ->
            !position.isStillOpen &&
                    position.openPrice > 0f &&
                    matchesCloseDateFilterForProfit(position)
        }

        if (closedPositions.isEmpty()) {
            tvTotalProfit.text = "BD"
            tvTotalProfit.setTextColor(0xFFB0B0B0.toInt())
            return
        }

        var totalInvested = 0f
        var totalReturned = 0f

        closedPositions.forEach { position ->
            totalInvested += position.openPrice
            totalReturned += position.closePrice
        }

        val profitPercent = if (totalInvested > 0f) {
            ((totalReturned - totalInvested) / totalInvested) * 100f
        } else {
            0f
        }

        val profitText = "${if (profitPercent >= 0f) "+" else ""}${"%.1f".format(profitPercent)}%"

        tvTotalProfit.text = profitText

        tvTotalProfit.setTextColor(
            if (profitPercent >= 0f) {
                0xFF10B981.toInt()
            } else {
                0xFFEF4444.toInt()
            }
        )
    }

    private fun matchesCloseDateFilterForProfit(position: ChartPosition): Boolean {
        if (currentTimeFilter == PortfolioFilterPrefs.TIME_ALL) {
            return true
        }

        if (position.isStillOpen) {
            return false
        }

        if (currentTimeFilter == PortfolioFilterPrefs.TIME_CUSTOM) {
            val from = customDateFrom
            val to = customDateTo

            if (from == null || to == null) {
                return true
            }

            return try {
                val closeDate = dateFormat.parse(position.closeDate)

                closeDate != null &&
                        !closeDate.before(from) &&
                        !closeDate.after(to)
            } catch (e: Exception) {
                false
            }
        }

        val calendar = Calendar.getInstance()

        when (currentTimeFilter) {
            PortfolioFilterPrefs.TIME_3_MONTHS -> calendar.add(Calendar.MONTH, -3)
            PortfolioFilterPrefs.TIME_6_MONTHS -> calendar.add(Calendar.MONTH, -6)
            PortfolioFilterPrefs.TIME_YEAR -> calendar.add(Calendar.YEAR, -1)
        }

        val cutoffDate = calendar.time

        return try {
            val closeDate = dateFormat.parse(position.closeDate)

            closeDate != null && !closeDate.before(cutoffDate)
        } catch (e: Exception) {
            false
        }
    }

    private fun applyTimeFilter(
        monthlyValues: TreeMap<String, Float>
    ): TreeMap<String, Float> {
        if (currentTimeFilter == PortfolioFilterPrefs.TIME_ALL) {
            return monthlyValues
        }

        val filtered = TreeMap<String, Float>()

        if (currentTimeFilter == PortfolioFilterPrefs.TIME_CUSTOM) {
            val from = customDateFrom
            val to = customDateTo

            if (from == null || to == null) {
                return monthlyValues
            }

            val fromMonth = monthFormat.format(from)
            val toMonth = monthFormat.format(to)

            monthlyValues.forEach { entry ->
                val month = entry.key
                val value = entry.value

                if (month >= fromMonth && month <= toMonth) {
                    filtered[month] = value
                }
            }

            return filtered
        }

        val calendar = Calendar.getInstance()

        when (currentTimeFilter) {
            PortfolioFilterPrefs.TIME_3_MONTHS -> calendar.add(Calendar.MONTH, -3)
            PortfolioFilterPrefs.TIME_6_MONTHS -> calendar.add(Calendar.MONTH, -6)
            PortfolioFilterPrefs.TIME_YEAR -> calendar.add(Calendar.YEAR, -1)
        }

        val cutoffMonth = monthFormat.format(calendar.time)

        monthlyValues.forEach { entry ->
            val month = entry.key
            val value = entry.value

            if (month >= cutoffMonth) {
                filtered[month] = value
            }
        }

        return filtered
    }

    private fun matchesTypeFilter(type: String): Boolean {
        return currentTypeFilter == PortfolioFilterPrefs.TYPE_ALL || type == currentTypeFilter
    }

    private fun parseDateToCalendar(dateText: String): Calendar? {
        return try {
            val date = dateFormat.parse(dateText) ?: return null

            Calendar.getInstance().apply {
                time = date
            }
        } catch (e: Exception) {
            Log.w("CHART_DATA", "Nieprawidłowa data: $dateText", e)
            null
        }
    }

    private fun normalizeToFirstDayOfMonth(calendar: Calendar) {
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
    }

    private fun isSameMonth(first: Calendar, second: Calendar): Boolean {
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
                first.get(Calendar.MONTH) == second.get(Calendar.MONTH)
    }

    private fun showDatePicker(editText: EditText) {
        val calendar = Calendar.getInstance()

        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val text = String.format(
                    Locale.getDefault(),
                    "%04d-%02d-%02d",
                    year,
                    month + 1,
                    day
                )

                editText.setText(text)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private data class ChartPosition(
        val name: String,
        val openDate: String,
        val closeDate: String,
        val openPrice: Float,
        val closePrice: Float,
        val type: String,
        val isStillOpen: Boolean
    )
}