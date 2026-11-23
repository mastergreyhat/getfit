package com.getfit.ui.util

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ThemePreferences {
    private const val PREFS_NAME = "getfit_theme_prefs"
    private const val KEY_THEME = "theme_mode" // "SYSTEM", "LIGHT", "DARK"

    private val _themeMode = MutableStateFlow("DARK")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    fun init(context: Context) {
        val saved = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME, "DARK") ?: "DARK"
        _themeMode.value = saved
    }

    fun setThemeMode(context: Context, mode: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME, mode)
            .apply()
        _themeMode.value = mode
    }
}

