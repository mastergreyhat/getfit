package com.getfit.data.sync

import android.content.Context
import android.util.Log
import com.getfit.data.remote.AuthService
import com.getfit.data.repository.GetFitRepository
import com.getfit.ui.util.AuthPreferences
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
     * Restores user's cloud backup into the local Room database upon sign-in.
     */
    suspend fun downloadAndRestore(context: Context, repository: GetFitRepository): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val token = AuthPreferences.getToken(context)
        if (token.isNullOrBlank()) {
            return@withContext Pair(false, "Not signed in.")
        }

        try {
            val result = AuthService.downloadSync(token)
            result.fold(
                onSuccess = { syncResult ->
                    val cloudJson = syncResult.backupJson
                    if (cloudJson.isNullOrBlank()) {
                        // Brand new account or no cloud data yet: upload local data to initialise account!
                        val localJson = repository.exportBackupJson()
                        val uploadRes = AuthService.uploadSync(token, localJson)
                        uploadRes.onSuccess { AuthPreferences.setLastSyncedTime(context, it) }
                        Pair(true, "Cloud account linked! Initial local data backed up.")
                    } else {
                        // Cloud data exists: import into Room
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
