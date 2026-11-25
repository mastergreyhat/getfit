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
                val sweepAngle = if (isSolid) 360f else 360f * animatedProgress
                rotate(-90f, pivot = center) {
                    drawArc(
                        color = activeColor,
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
    val currentInt = current.toInt()
    val targetInt = target.toInt()
    val overAmt = currentInt - targetInt
    val isOver = overAmt > 0
    val isMet = overAmt == 0 && targetInt > 0
    val progress = if (target > 0f) (current / target).coerceIn(0f, 1f) else 0f
    val valueColor = when {
        isOver -> GetFitTheme.colors.warning
        isMet -> GetFitTheme.colors.success
        else -> GetFitTheme.colors.textPrimary
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Dimens.xs)
    ) {
        if (inlineHeader) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // weight(1f) gives the label room; value never squeezes it
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = GetFitTheme.colors.textTertiary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(Dimens.xs))
                Text(
                    text = if (isOver) "+${overAmt}g" else "${currentInt}/${targetInt}g",
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.SemiBold,
                    color = valueColor,
                    maxLines = 1,
                    softWrap = false
                )
            }
        } else {
            GfOverline(text = label)
            Text(
                text = "${currentInt}g",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = valueColor
            )
            Text(
                text = when {
                    isOver -> "+${overAmt}g over"
                    isMet -> "Goal met"
                    else -> "of ${targetInt}g"
                },
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    isOver -> GetFitTheme.colors.warning
                    isMet -> GetFitTheme.colors.success
                    else -> GetFitTheme.colors.textSecondary
                }
            )
            Spacer(modifier = Modifier.height(2.dp))
        }
        GfProgressBar(
            progress = if (isOver || isMet) 1f else progress,
            color = color
        )
    }
}

@Preview(name = "GfProgress - Dark", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun GfProgressDarkPreview() {
    GetFitTheme {
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
