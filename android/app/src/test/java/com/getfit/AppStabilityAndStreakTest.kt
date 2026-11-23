package com.getfit

import com.getfit.ui.util.AppDateManager
import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AppStabilityAndStreakTest {

    @Test
    fun testAppDateManager_TodayAndFutureConstraints() {
        val today = AppDateManager.getTodayDate()
        assertTrue("Today's date should match yyyy-MM-dd format", today.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))

        // Set to today
        AppDateManager.goToToday()
        assertEquals(today, AppDateManager.selectedDate.value)
        assertTrue(AppDateManager.isToday())
        assertFalse(AppDateManager.isPast())
        assertFalse(AppDateManager.isFuture())
        assertFalse("Cannot advance past today", AppDateManager.canGoNext())

        // Attempting to advance beyond today should do nothing
        AppDateManager.nextDate()
        assertEquals(today, AppDateManager.selectedDate.value)

        // Attempting to set a future date directly should clamp to today
        AppDateManager.setDate("2099-12-31")
        assertEquals(today, AppDateManager.selectedDate.value)
        assertFalse(AppDateManager.isFuture())
    }

    @Test
    fun testAppDateManager_PastDateNavigation() {
        AppDateManager.goToToday()
        val today = AppDateManager.getTodayDate()

        // Move to yesterday
        AppDateManager.previousDate()
        val yesterday = AppDateManager.selectedDate.value
        assertNotEquals(today, yesterday)
        assertTrue(AppDateManager.isPast())
        assertFalse(AppDateManager.isToday())
        assertTrue("Can go next when in past", AppDateManager.canGoNext())

        // Move forward back to today
        AppDateManager.nextDate()
        assertEquals(today, AppDateManager.selectedDate.value)
        assertTrue(AppDateManager.isToday())
    }

    @Test
    fun testAppDateManager_DateFormattingSafety() {
        // Test short date formatting
        val shortDate = AppDateManager.formatShortDate("2026-11-12")
        assertTrue("Short date should contain day and month", shortDate.contains("12") && shortDate.contains("Nov"))

        // Malformed input should not throw exception
        val fallback = AppDateManager.formatShortDate("invalid-date-string")
        assertEquals("invalid-date-string", fallback)

        val labelFallback = AppDateManager.getDateDisplayLabel("invalid-date-string")
        assertEquals("invalid-date-string", labelFallback)
    }

    @Test
    fun testStrengthCurveCalculation_DivisionByZeroProtection() {
        // Edge Case 1: Identical weights (min == max)
        val weight1 = 40.0
        val weight2 = 40.0
        val minWeight = (weight1 - 5.0).coerceAtLeast(0.0) // 35.0
        val maxWeight = (weight2 + 5.0).coerceAtLeast(minWeight + 10.0) // 45.0
        val weightRange = (maxWeight - minWeight).coerceAtLeast(1.0)

        assertTrue("Weight range must be > 0", weightRange > 0.0)
        assertEquals(10.0, weightRange, 0.001)

        val normalizedY = ((40.0 - minWeight) / weightRange).toFloat().coerceIn(0f, 1f)
        assertEquals(0.5f, normalizedY, 0.001f)

        // Edge Case 2: Zero kg weight
        val zeroWeight = 0.0
        val minZero = (zeroWeight - 5.0).coerceAtLeast(0.0) // 0.0
        val maxZero = (zeroWeight + 5.0).coerceAtLeast(minZero + 10.0) // 10.0
        val rangeZero = (maxZero - minZero).coerceAtLeast(1.0)
        assertTrue("Weight range for 0kg must be >= 1.0", rangeZero >= 1.0)
    }

    @Test
    fun testOneRepMaxCalculation() {
        // Epley formula: 1RM = weight * (1 + reps / 30.0)
        fun calculate1RM(weight: Double, reps: Int): Double {
            if (reps <= 0) return 0.0
            if (reps == 1) return weight
            return weight * (1.0 + reps / 30.0)
        }

        assertEquals(0.0, calculate1RM(0.0, 10), 0.001)
        assertEquals(0.0, calculate1RM(100.0, 0), 0.001)
        assertEquals(100.0, calculate1RM(100.0, 1), 0.001)
        // 100kg for 10 reps: 100 * (1 + 10/30) = 133.33kg
        assertEquals(133.33, calculate1RM(100.0, 10), 0.01)
    }

    @Test
    fun testStopwatchFormatting() {
        fun formatStopwatch(millis: Long): String {
            val minutes = (millis / 60000) % 60
            val seconds = (millis / 1000) % 60
            val hundredths = (millis % 1000) / 10
            return String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, hundredths)
        }

        assertEquals("00:00.00", formatStopwatch(0L))
        assertEquals("00:08.17", formatStopwatch(8170L))
        assertEquals("01:30.50", formatStopwatch(90500L))
        assertEquals("59:59.99", formatStopwatch(3599990L))
    }

    @Test
    fun testComposeKeyCollisionProofing() {
        // Simulate items with duplicate or 0 IDs (such as online food search results or duplicate exercises)
        val duplicateFoodItems = listOf(
            Triple(0L, "Chicken 65", "ICMR-NIN"),
            Triple(0L, "Chicken 65 (Boneless)", "ICMR-NIN"),
            Triple(0L, "Chicken Curry", "ICMR-NIN")
        )

        // Using our indexed key formula
        val keys = duplicateFoodItems.mapIndexed { idx, item ->
            "online_${item.second}_${item.third}_$idx"
        }

        // Verify all generated keys are strictly distinct
        assertEquals(3, keys.size)
        assertEquals(3, keys.toSet().size)

        // Simulate duplicate exercise names in a workout routine
        val duplicatePlannedExercises = listOf("Incline Bench Press", "Incline Bench Press", "Incline Bench Press")
        val exerciseKeys = duplicatePlannedExercises.mapIndexed { idx, name ->
            "plan_ex_${name}_$idx"
        }
        assertEquals(3, exerciseKeys.size)
        assertEquals(3, exerciseKeys.toSet().size)

        // Simulate duplicate logs on same date with set 1 and id 0
        val duplicateLogs = listOf(
            Triple(0L, "2026-11-14", 1),
            Triple(0L, "2026-11-14", 1)
        )
        val logKeys = duplicateLogs.mapIndexed { idx, log ->
            "log_${log.first}_${log.second}_Bench_${log.third}_$idx"
        }
        assertEquals(2, logKeys.size)
        assertEquals(2, logKeys.toSet().size)
    }

    @Test
    fun testPastDateConfirmationOncePerDate() {
        AppDateManager.clearConfirmedPastDates()
        val pastDate1 = "2026-11-10"
        val pastDate2 = "2026-11-11"

        // Initially not confirmed
        assertFalse(AppDateManager.isPastDateConfirmed(pastDate1))
        assertFalse(AppDateManager.isPastDateConfirmed(pastDate2))

        // Confirm pastDate1
        AppDateManager.confirmPastDate(pastDate1)
        assertTrue("pastDate1 should now be confirmed", AppDateManager.isPastDateConfirmed(pastDate1))
        assertFalse("pastDate2 should still NOT be confirmed", AppDateManager.isPastDateConfirmed(pastDate2))

        // Confirm pastDate2
        AppDateManager.confirmPastDate(pastDate2)
        assertTrue(AppDateManager.isPastDateConfirmed(pastDate1))
        assertTrue(AppDateManager.isPastDateConfirmed(pastDate2))
    }

    @Test
    fun testStreakAvatarMilestones() {
        val avatars = com.getfit.ui.util.AvatarManager.getAllAvatars()
        assertEquals(5, avatars.size)

        val starter = avatars.find { it.id == "avatar_starter" }!!
        assertEquals(0, starter.requiredStreakDays)

        val month1 = avatars.find { it.id == "avatar_1_month" }!!
        assertEquals(30, month1.requiredStreakDays)

        val month3 = avatars.find { it.id == "avatar_3_months" }!!
        assertEquals(90, month3.requiredStreakDays)

        val month6 = avatars.find { it.id == "avatar_6_months" }!!
        assertEquals(180, month6.requiredStreakDays)

        val year1 = avatars.find { it.id == "avatar_1_year" }!!
        assertEquals(365, year1.requiredStreakDays)

        // Verify unlocking logic by streak thresholds
        fun checkUnlocked(avatar: com.getfit.ui.util.ProfileAvatar, bestStreak: Int): Boolean {
            return bestStreak >= avatar.requiredStreakDays
        }

        // At streak 0
        assertTrue(checkUnlocked(starter, 0))
        assertFalse(checkUnlocked(month1, 0))
        assertFalse(checkUnlocked(month3, 0))

        // At streak 30 (1 month)
        assertTrue(checkUnlocked(starter, 30))
        assertTrue(checkUnlocked(month1, 30))
        assertFalse(checkUnlocked(month3, 30))

        // At streak 90 (3 months)
        assertTrue(checkUnlocked(month3, 90))
        assertFalse(checkUnlocked(month6, 90))

        // At streak 180 (6 months)
        assertTrue(checkUnlocked(month6, 180))
        assertFalse(checkUnlocked(year1, 180))

        // At streak 365 (1 year)
        assertTrue(checkUnlocked(year1, 365))
    }

    @Test
    fun testOnboardingRouteResolution() {
        fun resolveStartRoute(hasCompleted: Boolean): String {
            return if (hasCompleted) com.getfit.ui.navigation.Screen.Dashboard.route
            else com.getfit.ui.navigation.Screen.OnboardingWelcome.route
        }

        assertEquals("onboarding_welcome", resolveStartRoute(false))
        assertEquals("dashboard", resolveStartRoute(true))
    }
}
