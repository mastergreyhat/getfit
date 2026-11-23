package com.getfit.ui.components.core

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.getfit.ui.theme.Dimens
import com.getfit.ui.theme.GetFitTheme

/**
 * Premium CRED / Apple TV style card.
 * Surface1 background, 24dp corner radius, 1dp hairline border,
 * optional subtle top-to-bottom gradient, 20dp internal padding. No elevation or shadow.
 */
@Composable
fun GfCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    cornerRadius: Dp = Dimens.xxl, // 24dp
    contentPadding: PaddingValues = PaddingValues(Dimens.xl), // 20dp
    useGradient: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val baseSurface = GetFitTheme.colors.surface1
    val isDark = GetFitTheme.colors.isDark

    val backgroundBrush = if (useGradient) {
        val lighterSurface = if (isDark) {
            baseSurface.copy(
                red = (baseSurface.red + 0.03f).coerceAtMost(1f),
                green = (baseSurface.green + 0.03f).coerceAtMost(1f),
                blue = (baseSurface.blue + 0.03f).coerceAtMost(1f)
            )
        } else {
            baseSurface.copy(
                red = (baseSurface.red - 0.02f).coerceAtLeast(0f),
                green = (baseSurface.green - 0.02f).coerceAtLeast(0f),
                blue = (baseSurface.blue - 0.02f).coerceAtLeast(0f)
            )
        }
        Brush.verticalGradient(listOf(baseSurface, lighterSurface))
    } else {
        Brush.verticalGradient(listOf(baseSurface, baseSurface))
    }

    Box(
        modifier = modifier
            .then(if (onClick != null) Modifier.pressable(onClick = onClick) else Modifier)
            .clip(shape)
            .background(backgroundBrush)
            .border(BorderStroke(1.dp, GetFitTheme.colors.hairline), shape)
            .padding(contentPadding)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

/**
 * Hero Card variant: Like GfCard but with a faint radial accent glow in top-end corner
 * (accent at 6% alpha, blurred via Brush.radialGradient for maximum performance).
 * Used at most once per screen.
 */
@Composable
fun GfHeroCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    cornerRadius: Dp = Dimens.xxl,
    contentPadding: PaddingValues = PaddingValues(Dimens.xl),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val baseSurface = GetFitTheme.colors.surface1
    val accentGlow = GetFitTheme.colors.accent.copy(alpha = 0.06f)

    Box(
        modifier = modifier
            .then(if (onClick != null) Modifier.pressable(onClick = onClick) else Modifier)
            .clip(shape)
            .background(baseSurface)
            .background(
                Brush.radialGradient(
                    colors = listOf(accentGlow, Color.Transparent),
                    center = Offset(Float.POSITIVE_INFINITY, 0f),
                    radius = 450f
                )
            )
            .border(BorderStroke(1.dp, GetFitTheme.colors.hairline), shape)
            .padding(contentPadding)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

@Preview(name = "GfCard - Dark", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun GfCardDarkPreview() {
    GetFitTheme(themeMode = "DARK") {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            GfCard {
                Text("Standard Card", style = MaterialTheme.typography.titleMedium, color = GetFitTheme.colors.textPrimary)
                Text("Surface1 layered with hairline border and 20dp padding", style = MaterialTheme.typography.bodyMedium, color = GetFitTheme.colors.textSecondary)
            }
            GfHeroCard {
                Text("Hero Card (Dominant)", style = MaterialTheme.typography.titleMedium, color = GetFitTheme.colors.textPrimary)
                Text("Subtle 6% radial accent glow in the top corner", style = MaterialTheme.typography.bodyMedium, color = GetFitTheme.colors.textSecondary)
            }
        }
    }
}

@Preview(name = "GfCard - Light", showBackground = true, backgroundColor = 0xFFF7F6F2)
@Composable
private fun GfCardLightPreview() {
    GetFitTheme(themeMode = "LIGHT") {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            GfCard {
                Text("Standard Card", style = MaterialTheme.typography.titleMedium, color = GetFitTheme.colors.textPrimary)
                Text("Surface1 layered with hairline border and 20dp padding", style = MaterialTheme.typography.bodyMedium, color = GetFitTheme.colors.textSecondary)
            }
            GfHeroCard {
                Text("Hero Card (Dominant)", style = MaterialTheme.typography.titleMedium, color = GetFitTheme.colors.textPrimary)
                Text("Subtle 6% radial accent glow in the top corner", style = MaterialTheme.typography.bodyMedium, color = GetFitTheme.colors.textSecondary)
            }
        }
    }
}
