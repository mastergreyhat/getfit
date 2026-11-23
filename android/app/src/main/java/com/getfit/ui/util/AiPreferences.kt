package com.getfit.ui.util

import android.content.Context
import android.content.SharedPreferences

object AiPreferences {
    private const val PREFS_NAME = "getfit_ai_preferences"
    private const val KEY_AI_SEARCH_ENABLED = "ai_search_enabled"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isAiSearchEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_AI_SEARCH_ENABLED, true)
    }

    fun setAiSearchEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_AI_SEARCH_ENABLED, enabled).apply()
    }
}
