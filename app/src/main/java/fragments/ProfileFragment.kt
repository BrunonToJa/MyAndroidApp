package com.example.mybussines.fragments

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.mybussines.LoginActivity
import com.example.mybussines.R
import java.util.*

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // Przycisk Brokerzy
        view.findViewById<Button>(R.id.btnBrokers).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, BrokersFragment())
                .addToBackStack(null)
                .commit()
        }

        // Przycisk Własny wykres - otwiera popup
        view.findViewById<Button>(R.id.btnCustomChart).setOnClickListener {
            showCustomChartDialog()
        }

        // Przycisk Wyloguj
        view.findViewById<Button>(R.id.btnLogout).setOnClickListener {
            logout()
        }

        return view
    }

    private fun showCustomChartDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_custom_chart, null)

        val etName = dialogView.findViewById<EditText>(R.id.etStockName)
        val etOpenDate = dialogView.findViewById<EditText>(R.id.etOpenDate)
        val etCloseDate = dialogView.findViewById<EditText>(R.id.etCloseDate)
        val etOpenPrice = dialogView.findViewById<EditText>(R.id.etOpenPrice)
        val etClosePrice = dialogView.findViewById<EditText>(R.id.etClosePrice)
        val cbStillOpen = dialogView.findViewById<CheckBox>(R.id.cbStillOpen)
        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinnerAccountType)

        // Ukryj pola zamknięcia jeśli ciągle otwarte
        cbStillOpen.setOnCheckedChangeListener { _, isChecked ->
            etCloseDate.visibility = if (isChecked) View.GONE else View.VISIBLE
            etClosePrice.hint = if (isChecked) "Aktualna cena" else "Cena zamknięcia"
        }

        // Spinner typ konta
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.account_types,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerType.adapter = adapter
        }

        // Date pickery
        etOpenDate.setOnClickListener { showDatePicker(etOpenDate) }
        etCloseDate.setOnClickListener { showDatePicker(etCloseDate) }

        AlertDialog.Builder(requireContext())
            .setTitle("Nowa pozycja")
            .setView(dialogView)
            .setPositiveButton("Dodaj") { _, _ ->
                val isStillOpen = cbStillOpen.isChecked
                val data = ChartData(
                    name = etName.text.toString(),
                    openDate = etOpenDate.text.toString(),
                    closeDate = if (isStillOpen) "OTWARTE" else etCloseDate.text.toString(),
                    openPrice = etOpenPrice.text.toString().toFloatOrNull() ?: 0f,
                    closePrice = etClosePrice.text.toString().toFloatOrNull() ?: 0f,
                    accountType = spinnerType.selectedItem.toString(),
                    isStillOpen = isStillOpen
                )
                addToChart(data)
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }

    private fun showDatePicker(editText: EditText) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                editText.setText(String.format("%04d-%02d-%02d", year, month + 1, day))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun addToChart(data: ChartData) {
        val prefs = requireContext().getSharedPreferences("chart_data", 0)
        val existing = prefs.getStringSet("positions", mutableSetOf()) ?: mutableSetOf()

        // Format: nazwa|dataOtwarcia|dataZamkniecia|cenaOtwarcia|cenaZamkniecia|typ|ciagleOtwarte
        existing.add("${data.name}|${data.openDate}|${data.closeDate}|${data.openPrice}|${data.closePrice}|${data.accountType}|${data.isStillOpen}")
        prefs.edit().putStringSet("positions", existing).apply()

        Toast.makeText(requireContext(), "Dodano ${data.name} (${data.openDate} - ${data.closeDate})", Toast.LENGTH_SHORT).show()

        // Przejdź do wykresów
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, ChartFragment())
            .commit()
    }

    private fun logout() {
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    data class ChartData(
        val name: String,
        val openDate: String,
        val closeDate: String,
        val openPrice: Float,
        val closePrice: Float,
        val accountType: String,
        val isStillOpen: Boolean = false
    )
}