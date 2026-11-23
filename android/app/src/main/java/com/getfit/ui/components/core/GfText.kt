package com.getfit.ui.components.core

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.getfit.ui.theme.Dimens
import com.getfit.ui.theme.GetFitTheme

/**
 * Quiet, uppercase overline label.
 * Uses labelSmall typography in textTertiary with generous tracking.
 */
@Composable
fun GfOverline(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = GetFitTheme.colors.textTertiary,
        modifier = modifier
    )
}

/**
 * Big, confident hero number display with smooth animated count-up.
 * Value counts up over 700ms via FastOutSlowInEasing.
 * Unit renders smaller and baseline-aligned in textSecondary.
 */
@Composable
fun GfBigNumber(
    value: Int,
    modifier: Modifier = Modifier,
    unit: String? = null,
    hero: Boolean = false,
    color: Color = GetFitTheme.colors.textPrimary,
    style: TextStyle = MaterialTheme.typography.displayLarge
) {
    val animatedValue by animateIntAsState(
        targetValue = value,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "big_number_count"
    )

    val currentDensity = LocalDensity.current
    val effectiveDensity = if (hero) {
        Density(currentDensity.density, currentDensity.fontScale.coerceAtMost(1.15f))
    } else {
        currentDensity
    }

    CompositionLocalProvider(LocalDensity provides effectiveDensity) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = animatedValue.toString(),
                style = style,
                color = color,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.alignByBaseline()
            )
            if (!unit.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(Dimens.xs))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = GetFitTheme.colors.textSecondary,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.alignByBaseline()
                )
            }
        }
    }
}

/**
 * Float / Double overload for GfBigNumber.
 * Formats with [decimals] places (default 0), animated count-up over 700ms,
 * and baseline-aligned unit in textSecondary.
 */
@Composable
fun GfBigNumber(
    value: Double,
    modifier: Modifier = Modifier,
    decimals: Int = 0,
    unit: String? = null,
    hero: Boolean = false,
    color: Color = GetFitTheme.colors.textPrimary,
    style: TextStyle = MaterialTheme.typography.displayLarge
) {
    val animatedValue by androidx.compose.animation.core.animateFloatAsState(
        targetValue = value.toFloat(),
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "big_number_double_count"
    )

    val formatted = if (decimals > 0) {
        String.format(java.util.Locale.US, "%.${decimals}f", animatedValue)
    } else {
        animatedValue.toInt().toString()
    }

    val currentDensity = LocalDensity.current
    val effectiveDensity = if (hero) {
        Density(currentDensity.density, currentDensity.fontScale.coerceAtMost(1.15f))
    } else {
        currentDensity
    }

    CompositionLocalProvider(LocalDensity provides effectiveDensity) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = formatted,
                style = style,
                color = color,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.alignByBaseline()
            )
            if (!unit.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(Dimens.xs))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = GetFitTheme.colors.textSecondary,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.alignByBaseline()
                )
            }
        }
    }
}

@Composable
fun GfBigNumber(
    value: Float,
    modifier: Modifier = Modifier,
    decimals: Int = 0,
    unit: String? = null,
    hero: Boolean = false,
    color: Color = GetFitTheme.colors.textPrimary,
    style: TextStyle = MaterialTheme.typography.displayLarge
) = GfBigNumber(
    value = value.toDouble(),
    modifier = modifier,
    decimals = decimals,
    unit = unit,
    hero = hero,
    color = color,
    style = style
)

/**
 * Section header with customizable [topSpacing], confident titleLarge styling,
 * and an optional interactive text action.
 */
@Composable
fun GfSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    topSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    action: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = topSpacing, bottom = Dimens.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = GetFitTheme.colors.textPrimary
        )
        if (!action.isNullOrBlank() && onActionClick != null) {
            Text(
                text = action,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = GetFitTheme.colors.accent,
                modifier = Modifier
                    .pressable(onClick = onActionClick)
                    .padding(horizontal = Dimens.xs, vertical = Dimens.xs)
            )
        }
    }
}

@Preview(name = "GfText - Dark", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun GfTextDarkPreview() {
    GetFitTheme(themeMode = "DARK") {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            GfOverline("Daily Calorie Target")
            GfBigNumber(value = 2450, unit = "kcal")
            GfSectionHeader(title = "Weekly Volume", action = "See All", onActionClick = {})
        }
    }
}

@Preview(name = "GfText - Light", showBackground = true, backgroundColor = 0xFFF7F6F2)
@Composable
private fun GfTextLightPreview() {
    GetFitTheme(themeMode = "LIGHT") {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            GfOverline("Daily Calorie Target")
            GfBigNumber(value = 2450, unit = "kcal")
            GfSectionHeader(title = "Weekly Volume", action = "See All", onActionClick = {})
        }
    }
}
