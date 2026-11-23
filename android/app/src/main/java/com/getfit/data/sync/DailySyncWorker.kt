package com.getfit.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.getfit.data.AppDatabase
import com.getfit.data.repository.GetFitRepository
import com.getfit.ui.util.AuthPreferences

/**
 * Background WorkManager worker that runs automatically every 24 hours
 * to silently sync workouts, meals, water, and weights to Cloudflare D1.
 */
class DailySyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (!AuthPreferences.isLoggedIn(applicationContext)) {
            Log.d("DailySyncWorker", "User not logged in; skipping automatic background sync.")
            return Result.success()
        }

        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            val repository = GetFitRepository(db)
            val (success, msg) = CloudSyncManager.syncNow(applicationContext, repository)
            if (success) {
                Log.i("DailySyncWorker", "Daily automatic cloud sync succeeded: $msg")
                Result.success()
            } else {
                Log.w("DailySyncWorker", "Daily automatic sync deferred: $msg")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("DailySyncWorker", "Daily sync failed with exception", e)
            Result.retry()
        }
    }
}
