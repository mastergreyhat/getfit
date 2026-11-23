package com.getfit.ui.util

import android.content.Context
import android.content.SharedPreferences

object AiPreferences {
    private const val PREFS_NAME = "getfit_ai_preferences"
    private const val KEY_GEMINI_API_KEY = "gemini_api_key"
    private const val KEY_AI_SEARCH_ENABLED = "ai_search_enabled"

    // API key is now securely stored in Cloudflare Worker secrets (never in the client APK)
    private const val DEFAULT_API_KEY = ""

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getGeminiApiKey(context: Context): String {
        val userKey = getPrefs(context).getString(KEY_GEMINI_API_KEY, null)?.trim()
        return if (!userKey.isNullOrBlank()) userKey else DEFAULT_API_KEY
    }

    fun setGeminiApiKey(context: Context, key: String) {
        getPrefs(context).edit().putString(KEY_GEMINI_API_KEY, key.trim()).apply()
    }

    fun hasCustomGeminiApiKey(context: Context): Boolean {
        val userKey = getPrefs(context).getString(KEY_GEMINI_API_KEY, null)?.trim()
        return !userKey.isNullOrBlank()
    }

    fun isAiSearchEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_AI_SEARCH_ENABLED, true)
    }

    fun setAiSearchEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_AI_SEARCH_ENABLED, enabled).apply()
    }
}
