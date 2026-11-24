package com.getfit.ui.components.core

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.getfit.ui.theme.Dimens
import com.getfit.ui.theme.GetFitTheme

/**
 * Tactical 4dp progress bar with 14% track opacity, spring animation, and full rounding.
 * Clamps progress between 0f and 1f.
 */
@Composable
fun GfProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = GetFitTheme.colors.accent,
    trackColor: Color = color.copy(alpha = 0.14f)
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clampedProgress,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "progress_bar_fill"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(CircleShape)
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedProgress)
                .clip(CircleShape)
                .background(color)
        )
    }
}

/**
 * Premium circular progress ring replacing stock CircularProgressIndicator.
 * Features 10dp thin stroke, rounded caps, rotated gradient sweep starting at 12 o'clock,
 * hairline track, and calm 900ms animation.
 *
 * Visual progress is clamped to 1f. If [overGoal] is true, the arc switches to the warning color.
 */
@Composable
fun GfRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    strokeWidth: Dp = 10.dp,
    color: Color = GetFitTheme.colors.accent,
    overGoal: Boolean = false,
    content: (@Composable BoxScope.() -> Unit)? = null
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clampedProgress,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "ring_progress"
    )

    val hairlineColor = GetFitTheme.colors.hairline
    val activeColor = if (overGoal) GetFitTheme.colors.warning else color
    val isSolid = clampedProgress >= 1f || overGoal

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(strokeWidth / 2)
        ) {
            val strokePx = strokeWidth.toPx()

            // 1. Subtle hairline background track
            drawCircle(
                color = hairlineColor,
                style = Stroke(width = strokePx)
            )

            // 2. Animated progress sweep rotated to start at 12 o'clock (-90 degrees)
            if (animatedProgress > 0f) {
                val sweepAngle = 360f * animatedProgress
                rotate(-90f, pivot = center) {
                    if (isSolid) {
                        // Solid-color arc when progress >= 1f or overGoal: completely seamless
                        drawArc(
                            color = activeColor,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(
                                width = strokePx,
                                cap = StrokeCap.Round
                            )
                        )
                    } else {
                        // For partial progress, identical first and last color stops prevent any visible seam
                        val sweepBrush = Brush.sweepGradient(
                            0.0f to activeColor,
                            (clampedProgress * 0.5f) to activeColor.copy(alpha = 0.85f),
                            clampedProgress to activeColor,
                            center = center
                        )
                        drawArc(
                            brush = sweepBrush,
                            startAngle = 0f,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(
                                width = strokePx,
                                cap = StrokeCap.Round
                            )
                        )
                    }
                }
            }
        }

        if (content != null) {
            content()
        }
    }
}

/**
 * Shared compact macro stat used across Dashboard and Food Diary.
 * If [current] > [target]: the value text turns warning color, the sub-line shows "+{current - target}g over"
 * in warning (replacing "of {target}g"), and the progress bar stays full in [color].
 * Otherwise: normal styling with current in textPrimary and sub-line "of {target}g" in textSecondary.
 */
@Composable
fun MacroStat(
    label: String,
    current: Float,
    target: Float,
    color: Color,
    modifier: Modifier = Modifier,
    inlineHeader: Boolean = false
) {
    val isOver = current > target
    val progress = if (target > 0f) (current / target).coerceIn(0f, 1f) else 0f

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(if (inlineHeader) Dimens.xs else Dimens.xs)
    ) {
        if (inlineHeader) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.xs),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    GfOverline(text = label)
                    if (isOver) {
                        Text(
                            text = "(+${(current - target).toInt()}g)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = GetFitTheme.colors.warning,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                Spacer(modifier = Modifier.width(Dimens.xs))
                Text(
                    text = "${current.toInt()}g of ${target.toInt()}g",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isOver) GetFitTheme.colors.warning else GetFitTheme.colors.textPrimary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        } else {
            GfOverline(text = label)
            Text(
                text = "${current.toInt()}g",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isOver) GetFitTheme.colors.warning else GetFitTheme.colors.textPrimary
            )
            Text(
                text = if (isOver) {
                    "+${(current - target).toInt()}g over"
                } else {
                    "of ${target.toInt()}g"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (isOver) GetFitTheme.colors.warning else GetFitTheme.colors.textSecondary
            )
            Spacer(modifier = Modifier.height(2.dp))
        }
        GfProgressBar(
            progress = if (isOver) 1f else progress,
            color = color
        )
    }
}

@Preview(name = "GfProgress - Dark", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun GfProgressDarkPreview() {
    GetFitTheme(themeMode = "DARK") {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            GfProgressBar(progress = 0.65f)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                GfRing(progress = 0.72f) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        GfBigNumber(value = 72, unit = "%", style = MaterialTheme.typography.titleLarge)
                        Text("Daily Goal", style = MaterialTheme.typography.labelSmall, color = GetFitTheme.colors.textSecondary)
                    }
                }
                GfRing(progress = 1.15f, overGoal = true) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        GfBigNumber(value = 115, unit = "%", style = MaterialTheme.typography.titleLarge)
                        Text("Over Goal", style = MaterialTheme.typography.labelSmall, color = GetFitTheme.colors.warning)
                    }
                }
            }
        }
    }
}

@Preview(name = "GfProgress - Light", showBackground = true, backgroundColor = 0xFFF7F6F2)
@Composable
private fun GfProgressLightPreview() {
    GetFitTheme(themeMode = "LIGHT") {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            GfProgressBar(progress = 0.65f)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                GfRing(progress = 0.72f) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        GfBigNumber(value = 72, unit = "%", style = MaterialTheme.typography.titleLarge)
                        Text("Daily Goal", style = MaterialTheme.typography.labelSmall, color = GetFitTheme.colors.textSecondary)
                    }
                }
                GfRing(progress = 1.15f, overGoal = true) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        GfBigNumber(value = 115, unit = "%", style = MaterialTheme.typography.titleLarge)
                        Text("Over Goal", style = MaterialTheme.typography.labelSmall, color = GetFitTheme.colors.warning)
                    }
                }
            }
        }
    }
}
