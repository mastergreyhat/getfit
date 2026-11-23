package com.getfit.ui.util

import android.content.Context
import android.content.SharedPreferences

object AuthPreferences {
    private const val PREFS_NAME = "getfit_auth_preferences"
    private const val KEY_TOKEN = "auth_session_token"
    private const val KEY_USER_ID = "auth_user_id"
    private const val KEY_USER_EMAIL = "auth_user_email"
    private const val KEY_USER_NAME = "auth_user_name"
    private const val KEY_AUTH_PROVIDER = "auth_provider"
    private const val KEY_LAST_SYNCED = "auth_last_synced_millis"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isLoggedIn(context: Context): Boolean {
        val token = getPrefs(context).getString(KEY_TOKEN, null)
        return !token.isNullOrBlank()
    }

    fun getToken(context: Context): String? {
        return getPrefs(context).getString(KEY_TOKEN, null)
    }

    fun getUserEmail(context: Context): String? {
        return getPrefs(context).getString(KEY_USER_EMAIL, null)
    }

    fun getUserName(context: Context): String? {
        return getPrefs(context).getString(KEY_USER_NAME, null)
    }

    fun getAuthProvider(context: Context): String? {
        return getPrefs(context).getString(KEY_AUTH_PROVIDER, "email")
    }

    fun getLastSyncedTime(context: Context): Long {
        return getPrefs(context).getLong(KEY_LAST_SYNCED, 0L)
    }

    fun setLastSyncedTime(context: Context, millis: Long) {
        getPrefs(context).edit().putLong(KEY_LAST_SYNCED, millis).apply()
    }

    fun saveSession(context: Context, token: String, userId: String, email: String, name: String, provider: String = "email") {
        getPrefs(context).edit()
            .putString(KEY_TOKEN, token.trim())
            .putString(KEY_USER_ID, userId.trim())
            .putString(KEY_USER_EMAIL, email.trim())
            .putString(KEY_USER_NAME, name.trim())
            .putString(KEY_AUTH_PROVIDER, provider)
            .putLong(KEY_LAST_SYNCED, System.currentTimeMillis())
            .apply()
    }

    fun clearSession(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
