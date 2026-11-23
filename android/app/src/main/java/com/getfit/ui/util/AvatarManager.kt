package com.getfit.ui.util

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.getfit.ui.theme.DarkAccent
import com.getfit.ui.theme.MacroCalories
import com.getfit.ui.theme.MacroCarbs
import com.getfit.ui.theme.MacroProtein
import com.getfit.ui.theme.SemanticSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ProfileAvatar(
    val id: String,
    val name: String,
    val requiredStreakDays: Int,
    val emoji: String,
    val tierLabel: String,
    val description: String,
    val accentColor: Color
)

object AvatarManager {
    private const val PREFS_NAME = "getfit_avatar_prefs"
    private const val KEY_SELECTED_AVATAR = "selected_avatar_id"

    private val _avatarUpdates = MutableStateFlow(0L)
    val avatarUpdates: StateFlow<Long> = _avatarUpdates.asStateFlow()

    val AVATARS = listOf(
        ProfileAvatar(
            id = "avatar_starter",
            name = "Novice Lifter",
            requiredStreakDays = 0,
            emoji = "🏋️",
            tierLabel = "Starter",
            description = "Welcome to your fitness journey! Unlocked by default for all athletes.",
            accentColor = SemanticSuccess
        ),
        ProfileAvatar(
            id = "avatar_1_month",
            name = "Iron Dedicated",
            requiredStreakDays = 30,
            emoji = "🔥",
            tierLabel = "1 Month Streak",
            description = "Unlocks at 30 days of consecutive daily consistency.",
            accentColor = MacroCalories
        ),
        ProfileAvatar(
            id = "avatar_3_months",
            name = "Silver Beast",
            requiredStreakDays = 90,
            emoji = "⚡",
            tierLabel = "3 Months Streak",
            description = "Unlocks at 90 days. Forging unbreakable physical discipline.",
            accentColor = MacroProtein
        ),
        ProfileAvatar(
            id = "avatar_6_months",
            name = "Gold Titan",
            requiredStreakDays = 180,
            emoji = "🏆",
            tierLabel = "6 Months Streak",
            description = "Unlocks at 180 days. Half a year of continuous athletic mastery.",
            accentColor = MacroCarbs
        ),
        ProfileAvatar(
            id = "avatar_1_year",
            name = "Diamond Legend",
            requiredStreakDays = 365,
            emoji = "💎",
            tierLabel = "1 Year Streak",
            description = "Unlocks at 365 days. The ultimate milestone: 1 full year of unstoppable dedication.",
            accentColor = DarkAccent
        )
    )

    fun getAllAvatars(): List<ProfileAvatar> = AVATARS

    fun getSelectedAvatar(context: Context): ProfileAvatar {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val selectedId = prefs.getString(KEY_SELECTED_AVATAR, "avatar_starter") ?: "avatar_starter"
        return AVATARS.find { it.id == selectedId } ?: AVATARS.first()
    }

    fun setSelectedAvatarId(context: Context, avatarId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SELECTED_AVATAR, avatarId).apply()
        _avatarUpdates.value = System.currentTimeMillis()
    }

    fun isUnlocked(context: Context, avatar: ProfileAvatar): Boolean {
        if (avatar.requiredStreakDays <= 0) return true
        val bestStreak = StreakManager.getBestStreak(context)
        return bestStreak >= avatar.requiredStreakDays
    }
}
