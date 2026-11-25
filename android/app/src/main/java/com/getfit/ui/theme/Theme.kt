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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
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

private fun TextStyle.scaled(factor: Float, minSp: Float = 10f): TextStyle {
    if (factor >= 0.999f) return this
    val newSize = (fontSize.value * factor).coerceAtLeast(minSp).sp
    val newLineHeight = if (lineHeight != TextUnit.Unspecified) {
        (lineHeight.value * factor).coerceAtLeast(newSize.value + 2f).sp
    } else lineHeight
    return copy(fontSize = newSize, lineHeight = newLineHeight)
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

    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    // Scale down typography on compact screens (e.g. 360dp S25) so headlines/cards fit without wrapping
    val fontScale = (screenWidthDp / 425f).coerceIn(0.86f, 1.0f)
    val adaptiveTypography = Typography.copy(
        displayLarge = Typography.displayLarge.scaled(fontScale),
        displayMedium = Typography.displayMedium.scaled(fontScale),
        displaySmall = Typography.displaySmall.scaled(fontScale),
        headlineLarge = Typography.headlineLarge.scaled(fontScale),
        headlineMedium = Typography.headlineMedium.scaled(fontScale),
        headlineSmall = Typography.headlineSmall.scaled(fontScale),
        titleLarge = Typography.titleLarge.scaled(fontScale),
        titleMedium = Typography.titleMedium.scaled(fontScale),
        titleSmall = Typography.titleSmall.scaled(fontScale),
        bodyLarge = Typography.bodyLarge.scaled(fontScale),
        bodyMedium = Typography.bodyMedium.scaled(fontScale),
        bodySmall = Typography.bodySmall.scaled(fontScale),
        labelLarge = Typography.labelLarge.scaled(fontScale),
        labelMedium = Typography.labelMedium.scaled(fontScale),
        labelSmall = Typography.labelSmall.scaled(fontScale)
    )

    CompositionLocalProvider(LocalGetFitColors provides DarkGetFitColors) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = adaptiveTypography,
            shapes = Shapes,
            content = content
        )
    }
}
