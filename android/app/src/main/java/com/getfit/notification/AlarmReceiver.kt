package com.getfit.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        when (intent.action) {
            AlarmScheduler.ACTION_MEAL_REMINDER -> {
                val mealType = intent.getStringExtra(AlarmScheduler.EXTRA_MEAL_TYPE) ?: "Meal"
                val time = intent.getStringExtra(AlarmScheduler.EXTRA_TIME) ?: ""
                val suggestedFood = intent.getStringExtra(AlarmScheduler.EXTRA_SUGGESTED_FOOD) ?: ""

                NotificationHelper.sendMealReminder(
                    context = context,
                    mealType = mealType,
                    suggestedFood = suggestedFood,
                    time = time
                )

                // Reschedule for next day at the same exact time
                if (time.isNotBlank()) {
                    AlarmScheduler.scheduleMealReminder(
                        context = context,
                        mealType = mealType,
                        time12H = time,
                        suggestedFood = suggestedFood
                    )
                }
            }

            AlarmScheduler.ACTION_WATER_REMINDER -> {
                val time = intent.getStringExtra(AlarmScheduler.EXTRA_TIME) ?: ""

                if (ReminderPreferences.isWaterEnabled(context)) {
                    NotificationHelper.sendWaterReminder(context, time)

                    // Reschedule for next day at the same exact time
                    if (time.isNotBlank()) {
                        AlarmScheduler.scheduleWaterReminder(context, time)
                    }
                }
            }

            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED" -> {
                // Ensure channels exist and reschedule all user reminders
                NotificationHelper.createNotificationChannels(context)
                AlarmScheduler.rescheduleAll(context)
            }
        }
    }
}
