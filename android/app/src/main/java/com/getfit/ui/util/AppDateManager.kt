package com.getfit.ui.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Shared Application Date Manager
 * Synchronizes the active selected date between Nutrition (Diary) and Training (Workout).
 * Enforces that future dates cannot be selected or navigated to.
 */
object AppDateManager {
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormatter = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
    private val shortDateFormatter = SimpleDateFormat("d MMM", Locale.getDefault())

    private val _selectedDate = MutableStateFlow(dateFormatter.format(Date()))
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _confirmedPastDates = mutableSetOf<String>()

    fun isPastDateConfirmed(date: String = _selectedDate.value): Boolean {
        return _confirmedPastDates.contains(date)
    }

    fun confirmPastDate(date: String = _selectedDate.value) {
        _confirmedPastDates.add(date)
    }

    fun clearConfirmedPastDates() {
        _confirmedPastDates.clear()
    }

    fun getTodayDate(): String = dateFormatter.format(Date())

    fun isToday(date: String = _selectedDate.value): Boolean = date == getTodayDate()

    fun isPast(date: String = _selectedDate.value): Boolean = date < getTodayDate()

    fun isFuture(date: String = _selectedDate.value): Boolean = date > getTodayDate()

    fun canGoNext(): Boolean = _selectedDate.value < getTodayDate()

    fun getDateDisplayLabel(date: String): String {
        return try {
            val parsed = dateFormatter.parse(date)
            val cal = Calendar.getInstance()
            if (parsed != null) cal.time = parsed
            val today = dateFormatter.format(Date())
            if (date == today) {
                "Today, " + displayDateFormatter.format(cal.time)
            } else {
                displayDateFormatter.format(cal.time)
            }
        } catch (e: Exception) {
            date
        }
    }

    fun formatShortDate(date: String): String {
        return try {
            val parsed = dateFormatter.parse(date) ?: return date
            shortDateFormatter.format(parsed)
        } catch (_: Exception) {
            date
        }
    }

    fun setDate(dateString: String) {
        val today = getTodayDate()
        // Never allow future dates
        _selectedDate.value = if (dateString > today) today else dateString
    }

    fun previousDate() {
        val cal = Calendar.getInstance()
        try {
            val parsed = dateFormatter.parse(_selectedDate.value)
            if (parsed != null) cal.time = parsed
        } catch (_: Exception) {}
        cal.add(Calendar.DAY_OF_YEAR, -1)
        _selectedDate.value = dateFormatter.format(cal.time)
    }

    fun nextDate() {
        val today = getTodayDate()
        if (_selectedDate.value >= today) {
            // Already at today or future; do not allow advancing
            return
        }
        val cal = Calendar.getInstance()
        try {
            val parsed = dateFormatter.parse(_selectedDate.value)
            if (parsed != null) cal.time = parsed
        } catch (_: Exception) {}
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val nextDateStr = dateFormatter.format(cal.time)
        _selectedDate.value = if (nextDateStr > today) today else nextDateStr
    }

    fun goToToday() {
        _selectedDate.value = dateFormatter.format(Date())
    }
}
