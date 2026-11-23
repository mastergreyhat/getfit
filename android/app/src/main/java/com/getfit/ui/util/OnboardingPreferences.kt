package com.getfit.ui.util

import android.content.Context

object OnboardingPreferences {
    private const val PREFS_NAME = "getfit_onboarding_prefs"
    private const val KEY_ONBOARDING_COMPLETED = "has_completed_onboarding"

    fun hasCompletedOnboarding(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun setOnboardingCompleted(context: Context, completed: Boolean = true) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }
}
