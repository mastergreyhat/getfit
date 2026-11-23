package com.getfit

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.getfit.data.sync.DailySyncWorker
import com.getfit.notification.MealReminderWorker
import com.getfit.notification.NotificationHelper
import java.util.concurrent.TimeUnit

class GetFitApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this

        // Global crash handler to catch and log any runtime exceptions
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("GetFitApp", "Uncaught exception in thread ${thread.name}", throwable)
        }

        // Initialize Notification Channels
        NotificationHelper.createNotificationChannels(this)

        // Schedule Exact On-Time Reminders via AlarmManager
        com.getfit.notification.AlarmScheduler.rescheduleAll(this)

        // Keep WorkManager as fallback
        scheduleBackgroundReminders()
        scheduleBackgroundSync()
    }

    private fun scheduleBackgroundReminders() {
        val workManager = WorkManager.getInstance(this)

        // Meal reminder every 1 hour (checks current time vs schedule)
        val mealWork = PeriodicWorkRequestBuilder<MealReminderWorker>(
            1, TimeUnit.HOURS,
            15, TimeUnit.MINUTES
        ).build()

        workManager.enqueueUniquePeriodicWork(
            "meal_reminder_work",
            ExistingPeriodicWorkPolicy.KEEP,
            mealWork
        )
    }

    private fun scheduleBackgroundSync() {
        val workManager = WorkManager.getInstance(this)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Automatic silent sync once every 24 hours whenever device is connected to internet
        val dailySyncWork = PeriodicWorkRequestBuilder<DailySyncWorker>(
            24, TimeUnit.HOURS,
            2, TimeUnit.HOURS // flex interval
        ).setConstraints(constraints).build()

        workManager.enqueueUniquePeriodicWork(
            "daily_cloud_sync_work",
            ExistingPeriodicWorkPolicy.KEEP,
            dailySyncWork
        )
    }

    companion object {
        lateinit var instance: GetFitApp
            private set
    }
}
