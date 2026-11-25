package com.getfit.ui.util

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.getfit.R
import com.getfit.ui.theme.DarkAccent
import com.getfit.ui.theme.MacroCalories
import com.getfit.ui.theme.MacroCarbs
import com.getfit.ui.theme.MacroProtein
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ProfileAvatar(
    val id: String,
    val name: String,
    val requiredStreakDays: Int,
    @DrawableRes val artRes: Int,
    val tierLabel: String,
    val description: String,
    val accentColor: Color
)

object AvatarManager {
    private const val PREFS_NAME = "getfit_avatar_prefs"
    private const val KEY_SELECTED_AVATAR = "selected_avatar_id"
    private const val KEY_SEEN_UNLOCKED_IDS = "seen_unlocked_avatar_ids"

    private val DiamondCyan = Color(0xFFB8F8FF)

    private val _avatarUpdates = MutableStateFlow(0L)
    val avatarUpdates: StateFlow<Long> = _avatarUpdates.asStateFlow()

    val AVATARS = listOf(
        ProfileAvatar(
            id = "avatar_starter",
            name = "Novice Lifter",
            requiredStreakDays = 0,
            artRes = R.drawable.avatar_starter,
            tierLabel = "Starter",
            description = "Welcome to your fitness journey! Unlocked by default for all athletes.",
            accentColor = DarkAccent
        ),
        ProfileAvatar(
            id = "avatar_1_month",
            name = "Iron Dedicated",
            requiredStreakDays = 30,
            artRes = R.drawable.avatar_1_month,
            tierLabel = "1 Month Streak",
            description = "Unlocks at 30 days of consecutive daily consistency.",
            accentColor = MacroCalories
        ),
        ProfileAvatar(
            id = "avatar_3_months",
            name = "Silver Beast",
            requiredStreakDays = 90,
            artRes = R.drawable.avatar_3_months,
            tierLabel = "3 Months Streak",
            description = "Unlocks at 90 days. Forging unbreakable physical discipline.",
            accentColor = MacroProtein
        ),
        ProfileAvatar(
            id = "avatar_6_months",
            name = "Gold Titan",
            requiredStreakDays = 180,
            artRes = R.drawable.avatar_6_months,
            tierLabel = "6 Months Streak",
            description = "Unlocks at 180 days. Half a year of continuous athletic mastery.",
            accentColor = MacroCarbs
        ),
        ProfileAvatar(
            id = "avatar_1_year",
            name = "Diamond Legend",
            requiredStreakDays = 365,
            artRes = R.drawable.avatar_1_year,
            tierLabel = "1 Year Streak",
            description = "Unlocks at 365 days. The ultimate milestone: 1 full year of unstoppable dedication.",
            accentColor = DiamondCyan
        )
    )

    fun getAllAvatars(): List<ProfileAvatar> = AVATARS

    fun getEffectiveBestStreak(context: Context): Int = StreakManager.getBestStreak(context)

    fun isUnlocked(avatar: ProfileAvatar, bestStreak: Int): Boolean =
        avatar.requiredStreakDays <= 0 || bestStreak >= avatar.requiredStreakDays

    fun isUnlocked(context: Context, avatar: ProfileAvatar): Boolean =
        isUnlocked(avatar, getEffectiveBestStreak(context))

    fun nextLockedAvatar(bestStreak: Int): ProfileAvatar? =
        AVATARS.filter { !isUnlocked(it, bestStreak) }.minByOrNull { it.requiredStreakDays }

    fun daysRemaining(avatar: ProfileAvatar, bestStreak: Int): Int =
        (avatar.requiredStreakDays - bestStreak).coerceAtLeast(0)

    fun getSelectedAvatar(context: Context): ProfileAvatar {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val selectedId = prefs.getString(KEY_SELECTED_AVATAR, "avatar_starter") ?: "avatar_starter"
        val candidate = AVATARS.find { it.id == selectedId } ?: AVATARS.first()
        return if (isUnlocked(context, candidate)) candidate else AVATARS.first()
    }

    fun setSelectedAvatarId(context: Context, avatarId: String): Boolean {
        val avatar = AVATARS.find { it.id == avatarId } ?: return false
        if (!isUnlocked(context, avatar)) return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SELECTED_AVATAR, avatarId).apply()
        _avatarUpdates.value = System.currentTimeMillis()
        return true
    }

    fun checkNewUnlock(context: Context, bestStreak: Int = getEffectiveBestStreak(context)): ProfileAvatar? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val seen = prefs.getStringSet(KEY_SEEN_UNLOCKED_IDS, null) ?: setOf("avatar_starter")
        return AVATARS
            .filter { it.requiredStreakDays > 0 && isUnlocked(it, bestStreak) && it.id !in seen }
            .maxByOrNull { it.requiredStreakDays }
    }

    fun markUnlockSeen(context: Context, avatarId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val seen = (prefs.getStringSet(KEY_SEEN_UNLOCKED_IDS, null) ?: setOf("avatar_starter")).toMutableSet()
        if (seen.add(avatarId)) {
            prefs.edit().putStringSet(KEY_SEEN_UNLOCKED_IDS, seen).apply()
        }
    }
}
