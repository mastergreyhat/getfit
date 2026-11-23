package com.getfit.ui.theme

import android.app.Activity
import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = DarkAccent,
    onPrimary = DarkOnAccent,
    primaryContainer = DarkSurface2,
    onPrimaryContainer = DarkTextPrimary,
    secondary = DarkAccent,
    onSecondary = DarkOnAccent,
    secondaryContainer = DarkSurface2,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = MacroCarbs,
    onTertiary = DarkOnAccent,
    tertiaryContainer = DarkSurface2,
    onTertiaryContainer = DarkTextPrimary,
    error = SemanticDanger,
    onError = Color.White,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface1,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurface2,
    onSurfaceVariant = DarkTextSecondary,
    surfaceTint = Color.Transparent,
    outline = DarkHairline,
    outlineVariant = DarkHairline
)

private val LightColorScheme = lightColorScheme(
    primary = LightAccent,
    onPrimary = LightOnAccent,
    primaryContainer = LightSurface2,
    onPrimaryContainer = LightTextPrimary,
    secondary = LightAccent,
    onSecondary = LightOnAccent,
    secondaryContainer = LightSurface2,
    onSecondaryContainer = LightTextPrimary,
    tertiary = MacroCarbs,
    onTertiary = LightOnAccent,
    tertiaryContainer = LightSurface2,
    onTertiaryContainer = LightTextPrimary,
    error = SemanticDanger,
    onError = Color.White,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface1,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurface2,
    onSurfaceVariant = LightTextSecondary,
    surfaceTint = Color.Transparent,
    outline = LightHairline,
    outlineVariant = LightHairline
)

object GetFitTheme {
    val colors: GetFitColors
        @Composable
        get() = LocalGetFitColors.current

    val typography: Typography
        @Composable
        get() = MaterialTheme.typography

    val shapes: Shapes
        @Composable
        get() = MaterialTheme.shapes

    val dimens: Dimens
        get() = Dimens
}

@Composable
fun GetFitTheme(
    themeMode: String = "DARK", // Default to Dark mode (CRED/Apple TV style)
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }

    val getFitColors = if (isDark) DarkGetFitColors else LightGetFitColors
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity
            if (activity != null) {
                // Ensure edge-to-edge transparent system bars
                val window = activity.window
                WindowCompat.setDecorFitsSystemWindows(window, false)
                window.statusBarColor = AndroidColor.TRANSPARENT
                window.navigationBarColor = AndroidColor.TRANSPARENT
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    CompositionLocalProvider(LocalGetFitColors provides getFitColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
