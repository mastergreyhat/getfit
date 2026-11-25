package com.getfit.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.getfit.ui.theme.CaloriesOrange
import com.getfit.ui.theme.GetFitTheme
import com.getfit.ui.util.AppDateManager

data class StrengthCurvePoint(
    val date: String,
    val weight: Double,
    val calculated1RM: Double = 0.0
)

@Composable
fun StrengthCurveChart(
    points: List<StrengthCurvePoint>,
    modifier: Modifier = Modifier,
    animateIntro: Boolean = true,
    onIntroFinished: () -> Unit = {}
) {
    if (points.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(130.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Log workout sets to plot your strength progression curve!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val displayPoints = remember(points) { points.takeLast(7) }

    val minWeight = remember(displayPoints) {
        val min = displayPoints.minOf { it.weight }
        (min - 5.0).coerceAtLeast(0.0)
    }
    val maxWeight = remember(displayPoints) {
        val max = displayPoints.maxOf { it.weight }
        (max + 5.0).coerceAtLeast(minWeight + 10.0)
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val lineColor = GetFitTheme.colors.success
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Summary row for latest & max recorded
        val latest = displayPoints.last()
        val highest = displayPoints.maxByOrNull { it.weight } ?: latest

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = lineColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Latest: ${latest.weight.toInt()}kg (${AppDateManager.formatShortDate(latest.date)})",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (highest.calculated1RM > 0) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = CaloriesOrange.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Est. 1RM ~${highest.calculated1RM.toInt()}kg",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = CaloriesOrange,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Real Interactive Line Graph (cached geometry/paths via drawWithCache + one-shot intro reveal)
        var drawnOnce by remember(animateIntro) { mutableStateOf(!animateIntro) }
        LaunchedEffect(animateIntro) {
            if (animateIntro && !drawnOnce) {
                drawnOnce = true
                onIntroFinished()
            }
        }
        val revealProgress by animateFloatAsState(
            targetValue = if (drawnOnce) 1f else 0f,
            animationSpec = tween(durationMillis = if (animateIntro) 400 else 0, easing = FastOutSlowInEasing),
            label = "strength_curve_reveal"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            val surfaceColor = MaterialTheme.colorScheme.surface
            Spacer(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithCache {
                        val w = size.width
                        val h = size.height - 24.dp.toPx()
                        val weightRange = (maxWeight - minWeight).coerceAtLeast(1.0)
                        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        val gridY = listOf(h * 0.15f, h * 0.55f, h * 0.95f)
                        val strokeStyle = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )

                        val coords = if (displayPoints.size == 1) {
                            val pt = displayPoints[0]
                            val normalizedY = ((pt.weight - minWeight) / weightRange).toFloat().coerceIn(0f, 1f)
                            val cy = h - (normalizedY * (h - 20.dp.toPx())) - 10.dp.toPx()
                            listOf(Offset(w / 2f, cy))
                        } else {
                            displayPoints.mapIndexed { index, pt ->
                                val cx = (w / (displayPoints.size - 1)) * index
                                val normalizedY = ((pt.weight - minWeight) / weightRange).toFloat().coerceIn(0f, 1f)
                                val cy = h - (normalizedY * (h - 24.dp.toPx())) - 12.dp.toPx()
                                Offset(cx, cy)
                            }
                        }

                        val fillPath = if (coords.size > 1) {
                            Path().apply {
                                moveTo(coords.first().x, coords.first().y)
                                for (i in 1 until coords.size) {
                                    val p0 = coords[i - 1]
                                    val p1 = coords[i]
                                    val midX = (p0.x + p1.x) / 2f
                                    cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                                }
                                lineTo(coords.last().x, h)
                                lineTo(coords.first().x, h)
                                close()
                            }
                        } else null

                        val linePath = if (coords.size > 1) {
                            Path().apply {
                                moveTo(coords.first().x, coords.first().y)
                                for (i in 1 until coords.size) {
                                    val p0 = coords[i - 1]
                                    val p1 = coords[i]
                                    val midX = (p0.x + p1.x) / 2f
                                    cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                                }
                            }
                        } else null

                        val areaBrush = Brush.verticalGradient(
                            colors = listOf(lineColor.copy(alpha = 0.35f), Color.Transparent),
                            startY = 0f,
                            endY = h
                        )

                        onDrawBehind {
                            gridY.forEach { y ->
                                drawLine(
                                    color = gridColor,
                                    start = Offset(0f, y),
                                    end = Offset(w, y),
                                    strokeWidth = 1.dp.toPx(),
                                    pathEffect = dashEffect
                                )
                            }

                            clipRect(right = size.width * revealProgress) {
                                if (coords.size == 1) {
                                    val ptCenter = coords[0]
                                    drawCircle(color = lineColor.copy(alpha = 0.25f), radius = 16.dp.toPx(), center = ptCenter)
                                    drawCircle(color = lineColor, radius = 7.dp.toPx(), center = ptCenter)
                                    drawCircle(color = surfaceColor, radius = 3.dp.toPx(), center = ptCenter)
                                } else if (fillPath != null && linePath != null) {
                                    drawPath(path = fillPath, brush = areaBrush)
                                    drawPath(path = linePath, color = lineColor, style = strokeStyle)
                                    coords.forEach { ptOffset ->
                                        drawCircle(color = lineColor.copy(alpha = 0.3f), radius = 9.dp.toPx(), center = ptOffset)
                                        drawCircle(color = lineColor, radius = 5.dp.toPx(), center = ptOffset)
                                        drawCircle(color = surfaceColor, radius = 2.dp.toPx(), center = ptOffset)
                                    }
                                }
                            }
                        }
                    }
            )

            // X-Axis Date Labels & Weight Badges along the bottom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                horizontalArrangement = if (displayPoints.size == 1) Arrangement.Center else Arrangement.SpaceBetween
            ) {
                displayPoints.forEach { pt ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${pt.weight.toInt()}kg",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = AppDateManager.formatShortDate(pt.date), // Shows "12 Nov", not confusing "11-12"
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (displayPoints.size == 1) {
            Text(
                text = "1 session logged. Log this exercise on other dates to see your progression curve rise!",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}
