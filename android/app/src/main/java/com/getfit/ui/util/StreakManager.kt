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

        val yesterday = yesterdayDate(realToday)
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
        val storedCurrent = prefs.getInt(KEY_CURRENT_STREAK, 0)
        val storedBest = prefs.getInt(KEY_BEST_STREAK, 0)

        val evaluated = evaluateStreak(
            realToday = realToday,
            lastStreakDate = lastStreakDate,
            storedCurrent = storedCurrent,
            storedBest = storedBest,
            isLoggedToday = isLoggedToday
        )

        if (evaluated.currentStreak != storedCurrent ||
            evaluated.bestStreak != storedBest ||
            evaluated.lastStreakDate != lastStreakDate
        ) {
            prefs.edit()
                .putString(KEY_LAST_STREAK_DATE, evaluated.lastStreakDate)
                .putInt(KEY_CURRENT_STREAK, evaluated.currentStreak)
                .putInt(KEY_BEST_STREAK, evaluated.bestStreak)
                .apply()
        }

        return Pair(evaluated.currentStreak, evaluated.bestStreak)
    }

    data class StreakState(
        val currentStreak: Int,
        val bestStreak: Int,
        val lastStreakDate: String
    )

    internal fun evaluateStreak(
        realToday: String,
        lastStreakDate: String,
        storedCurrent: Int,
        storedBest: Int,
        isLoggedToday: Boolean
    ): StreakState {
        val yesterday = yesterdayDate(realToday)
        var currentStreak = storedCurrent
        var bestStreak = storedBest
        var updatedLastDate = lastStreakDate

        when (lastStreakDate) {
            realToday -> {
                if (!isLoggedToday) {
                    // All logs for today were deleted/reset; roll back today's streak credit
                    currentStreak = (storedCurrent - 1).coerceAtLeast(0)
                    updatedLastDate = if (currentStreak > 0) yesterday else ""
                }
            }
            yesterday -> {
                if (isLoggedToday) {
                    currentStreak = if (storedCurrent > 0) storedCurrent + 1 else 1
                    bestStreak = maxOf(storedBest, currentStreak)
                    updatedLastDate = realToday
                }
            }
            else -> {
                if (isLoggedToday) {
                    currentStreak = 1
                    bestStreak = maxOf(storedBest, 1)
                    updatedLastDate = realToday
                } else {
                    currentStreak = 0
                }
            }
        }
        return StreakState(currentStreak, bestStreak, updatedLastDate)
    }

    internal fun computeActiveStreakDates(currentStreak: Int, lastStreakDate: String): Set<String> {
        if (currentStreak < 1 || lastStreakDate.isBlank()) {
            return emptySet()
        }
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
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

    private fun yesterdayDate(today: String): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        try {
            sdf.parse(today)?.let { cal.time = it }
        } catch (_: Exception) {}
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return sdf.format(cal.time)
    }

    fun exportState(context: Context): StreakState {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return StreakState(
            currentStreak = prefs.getInt(KEY_CURRENT_STREAK, 0),
            bestStreak = prefs.getInt(KEY_BEST_STREAK, 0),
            lastStreakDate = prefs.getString(KEY_LAST_STREAK_DATE, "") ?: ""
        )
    }

    fun restoreState(context: Context, currentStreak: Int, bestStreak: Int, lastStreakDate: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_CURRENT_STREAK, currentStreak.coerceAtLeast(0))
            .putInt(KEY_BEST_STREAK, maxOf(bestStreak, currentStreak).coerceAtLeast(0))
            .putString(KEY_LAST_STREAK_DATE, lastStreakDate)
            .apply()
        _streakUpdates.value = System.currentTimeMillis()
    }

    fun getActiveStreakDates(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentStreak = prefs.getInt(KEY_CURRENT_STREAK, 0)
        val lastStreakDate = prefs.getString(KEY_LAST_STREAK_DATE, "") ?: ""
        return computeActiveStreakDates(currentStreak, lastStreakDate)
    }
}
