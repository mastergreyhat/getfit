package com.getfit.ui.theme

import android.app.Activity
import android.graphics.Color as AndroidColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
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
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity
            if (activity != null) {
                val window = activity.window
                WindowCompat.setDecorFitsSystemWindows(window, false)
                window.statusBarColor = AndroidColor.TRANSPARENT
                window.navigationBarColor = AndroidColor.TRANSPARENT
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            }
        }
    }

    CompositionLocalProvider(LocalGetFitColors provides DarkGetFitColors) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
