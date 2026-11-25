package com.getfit.ui.util

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object HapticsPreferences {
    private const val PREFS_NAME = "getfit_app_prefs"
    private const val KEY_HAPTICS_ENABLED = "haptics_enabled"

    private val _hapticsEnabled = MutableStateFlow(true)
    val hapticsEnabled: StateFlow<Boolean> = _hapticsEnabled.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _hapticsEnabled.value = prefs.getBoolean(KEY_HAPTICS_ENABLED, true)
    }

    fun isHapticsEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(KEY_HAPTICS_ENABLED, true)
        _hapticsEnabled.value = enabled
        return enabled
    }

    fun setHapticsEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_HAPTICS_ENABLED, enabled)
            .apply()
        _hapticsEnabled.value = enabled
    }
}
