package com.getfit.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ==========================================
// Dark-First Palette Tokens (CRED / Apple TV Inspired)
// ==========================================
val DarkBackground = Color(0xFF000000)
val DarkSurface1 = Color(0xFF0E0E10) // Cards
val DarkSurface2 = Color(0xFF161618) // Nested elements, inputs
val DarkSurface3 = Color(0xFF1F1F22) // Pressed or selected
val DarkHairline = Color(0x14FFFFFF) // White at 8% alpha (0.08 * 255 ~= 20 = 0x14)
val DarkTextPrimary = Color(0xFFF5F5F7)
val DarkTextSecondary = Color(0xFFA1A1A6)
val DarkTextTertiary = Color(0xFF78787D)
val DarkAccent = Color(0xFFC6F432) // Electric lime
val DarkOnAccent = Color(0xFF0A0A0A)

// ==========================================
// Warm & Clean Light Palette Tokens
// ==========================================
val LightBackground = Color(0xFFF7F6F2)
val LightSurface1 = Color(0xFFFFFFFF)
val LightSurface2 = Color(0xFFF1F0EB)
val LightSurface3 = Color(0xFFE8E6E1)
val LightHairline = Color(0x14000000) // Black at 8% alpha
val LightTextPrimary = Color(0xFF0F0E0C)
val LightTextSecondary = Color(0xFF6B6760)
val LightTextTertiary = Color(0xFFA8A49F)
val LightAccent = Color(0xFF15803D)
val LightOnAccent = Color(0xFFFFFFFF)

// ==========================================
// Nutritional Macros (Calibrated & ~15% Desaturated)
// ==========================================
val MacroCalories = Color(0xFFDF6826)
val MacroProtein = Color(0xFF3B74E6)
val MacroCarbs = Color(0xFFCF801F)
val MacroFat = Color(0xFFD94141)
val MacroWater = Color(0xFF2292C9)
val MacroFiber = DarkAccent

// Compatibility aliases for existing UI code
val CaloriesOrange = MacroCalories
val ProteinBlue = MacroProtein
val CarbsAmber = MacroCarbs
val FatRed = MacroFat
val WaterBlue = MacroWater
val Teal80 = Color(0xFF80CBC4)
val Teal40 = Color(0xFF00897B)
val Orange80 = Color(0xFFFFCC80)
val Orange40 = Color(0xFFEF6C00)
val Gray10 = DarkSurface1
val Gray20 = DarkSurface2
val Gray90 = LightSurface3
val Gray95 = LightBackground
val Gray99 = LightSurface1
val Red80 = Color(0xFFFFB4AB)
val Red40 = Color(0xFFDC2626)

// Clean palette legacy aliases
val CleanPrimary = DarkAccent
val CleanBackground = DarkBackground
val CleanSurface = DarkSurface1
val CleanSurfaceVariant = DarkSurface2
val CleanBorder = DarkHairline
val CleanOnPrimary = DarkOnAccent
val CleanOnSurface = DarkTextPrimary
val CleanOnSurfaceVariant = DarkTextSecondary
val CleanTextMuted = DarkTextTertiary
val CleanDarkPrimary = DarkAccent
val CleanDarkOnPrimary = DarkOnAccent
val CleanDarkBackground = DarkBackground
val CleanDarkSurface = DarkSurface1
val CleanDarkSurfaceVariant = DarkSurface2
val CleanDarkBorder = DarkHairline
val CleanDarkOnBackground = DarkTextPrimary
val CleanDarkOnSurface = DarkTextPrimary
val CleanDarkOnSurfaceVariant = DarkTextSecondary
val CleanDarkTextMuted = DarkTextTertiary

// ==========================================
// Semantic Tokens
// ==========================================
val SemanticSuccess = DarkAccent
val SemanticWarning = Color(0xFFEAB308)
val SemanticDanger = Color(0xFFEF4444)
val SemanticStreakGlowDark = Color(0x33C6F432) // 20% alpha electric lime
val SemanticStreakGlowLight = Color(0x3315803D)

// ==========================================
// Custom GetFitColors Data Class
// ==========================================
@Immutable
data class GetFitColors(
    val background: Color,
    val surface1: Color,
    val surface2: Color,
    val surface3: Color,
    val hairline: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val onAccent: Color,
    val calories: Color,
    val protein: Color,
    val carbs: Color,
    val fat: Color,
    val water: Color,
    val fiber: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val streakGlow: Color,
    val isDark: Boolean
)

val DarkGetFitColors = GetFitColors(
    background = DarkBackground,
    surface1 = DarkSurface1,
    surface2 = DarkSurface2,
    surface3 = DarkSurface3,
    hairline = DarkHairline,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    textTertiary = DarkTextTertiary,
    accent = DarkAccent,
    onAccent = DarkOnAccent,
    calories = MacroCalories,
    protein = MacroProtein,
    carbs = MacroCarbs,
    fat = MacroFat,
    water = MacroWater,
    fiber = DarkAccent,
    success = DarkAccent,
    warning = SemanticWarning,
    danger = SemanticDanger,
    streakGlow = SemanticStreakGlowDark,
    isDark = true
)

val LightGetFitColors = GetFitColors(
    background = LightBackground,
    surface1 = LightSurface1,
    surface2 = LightSurface2,
    surface3 = LightSurface3,
    hairline = LightHairline,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    textTertiary = LightTextTertiary,
    accent = LightAccent,
    onAccent = LightOnAccent,
    calories = MacroCalories,
    protein = MacroProtein,
    carbs = MacroCarbs,
    fat = MacroFat,
    water = MacroWater,
    fiber = LightAccent,
    success = LightAccent,
    warning = SemanticWarning,
    danger = SemanticDanger,
    streakGlow = SemanticStreakGlowLight,
    isDark = false
)

val LocalGetFitColors = staticCompositionLocalOf<GetFitColors> {
    DarkGetFitColors
}
