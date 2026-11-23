package com.getfit.notification

import android.content.Context
import android.content.SharedPreferences

object ReminderPreferences {
    private const val PREFS_NAME = "getfit_reminder_prefs"
    private const val KEY_WATER_ENABLED = "water_reminders_enabled"
    private const val KEY_WATER_TIMES = "water_reminder_times"

    private val DEFAULT_WATER_TIMES = listOf(
        "09:00 AM",
        "11:30 AM",
        "02:00 PM",
        "04:30 PM",
        "07:00 PM",
        "09:00 PM"
    )

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isWaterEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_WATER_ENABLED, true)
    }

    fun setWaterEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_WATER_ENABLED, enabled).apply()
    }

    fun getWaterTimes(context: Context): List<String> {
        val raw = getPrefs(context).getString(KEY_WATER_TIMES, null)
        if (raw.isNullOrBlank()) {
            return DEFAULT_WATER_TIMES
        }
        return raw.split(";").map { it.trim() }.filter { it.isNotBlank() }
    }

    fun setWaterTimes(context: Context, times: List<String>) {
        val serialized = times.joinToString(";")
        getPrefs(context).edit().putString(KEY_WATER_TIMES, serialized).apply()
    }
}
