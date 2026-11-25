package com.getfit.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test

class StreakManagerTest {

    @Test
    fun sameDayLog_continuesStreakFromYesterdayAndUpdatesBest() {
        val state = StreakManager.evaluateStreak(
            realToday = "2026-11-26",
            lastStreakDate = "2026-11-25",
            storedCurrent = 4,
            storedBest = 4,
            isLoggedToday = true
        )
        assertEquals(5, state.currentStreak)
        assertEquals(5, state.bestStreak)
        assertEquals("2026-11-26", state.lastStreakDate)
        assertEquals(
            setOf("2026-11-22", "2026-11-23", "2026-11-24", "2026-11-25", "2026-11-26"),
            StreakManager.computeActiveStreakDates(state.currentStreak, state.lastStreakDate)
        )
    }

    @Test
    fun yesterdayStreak_remainsAliveBeforeTodayLog() {
        val state = StreakManager.evaluateStreak(
            realToday = "2026-11-26",
            lastStreakDate = "2026-11-25",
            storedCurrent = 3,
            storedBest = 7,
            isLoggedToday = false
        )
        assertEquals(3, state.currentStreak)
        assertEquals(7, state.bestStreak)
        assertEquals("2026-11-25", state.lastStreakDate)
    }

    @Test
    fun missedDay_resetsCurrentStreakButPreservesBest() {
        val state = StreakManager.evaluateStreak(
            realToday = "2026-11-26",
            lastStreakDate = "2026-11-24",
            storedCurrent = 5,
            storedBest = 12,
            isLoggedToday = false
        )
        assertEquals(0, state.currentStreak)
        assertEquals(12, state.bestStreak)
    }

    @Test
    fun resetTodaysLogs_rollsBackTodaysStreakCredit() {
        val state = StreakManager.evaluateStreak(
            realToday = "2026-11-26",
            lastStreakDate = "2026-11-26",
            storedCurrent = 5,
            storedBest = 5,
            isLoggedToday = false
        )
        assertEquals(4, state.currentStreak)
        assertEquals("2026-11-25", state.lastStreakDate)
    }
}
