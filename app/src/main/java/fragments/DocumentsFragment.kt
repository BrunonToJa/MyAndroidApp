package com.example.mybussines.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mybussines.R
import com.example.mybussines.adapters.PositionAdapter
import com.example.mybussines.utils.PortfolioFilterPrefs
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DocumentsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var tvTotalProfit: TextView
    private lateinit var adapter: PositionAdapter

    private lateinit var spinnerTypeFilter: Spinner
    private lateinit var spinnerCloseDateFilter: Spinner

    private val allPositions = mutableListOf<PositionAdapter.PositionItem>()
    private val displayedPositions = mutableListOf<PositionAdapter.PositionItem>()

    private var currentTypeFilter = PortfolioFilterPrefs.DEFAULT_TYPE_FILTER
    private var currentCloseDateFilter = PortfolioFilterPrefs.DEFAULT_TIME_FILTER

    private var customDateFrom: Date? = null
    private var customDateTo: Date? = null

    private var ignoreSpinnerEvents = false

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
        isLenient = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_documents, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.rvPositions)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        tvTotalProfit = view.findViewById(R.id.tvTotalProfit)

        spinnerTypeFilter = view.findViewById(R.id.spinnerTab1)
        spinnerCloseDateFilter = view.findViewById(R.id.spinnerTab2)

        adapter = PositionAdapter(displayedPositions) { item, _ ->
            showEditDialog(item)
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        loadSavedFilters()
        setupFilters()

        loadPositions()
        applyFilters()
    }

    override fun onResume() {
        super.onResume()

        loadSavedFilters()
        syncSpinnerSelections()

        loadPositions()
        applyFilters()
    }

    private fun loadSavedFilters() {
        currentTypeFilter = PortfolioFilterPrefs.getTypeFilter(requireContext())
        currentCloseDateFilter = PortfolioFilterPrefs.getTimeFilter(requireContext())
        customDateFrom = PortfolioFilterPrefs.getCustomDateFrom(requireContext())
        customDateTo = PortfolioFilterPrefs.getCustomDateTo(requireContext())
    }

    private fun setupFilters() {
        setupTypeFilter()
        setupCloseDateFilter()
        syncSpinnerSelections()
    }

    private fun syncSpinnerSelections() {
        if (!::spinnerTypeFilter.isInitialized || !::spinnerCloseDateFilter.isInitialized) return
        if (spinnerTypeFilter.adapter == null || spinnerCloseDateFilter.adapter == null) return

        ignoreSpinnerEvents = true

        val typeIndex = findSpinnerIndex(spinnerTypeFilter, currentTypeFilter)
        if (typeIndex >= 0) {
            spinnerTypeFilter.setSelection(typeIndex, false)
        }

        val timeIndex = findSpinnerIndex(spinnerCloseDateFilter, currentCloseDateFilter)
        if (timeIndex >= 0) {
            spinnerCloseDateFilter.setSelection(timeIndex, false)
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

    private fun setupTypeFilter() {
        val types = mutableListOf(PortfolioFilterPrefs.TYPE_ALL)

        try {
            val accountTypes = resources.getStringArray(R.array.account_types).toList()
            accountTypes.forEach { type ->
                if (!types.contains(type)) {
                    types.add(type)
                }
            }
        } catch (e: Exception) {
            Log.w("DOCS", "Nie znaleziono R.array.account_types", e)
        }

        val typeAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            types
        )

        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTypeFilter.adapter = typeAdapter

        spinnerTypeFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                selectedView: View?,
                position: Int,
                id: Long
            ) {
                if (ignoreSpinnerEvents) return

                currentTypeFilter = types[position]
                PortfolioFilterPrefs.saveTypeFilter(requireContext(), currentTypeFilter)

                Log.d("DOCS", "Lewy filtr typ: $currentTypeFilter")

                applyFilters()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupCloseDateFilter() {
        val closeDateOptions = PortfolioFilterPrefs.timeOptions

        val closeDateAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            closeDateOptions
        )

        closeDateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCloseDateFilter.adapter = closeDateAdapter

        spinnerCloseDateFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                selectedView: View?,
                position: Int,
                id: Long
            ) {
                if (ignoreSpinnerEvents) return

                currentCloseDateFilter = closeDateOptions[position]
                PortfolioFilterPrefs.saveTimeFilter(requireContext(), currentCloseDateFilter)

                Log.d("DOCS", "Prawy filtr czas zamknięcia: $currentCloseDateFilter")

                if (currentCloseDateFilter == PortfolioFilterPrefs.TIME_CUSTOM) {
                    showCustomDateRangeDialog()
                } else {
                    applyFilters()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadPositions() {
        allPositions.clear()

        val prefs = requireContext().getSharedPreferences("chart_data", 0)
        val saved = prefs.getStringSet("positions", emptySet()) ?: emptySet()

        Log.d("DOCS", "Wczytuję pozycje. Zapisanych rekordów: ${saved.size}")

        saved.forEach { raw ->
            val parts = raw.split("|")

            if (parts.size >= 7) {
                val item = PositionAdapter.PositionItem(
                    name = parts[0],
                    openDate = parts[1],
                    closeDate = parts[2],
                    openPrice = parts[3].replace(",", ".").toFloatOrNull() ?: 0f,
                    closePrice = parts[4].replace(",", ".").toFloatOrNull() ?: 0f,
                    accountType = parts[5],
                    isStillOpen = parts[6].toBoolean()
                )

                allPositions.add(item)

                Log.d(
                    "DOCS",
                    "Dodano: name=${item.name}, type=${item.accountType}, open=${item.openDate}, close=${item.closeDate}, isStillOpen=${item.isStillOpen}"
                )
            } else {
                Log.w("DOCS", "Pominięto błędny rekord: $raw")
            }
        }

        Log.d("DOCS", "Po wczytaniu allPositions=${allPositions.size}")
    }

    private fun applyFilters() {
        Log.d(
            "DOCS",
            "applyFilters(): type=$currentTypeFilter, closeDate=$currentCloseDateFilter, all=${allPositions.size}"
        )

        val result = allPositions.filter { item ->
            matchesTypeFilter(item) && matchesCloseDateFilter(item)
        }

        displayedPositions.clear()
        displayedPositions.addAll(result)

        adapter.notifyDataSetChanged()

        Log.d("DOCS", "Po filtrach displayedPositions=${displayedPositions.size}")

        updateEmptyState()
        updateTotalProfit()
    }

    private fun matchesTypeFilter(item: PositionAdapter.PositionItem): Boolean {
        return currentTypeFilter == PortfolioFilterPrefs.TYPE_ALL ||
                item.accountType == currentTypeFilter
    }

    private fun matchesCloseDateFilter(item: PositionAdapter.PositionItem): Boolean {
        if (currentCloseDateFilter == PortfolioFilterPrefs.TIME_ALL) {
            return true
        }

        if (item.isStillOpen) {
            return false
        }

        if (currentCloseDateFilter == PortfolioFilterPrefs.TIME_CUSTOM) {
            val from = customDateFrom
            val to = customDateTo

            if (from == null || to == null) {
                return true
            }

            return try {
                val closeDate = dateFormat.parse(item.closeDate)

                closeDate != null &&
                        !closeDate.before(from) &&
                        !closeDate.after(to)
            } catch (e: Exception) {
                Log.w(
                    "DOCS",
                    "Nieprawidłowa data zamknięcia custom: ${item.closeDate}, pozycja=${item.name}",
                    e
                )

                false
            }
        }

        val calendar = Calendar.getInstance()

        when (currentCloseDateFilter) {
            PortfolioFilterPrefs.TIME_3_MONTHS -> calendar.add(Calendar.MONTH, -3)
            PortfolioFilterPrefs.TIME_6_MONTHS -> calendar.add(Calendar.MONTH, -6)
            PortfolioFilterPrefs.TIME_YEAR -> calendar.add(Calendar.YEAR, -1)
        }

        val cutoffDate = calendar.time

        return try {
            val closeDate = dateFormat.parse(item.closeDate)

            closeDate != null && !closeDate.before(cutoffDate)
        } catch (e: Exception) {
            Log.w(
                "DOCS",
                "Nieprawidłowa data zamknięcia: ${item.closeDate}, pozycja=${item.name}",
                e
            )

            false
        }
    }

    private fun showCustomDateRangeDialog() {
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

                    currentCloseDateFilter = PortfolioFilterPrefs.TIME_ALL
                    PortfolioFilterPrefs.saveTimeFilter(requireContext(), currentCloseDateFilter)
                    spinnerCloseDateFilter.setSelection(0)
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

                        currentCloseDateFilter = PortfolioFilterPrefs.TIME_ALL
                        PortfolioFilterPrefs.saveTimeFilter(requireContext(), currentCloseDateFilter)
                        spinnerCloseDateFilter.setSelection(0)
                        return@setPositiveButton
                    }

                    if (from.after(to)) {
                        Toast.makeText(
                            requireContext(),
                            "Data od nie może być późniejsza niż data do",
                            Toast.LENGTH_SHORT
                        ).show()

                        currentCloseDateFilter = PortfolioFilterPrefs.TIME_ALL
                        PortfolioFilterPrefs.saveTimeFilter(requireContext(), currentCloseDateFilter)
                        spinnerCloseDateFilter.setSelection(0)
                        return@setPositiveButton
                    }

                    customDateFrom = from
                    customDateTo = to

                    PortfolioFilterPrefs.saveTimeFilter(requireContext(), PortfolioFilterPrefs.TIME_CUSTOM)
                    PortfolioFilterPrefs.saveCustomDateRange(requireContext(), customDateFrom, customDateTo)

                    applyFilters()
                } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        "Podaj poprawne daty w formacie yyyy-MM-dd",
                        Toast.LENGTH_SHORT
                    ).show()

                    customDateFrom = null
                    customDateTo = null

                    PortfolioFilterPrefs.saveCustomDateRange(requireContext(), null, null)

                    currentCloseDateFilter = PortfolioFilterPrefs.TIME_ALL
                    PortfolioFilterPrefs.saveTimeFilter(requireContext(), currentCloseDateFilter)

                    spinnerCloseDateFilter.setSelection(0)
                    applyFilters()
                }
            }
            .setNegativeButton("Anuluj") { _, _ ->
                currentCloseDateFilter = PortfolioFilterPrefs.TIME_ALL
                PortfolioFilterPrefs.saveTimeFilter(requireContext(), currentCloseDateFilter)

                spinnerCloseDateFilter.setSelection(0)
                applyFilters()
            }
            .show()
    }

    private fun updateEmptyState() {
        if (displayedPositions.isEmpty()) {
            recyclerView.visibility = View.GONE
            tvEmpty.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            tvEmpty.visibility = View.GONE
        }
    }

    private fun updateTotalProfit() {
        val closedPositions = displayedPositions.filter {
            !it.isStillOpen && it.openPrice > 0f
        }

        if (closedPositions.isEmpty()) {
            tvTotalProfit.text = "BD"
            tvTotalProfit.setTextColor(0xFFB0B0B0.toInt())
            return
        }

        var totalInvested = 0f
        var totalReturned = 0f

        closedPositions.forEach { item ->
            totalInvested += item.openPrice
            totalReturned += item.closePrice
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

    private fun showEditDialog(item: PositionAdapter.PositionItem) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_custom_chart, null)

        val etName = dialogView.findViewById<EditText>(R.id.etStockName)
        val etOpenDate = dialogView.findViewById<EditText>(R.id.etOpenDate)
        val etCloseDate = dialogView.findViewById<EditText>(R.id.etCloseDate)
        val etOpenPrice = dialogView.findViewById<EditText>(R.id.etOpenPrice)
        val etClosePrice = dialogView.findViewById<EditText>(R.id.etClosePrice)
        val cbStillOpen = dialogView.findViewById<CheckBox>(R.id.cbStillOpen)
        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinnerAccountType)

        etName.setText(item.name)
        etOpenDate.setText(item.openDate)
        etCloseDate.setText(item.closeDate)
        etOpenPrice.setText(item.openPrice.toString())
        etClosePrice.setText(item.closePrice.toString())
        cbStillOpen.isChecked = item.isStillOpen

        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.account_types,
            android.R.layout.simple_spinner_item
        ).also { spinnerAdapter ->
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerType.adapter = spinnerAdapter

            val typeIndex = resources
                .getStringArray(R.array.account_types)
                .indexOf(item.accountType)

            if (typeIndex >= 0) {
                spinnerType.setSelection(typeIndex)
            }
        }

        fun updateCloseFields(isStillOpen: Boolean) {
            if (isStillOpen) {
                etCloseDate.visibility = View.GONE
                etClosePrice.hint = "Aktualna cena"
            } else {
                etCloseDate.visibility = View.VISIBLE
                etClosePrice.hint = "Cena zamknięcia"
            }
        }

        updateCloseFields(item.isStillOpen)

        cbStillOpen.setOnCheckedChangeListener { _, isChecked ->
            updateCloseFields(isChecked)
        }

        etOpenDate.setOnClickListener {
            showDatePicker(etOpenDate)
        }

        etCloseDate.setOnClickListener {
            showDatePicker(etCloseDate)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edytuj pozycję")
            .setView(dialogView)
            .setPositiveButton("Zapisz") { _, _ ->
                val isStillOpen = cbStillOpen.isChecked

                val updated = PositionAdapter.PositionItem(
                    name = etName.text.toString().trim(),
                    openDate = etOpenDate.text.toString().trim(),
                    closeDate = if (isStillOpen) {
                        "OTWARTE"
                    } else {
                        etCloseDate.text.toString().trim()
                    },
                    openPrice = etOpenPrice.text.toString().replace(",", ".").toFloatOrNull() ?: 0f,
                    closePrice = etClosePrice.text.toString().replace(",", ".").toFloatOrNull() ?: 0f,
                    accountType = spinnerType.selectedItem.toString(),
                    isStillOpen = isStillOpen
                )

                val index = allPositions.indexOfFirst {
                    it.name == item.name &&
                            it.openDate == item.openDate &&
                            it.accountType == item.accountType
                }

                if (index >= 0) {
                    allPositions[index] = updated
                }

                savePositions()
                loadPositions()
                applyFilters()
            }
            .setNegativeButton("Usuń") { _, _ ->
                val index = allPositions.indexOfFirst {
                    it.name == item.name &&
                            it.openDate == item.openDate &&
                            it.accountType == item.accountType
                }

                if (index >= 0) {
                    allPositions.removeAt(index)
                }

                savePositions()
                loadPositions()
                applyFilters()
            }
            .setNeutralButton("Anuluj", null)
            .show()
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

    private fun savePositions() {
        val prefs = requireContext().getSharedPreferences("chart_data", 0)

        val set = allPositions.map { item ->
            "${item.name}|${item.openDate}|${item.closeDate}|${item.openPrice}|${item.closePrice}|${item.accountType}|${item.isStillOpen}"
        }.toMutableSet()

        prefs.edit()
            .putStringSet("positions", set)
            .apply()

        Log.d("DOCS", "Zapisano pozycji: ${set.size}")
    }
}
