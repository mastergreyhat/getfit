package com.getfit.ui.util

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages daily streak progression with strict real-world date validation.
 *
 * Rules:
 * 1. Streak is ONLY credited when a user adds data for the actual CURRENT day.
 * 2. Adding past or future days data is logged in the database, but NEVER increments, restores, or starts a streak.
 * 3. A streak requires consecutive day logging on each respective day. If yesterday was not logged, the streak resets to 0.
 */
object StreakManager {
    private const val PREFS_NAME = "getfit_streak_prefs"
    private const val KEY_CURRENT_STREAK = "current_streak"
    private const val KEY_BEST_STREAK = "best_streak"
    private const val KEY_LAST_STREAK_DATE = "last_streak_date"

    private val _streakUpdates = MutableStateFlow(0L)
    val streakUpdates: StateFlow<Long> = _streakUpdates.asStateFlow()

    fun recordLogAction(context: Context, targetDate: String) {
        val realToday = AppDateManager.getTodayDate()
        // Strict: only data logged for the current real-world day counts toward streak!
        if (targetDate != realToday) {
            return
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastStreakDate = prefs.getString(KEY_LAST_STREAK_DATE, "") ?: ""
        val currentStreak = prefs.getInt(KEY_CURRENT_STREAK, 0)
        val bestStreak = prefs.getInt(KEY_BEST_STREAK, 0)

        if (lastStreakDate == realToday) {
            // Streak already credited for today
            return
        }

        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)

        val newStreak = if (lastStreakDate == yesterday && currentStreak > 0) {
            currentStreak + 1
        } else {
            1
        }
        val newBest = maxOf(bestStreak, newStreak)

        prefs.edit()
            .putString(KEY_LAST_STREAK_DATE, realToday)
            .putInt(KEY_CURRENT_STREAK, newStreak)
            .putInt(KEY_BEST_STREAK, newBest)
            .apply()

        _streakUpdates.value = System.currentTimeMillis()
    }

    fun getBestStreak(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentStreak = prefs.getInt(KEY_CURRENT_STREAK, 0)
        val bestStreak = prefs.getInt(KEY_BEST_STREAK, 0)
        return maxOf(currentStreak, bestStreak)
    }

    fun getCurrentStreak(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_CURRENT_STREAK, 0)
    }

    fun getStreak(context: Context, isLoggedToday: Boolean): Pair<Int, Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val realToday = AppDateManager.getTodayDate()
        val lastStreakDate = prefs.getString(KEY_LAST_STREAK_DATE, "") ?: ""
        var currentStreak = prefs.getInt(KEY_CURRENT_STREAK, 0)
        var bestStreak = prefs.getInt(KEY_BEST_STREAK, 0)

        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)

        if (lastStreakDate == realToday) {
            // Already logged and credited today
        } else if (lastStreakDate == yesterday) {
            if (isLoggedToday) {
                // Today was logged; credit the consecutive day
                val newStreak = if (currentStreak > 0) currentStreak + 1 else 1
                bestStreak = maxOf(bestStreak, newStreak)
                prefs.edit()
                    .putString(KEY_LAST_STREAK_DATE, realToday)
                    .putInt(KEY_CURRENT_STREAK, newStreak)
                    .putInt(KEY_BEST_STREAK, bestStreak)
                    .apply()
                currentStreak = newStreak
            }
            // else: Streak from yesterday is alive, pending today's log to increment
        } else {
            // Missed a day or first time running
            if (isLoggedToday) {
                // User has data for today; start fresh streak of 1
                val newStreak = 1
                bestStreak = maxOf(bestStreak, newStreak)
                prefs.edit()
                    .putString(KEY_LAST_STREAK_DATE, realToday)
                    .putInt(KEY_CURRENT_STREAK, newStreak)
                    .putInt(KEY_BEST_STREAK, bestStreak)
                    .apply()
                currentStreak = newStreak
            } else {
                // Streak broken; reset current streak to 0
                currentStreak = 0
                if (prefs.getInt(KEY_CURRENT_STREAK, 0) != 0) {
                    prefs.edit().putInt(KEY_CURRENT_STREAK, 0).apply()
                }
            }
        }

        return Pair(currentStreak, bestStreak)
    }

    fun getActiveStreakDates(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentStreak = prefs.getInt(KEY_CURRENT_STREAK, 0)
        val lastStreakDate = prefs.getString(KEY_LAST_STREAK_DATE, "") ?: ""
        // Streak is only qualified as active multi-day streak when it reaches 2 or more days
        if (currentStreak < 2 || lastStreakDate.isBlank()) {
            return emptySet()
        }
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val streakDates = mutableSetOf<String>()
        try {
            val lastDate = sdf.parse(lastStreakDate) ?: return emptySet()
            val cal = Calendar.getInstance().apply { time = lastDate }
            for (i in 0 until currentStreak) {
                streakDates.add(sdf.format(cal.time))
                cal.add(Calendar.DAY_OF_YEAR, -1)
            }
        } catch (_: Exception) {}
        return streakDates
    }
}
