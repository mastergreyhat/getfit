package com.getfit.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.getfit.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.abs

object AlarmScheduler {
    const val ACTION_MEAL_REMINDER = "com.getfit.ACTION_MEAL_REMINDER"
    const val ACTION_WATER_REMINDER = "com.getfit.ACTION_WATER_REMINDER"

    const val EXTRA_MEAL_TYPE = "extra_meal_type"
    const val EXTRA_TIME = "extra_time"
    const val EXTRA_SUGGESTED_FOOD = "extra_suggested_food"

    fun scheduleMealReminder(
        context: Context,
        mealType: String,
        time12H: String,
        suggestedFood: String = ""
    ) {
        val triggerMillis = computeNextTriggerMillis(time12H) ?: return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_MEAL_REMINDER
            putExtra(EXTRA_MEAL_TYPE, mealType)
            putExtra(EXTRA_TIME, time12H)
            putExtra(EXTRA_SUGGESTED_FOOD, suggestedFood)
        }

        val requestCode = 1000 + abs(mealType.hashCode() % 1000)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExactAlarm(alarmManager, triggerMillis, pendingIntent)
    }

    fun cancelMealReminder(context: Context, mealType: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_MEAL_REMINDER
        }
        val requestCode = 1000 + abs(mealType.hashCode() % 1000)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun scheduleWaterReminder(context: Context, time12H: String) {
        val triggerMillis = computeNextTriggerMillis(time12H) ?: return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_WATER_REMINDER
            putExtra(EXTRA_TIME, time12H)
        }

        val requestCode = 3000 + abs(time12H.hashCode() % 1000)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExactAlarm(alarmManager, triggerMillis, pendingIntent)
    }

    fun cancelWaterReminder(context: Context, time12H: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_WATER_REMINDER
        }
        val requestCode = 3000 + abs(time12H.hashCode() % 1000)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun cancelAllWaterReminders(context: Context, times: List<String>) {
        times.forEach { cancelWaterReminder(context, it) }
    }

    fun rescheduleAll(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Reschedule Meal Schedules
                val db = AppDatabase.getDatabase(context)
                val mealSchedules = db.mealScheduleDao().getAllSchedules().first()
                for (schedule in mealSchedules) {
                    if (schedule.isEnabled) {
                        scheduleMealReminder(
                            context = context,
                            mealType = schedule.mealType,
                            time12H = schedule.scheduledTime,
                            suggestedFood = schedule.suggestedFoods
                        )
                    } else {
                        cancelMealReminder(context, schedule.mealType)
                    }
                }

                // 2. Reschedule Water Reminders
                val waterEnabled = ReminderPreferences.isWaterEnabled(context)
                val waterTimes = ReminderPreferences.getWaterTimes(context)
                if (waterEnabled) {
                    for (time in waterTimes) {
                        scheduleWaterReminder(context, time)
                    }
                } else {
                    cancelAllWaterReminders(context, waterTimes)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun scheduleExactAlarm(
        alarmManager: AlarmManager,
        triggerMillis: Long,
        pendingIntent: PendingIntent
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun computeNextTriggerMillis(timeStr: String): Long? {
        val totalMinutes = TimeValidator.toMinutes(timeStr)
        val hour24 = totalMinutes / 60
        val minute = totalMinutes % 60

        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour24)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If target has already passed today, push to next day
        if (target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        return target.timeInMillis
    }
}
