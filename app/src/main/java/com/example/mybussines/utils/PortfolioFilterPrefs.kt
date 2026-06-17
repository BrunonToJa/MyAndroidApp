package com.example.mybussines.utils

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PortfolioFilterPrefs {

    private const val PREFS_NAME = "portfolio_filters"

    private const val KEY_TYPE_FILTER = "type_filter"
    private const val KEY_TIME_FILTER = "time_filter"
    private const val KEY_CUSTOM_DATE_FROM = "custom_date_from"
    private const val KEY_CUSTOM_DATE_TO = "custom_date_to"

    const val DEFAULT_TYPE_FILTER = "Wszystko"
    const val DEFAULT_TIME_FILTER = "Wszystko"

    const val TYPE_ALL = "Wszystko"
    const val TIME_ALL = "Wszystko"
    const val TIME_3_MONTHS = "3 Miesiące"
    const val TIME_6_MONTHS = "6 Miesięcy"
    const val TIME_YEAR = "Rok"
    const val TIME_CUSTOM = "Custom"

    val timeOptions = listOf(
        TIME_ALL,
        TIME_3_MONTHS,
        TIME_6_MONTHS,
        TIME_YEAR,
        TIME_CUSTOM
    )

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
        isLenient = false
    }

    fun saveTypeFilter(context: Context, value: String) {
        context.getSharedPreferences(PREFS_NAME, 0)
            .edit()
            .putString(KEY_TYPE_FILTER, value)
            .apply()
    }

    fun saveTimeFilter(context: Context, value: String) {
        context.getSharedPreferences(PREFS_NAME, 0)
            .edit()
            .putString(KEY_TIME_FILTER, value)
            .apply()
    }

    fun saveCustomDateRange(context: Context, from: Date?, to: Date?) {
        val editor = context.getSharedPreferences(PREFS_NAME, 0).edit()

        if (from == null) {
            editor.remove(KEY_CUSTOM_DATE_FROM)
        } else {
            editor.putString(KEY_CUSTOM_DATE_FROM, dateFormat.format(from))
        }

        if (to == null) {
            editor.remove(KEY_CUSTOM_DATE_TO)
        } else {
            editor.putString(KEY_CUSTOM_DATE_TO, dateFormat.format(to))
        }

        editor.apply()
    }

    fun getTypeFilter(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, 0)
            .getString(KEY_TYPE_FILTER, DEFAULT_TYPE_FILTER)
            ?: DEFAULT_TYPE_FILTER
    }

    fun getTimeFilter(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, 0)
            .getString(KEY_TIME_FILTER, DEFAULT_TIME_FILTER)
            ?: DEFAULT_TIME_FILTER
    }

    fun getCustomDateFrom(context: Context): Date? {
        val value = context.getSharedPreferences(PREFS_NAME, 0)
            .getString(KEY_CUSTOM_DATE_FROM, null)

        return parseDate(value)
    }

    fun getCustomDateTo(context: Context): Date? {
        val value = context.getSharedPreferences(PREFS_NAME, 0)
            .getString(KEY_CUSTOM_DATE_TO, null)

        return parseDate(value)
    }

    fun formatDate(date: Date): String {
        return dateFormat.format(date)
    }

    fun parseDate(value: String?): Date? {
        if (value.isNullOrBlank()) return null

        return try {
            dateFormat.parse(value)
        } catch (e: Exception) {
            null
        }
    }
}