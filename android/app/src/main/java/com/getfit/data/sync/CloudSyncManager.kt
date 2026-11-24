package com.getfit.data.sync

import android.content.Context
import android.util.Log
import com.getfit.data.remote.AuthService
import com.getfit.data.repository.GetFitRepository
import com.getfit.ui.util.AuthPreferences
import com.getfit.ui.util.OnboardingPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manages silent background data synchronization between local SQLite (Room)
 * and the user's private Cloudflare D1 serverless database.
 */
object CloudSyncManager {

    private const val TAG = "CloudSyncManager"
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Silent fire-and-forget sync triggered after meal logging, workout logging,
     * or app lifecycle events. Runs in background only if user has logged in.
     */
    fun triggerSilentUpload(context: Context, repository: GetFitRepository) {
        if (!AuthPreferences.isLoggedIn(context)) return

        syncScope.launch {
            try {
                val token = AuthPreferences.getToken(context) ?: return@launch
                val backupJson = repository.exportBackupJson()
                val result = AuthService.uploadSync(token, backupJson)
                result.onSuccess { syncedAt ->
                    AuthPreferences.setLastSyncedTime(context, syncedAt)
                    Log.i(TAG, "Silent sync completed at $syncedAt")
                }.onFailure { err ->
                    Log.w(TAG, "Silent sync deferred: ${err.message}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Silent sync exception: ${e.message}")
            }
        }
    }

    /**
     * Manual sync requested by user from the Profile / Settings screen.
     */
    suspend fun syncNow(context: Context, repository: GetFitRepository): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val token = AuthPreferences.getToken(context)
        if (token.isNullOrBlank()) {
            return@withContext Pair(false, "Please sign in to sync with Cloudflare.")
        }

        try {
            val backupJson = repository.exportBackupJson()
            val result = AuthService.uploadSync(token, backupJson)
            result.fold(
                onSuccess = { syncedAt ->
                    AuthPreferences.setLastSyncedTime(context, syncedAt)
                    Pair(true, "All data synced to cloud successfully.")
                },
                onFailure = { err ->
                    Pair(false, err.message ?: "Failed to sync with cloud.")
                }
            )
        } catch (e: Exception) {
            Pair(false, e.localizedMessage ?: "Sync error occurred.")
        }
    }

    /**
     * Clears all local user-scoped Room data and preferences so Guest and Logged-In
     * sessions stay completely independent and never merge.
     */
    suspend fun clearLocalUserData(context: Context, repository: GetFitRepository) = withContext(Dispatchers.IO) {
        repository.clearUserScopedData()
        context.getSharedPreferences("custom_routine_prefs", Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("getfit_streak_prefs", Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("getfit_avatar_prefs", Context.MODE_PRIVATE).edit().clear().apply()
        OnboardingPreferences.setOnboardingCompleted(context, false)
    }

    /**
     * Restores user's cloud backup into a clean local Room database upon sign-in.
     * Never merges local guest data into a signed-in account.
     */
    suspend fun downloadAndRestore(context: Context, repository: GetFitRepository): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val token = AuthPreferences.getToken(context)
        if (token.isNullOrBlank()) {
            return@withContext Pair(false, "Not signed in.")
        }

        try {
            // Wipe any prior guest or previous-account data before loading this account
            clearLocalUserData(context, repository)

            val result = AuthService.downloadSync(token)
            result.fold(
                onSuccess = { syncResult ->
                    val cloudJson = syncResult.backupJson
                    if (cloudJson.isNullOrBlank()) {
                        Pair(true, "Cloud account linked! Ready for your setup.")
                    } else {
                        val summary = repository.importBackupJson(cloudJson)
                        if (syncResult.updatedAt != null) {
                            AuthPreferences.setLastSyncedTime(context, syncResult.updatedAt)
                        }
                        Pair(true, "Cloud data restored successfully! $summary")
                    }
                },
                onFailure = { err ->
                    Pair(false, err.message ?: "Failed to fetch cloud backup.")
                }
            )
        } catch (e: Exception) {
            Pair(false, e.localizedMessage ?: "Restore error occurred.")
        }
    }
}
