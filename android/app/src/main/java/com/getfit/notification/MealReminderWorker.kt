package com.getfit.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.getfit.data.AppDatabase
import com.getfit.data.MealPlanSuggestions
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class MealReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val now = Calendar.getInstance()
            val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

            val db = AppDatabase.getDatabase(applicationContext)
            val schedules = db.mealScheduleDao().getEnabledSchedules().first()

            if (schedules.isNotEmpty()) {
                // Find a meal scheduled within +/- 30 minutes of now
                val upcoming = schedules.firstOrNull { schedule ->
                    val scheduledMinutes = parseTimeToMinutes(schedule.scheduledTime)
                    scheduledMinutes != null && abs(scheduledMinutes - currentMinutes) <= 30
                }

                if (upcoming != null) {
                    NotificationHelper.sendMealReminder(
                        applicationContext,
                        mealType = upcoming.mealType,
                        suggestedFood = upcoming.suggestedFoods,
                        time = upcoming.scheduledTime
                    )
                }
            } else {
                // Fallback to standard 2500 kcal plan meals
                val defaultPlan = MealPlanSuggestions.get2500CalPlan()
                val match = defaultPlan.meals.firstOrNull { meal ->
                    val mealMinutes = parseTimeToMinutes(meal.time)
                    mealMinutes != null && abs(mealMinutes - currentMinutes) <= 30
                }

                if (match != null) {
                    NotificationHelper.sendMealReminder(
                        applicationContext,
                        mealType = match.mealType,
                        suggestedFood = match.foods.joinToString(", "),
                        time = match.time
                    )
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun parseTimeToMinutes(timeStr: String): Int? {
        return try {
            val parts = timeStr.trim().split(":")
            if (parts.size >= 2) {
                val hour = parts[0].toInt()
                val min = parts[1].toInt()
                hour * 60 + min
            } else null
        } catch (_: Exception) {
            null
        }
    }
}
