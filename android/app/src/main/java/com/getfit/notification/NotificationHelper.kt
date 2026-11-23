package com.getfit.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.getfit.MainActivity
import com.getfit.R

object NotificationHelper {

    const val CHANNEL_MEALS = "meal_reminders"
    const val CHANNEL_WATER = "water_reminders"

    private const val MEAL_NOTIFICATION_ID = 1002
    private const val WATER_NOTIFICATION_ID = 1004

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val mealChannel = NotificationChannel(
                CHANNEL_MEALS,
                "Meal Schedule Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for scheduled muscle-building meals and nutrition"
                enableVibration(true)
            }

            val waterChannel = NotificationChannel(
                CHANNEL_WATER,
                "Water Hydration Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Scheduled alerts to drink water at user-configured times"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(mealChannel)
            notificationManager.createNotificationChannel(waterChannel)
        }
    }

    fun sendMealReminder(context: Context, mealType: String, suggestedFood: String, time: String) {
        val title = "Time for $mealType ($time)"
        val message = if (suggestedFood.isNotBlank()) {
            "Suggested: $suggestedFood"
        } else {
            "Time to fuel your muscles. Log your $mealType now."
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_MEALS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationId = 1000 + kotlin.math.abs(mealType.hashCode() % 1000)
        notifySafe(context, notificationId, notification)
    }

    fun sendWaterReminder(context: Context, time: String) {
        val title = "Drink Water Reminder ($time)"
        val message = "Time to drink 250-500ml of water to stay hydrated and hit your target."

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            3,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_WATER)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationId = 3000 + kotlin.math.abs(time.hashCode() % 1000)
        notifySafe(context, notificationId, notification)
    }

    fun sendTestNotification(context: Context) {
        val title = "GetFit Reminders Configured"
        val message = "Meal & water reminders are active at your exact chosen times. No spam."

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_MEALS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notifySafe(context, 1003, notification)
    }

    fun isNotificationPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    private fun notifySafe(context: Context, notificationId: Int, notification: android.app.Notification) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
            }
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private const val PREFS_NOTIFICATION_PROMPT = "getfit_notification_prompt_prefs"
    private const val KEY_NOTIFICATION_PROMPTED = "has_prompted_notification_permission"

    fun hasPromptedNotification(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NOTIFICATION_PROMPT, Context.MODE_PRIVATE)
            .getBoolean(KEY_NOTIFICATION_PROMPTED, false)
    }

    fun setPromptedNotification(context: Context, prompted: Boolean = true) {
        context.getSharedPreferences(PREFS_NOTIFICATION_PROMPT, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_NOTIFICATION_PROMPTED, prompted)
            .apply()
    }
}

object TimeValidator {
    private val TIME_12H_REGEX = Regex("^(0?[1-9]|1[0-2]):([0-5][0-9])\\s*([AaPp][Mm])$")
    private val TIME_24H_REGEX = Regex("^([01]?[0-9]|2[0-3]):([0-5][0-9])$")

    fun isValid(input: String): Boolean {
        val trimmed = input.trim()
        return TIME_12H_REGEX.matches(trimmed) || TIME_24H_REGEX.matches(trimmed)
    }

    fun normalizeTo12H(input: String): String? {
        val trimmed = input.trim()
        val match12 = TIME_12H_REGEX.matchEntire(trimmed)
        if (match12 != null) {
            val (h, m, period) = match12.destructured
            val hour = h.toInt()
            val formattedHour = String.format("%02d", hour)
            return "$formattedHour:$m ${period.uppercase()}"
        }
        val match24 = TIME_24H_REGEX.matchEntire(trimmed)
        if (match24 != null) {
            val (h, m) = match24.destructured
            var hour = h.toInt()
            val period = if (hour >= 12) "PM" else "AM"
            if (hour == 0) {
                hour = 12
            } else if (hour > 12) {
                hour -= 12
            }
            val formattedHour = String.format("%02d", hour)
            return "$formattedHour:$m $period"
        }
        return null
    }

    fun toMinutes(timeStr: String): Int {
        val norm = normalizeTo12H(timeStr) ?: return 0
        val parts = norm.split(" ", ":")
        if (parts.size < 3) return 0
        var hour = parts[0].toIntOrNull() ?: 0
        val min = parts[1].toIntOrNull() ?: 0
        val isPm = parts[2].equals("PM", ignoreCase = true)
        if (hour == 12) {
            hour = if (isPm) 12 else 0
        } else if (isPm) {
            hour += 12
        }
        return hour * 60 + min
    }
}
